package com.actito.go.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.actito.go.BuildConfig
import com.actito.go.R
import com.actito.go.databinding.FragmentProfileBinding
import com.actito.go.databinding.FragmentProfileFieldDatePickerBinding
import com.actito.go.databinding.FragmentProfileFieldTextBinding
import com.actito.go.databinding.FragmentProfileFieldToggleBinding
import com.actito.go.databinding.FragmentProfileFieldUnsupportedBinding
import com.actito.go.models.UserInfo
import com.bumptech.glide.Glide
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var binding: FragmentProfileBinding
    private val textWatchers = mutableMapOf<String, TextWatcher>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.userInfo.observe(viewLifecycleOwner, ::render)
        viewModel.userDataFields.observe(viewLifecycleOwner, ::render)

        binding.headerSection.setOnLongClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("User ID", viewModel.userInfo.value?.id)
            clipboard.setPrimaryClip(clip)

            Snackbar.make(binding.root, R.string.profile_user_id_copied_message, Snackbar.LENGTH_SHORT).show()

            true
        }

        binding.deleteAccountButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_delete_account_confirmation_title)
                .setMessage(R.string.profile_delete_account_confirmation_message)
                .setPositiveButton(R.string.dialog_yes_button) { _, _ ->
                    lifecycleScope.launch {
                        try {
                            viewModel.deleteAccount()
                            showIntro()
                        } catch (e: Exception) {
                            if (e is FirebaseAuthRecentLoginRequiredException) {
                                Timber.w("Must re-authenticate the user before removing the account.")
                                authenticate()

                                return@launch
                            }

                            Snackbar.make(
                                binding.root,
                                R.string.profile_delete_account_failure,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .setNegativeButton(R.string.dialog_cancel_button, null)
                .show()
        }

        viewModel.membershipCard.observe(viewLifecycleOwner) { membershipCardUrl ->
            binding.membershipCard.root.isVisible = membershipCardUrl != null

            if (membershipCardUrl != null) {
                val url = membershipCardUrl.toUri()

                binding.membershipCard.root.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to open the membership card URL.")
                    }
                }
            }
        }
    }


    private fun render(userInfo: UserInfo) {
        Glide.with(this)
            .load(userInfo.pictureUrl)
            .circleCrop()
            .into(binding.avatarImage)

        binding.idLabel.text = userInfo.id
        binding.nameLabel.text = userInfo.name ?: getString(R.string.profile_anonymous_user_name)
    }

    private fun render(userDataFields: List<ProfileViewModel.UserDataField>) {
        // The field views are only created once.
        // The logic rests on the assumption the field index and type are not mutable.
        if (binding.userDataFieldsContainer.isEmpty()) {
            userDataFields.forEach { field ->
                when (field.type) {
                    "text", "number" -> FragmentProfileFieldTextBinding.inflate(
                        layoutInflater,
                        binding.userDataFieldsContainer,
                        true
                    )
                    "date" -> FragmentProfileFieldDatePickerBinding.inflate(
                        layoutInflater,
                        binding.userDataFieldsContainer,
                        true
                    )
                    "boolean" -> FragmentProfileFieldToggleBinding.inflate(
                        layoutInflater,
                        binding.userDataFieldsContainer,
                        true
                    )
                    else -> FragmentProfileFieldUnsupportedBinding.inflate(
                        layoutInflater,
                        binding.userDataFieldsContainer,
                        true
                    )
                }
            }
        }

        // Bind the updated values to each view.
        userDataFields.forEachIndexed { index, field ->
            val view = binding.userDataFieldsContainer.getChildAt(index)

            when (field.type) {
                "text", "number" -> {
                    FragmentProfileFieldTextBinding.bind(view).also { binding ->
                        binding.inputLayout.hint = field.label
                        binding.inputEditText.apply {
                            inputType =
                                if (field.type == "number") InputType.TYPE_CLASS_NUMBER
                                else InputType.TYPE_CLASS_TEXT

                            // Remove any previous text watcher preventing an infinite loop.
                            textWatchers[field.key]?.let { removeTextChangedListener(it) }

                            setText(field.value)
                            setSelection(field.value.length)

                            addTextChangedListener {
                                userDataFields.first { it.key == field.key }.value = text.toString()
                                viewModel.userDataFieldChanges.tryEmit(userDataFields)
                            }.also { textWatchers[field.key] = it }
                        }
                    }
                }
                "date" -> {
                    FragmentProfileFieldDatePickerBinding.bind(view).also { binding ->
                        val parsedDate: Date? = try {
                            dateParser.parse(field.value)
                        } catch (_: Exception) {
                            null
                        }

                        binding.nameLabel.text = field.label
                        binding.valueLabel.text = parsedDate?.let { dateFormatter.format(it) }

                        binding.root.setOnClickListener {
                            val datePicker = MaterialDatePicker.Builder.datePicker()
                                .setTitleText(field.label)
                                .apply { parsedDate?.let { setSelection(it.time) } }
                                .build()

                            datePicker.addOnPositiveButtonClickListener { date ->
                                binding.valueLabel.text = dateFormatter.format(date)

                                userDataFields.first { it.key == field.key }.value = dateParser.format(date)
                                viewModel.userDataFieldChanges.tryEmit(userDataFields)
                            }

                            datePicker.show(childFragmentManager, "date-picker")
                        }
                    }
                }
                "boolean" -> {
                    FragmentProfileFieldToggleBinding.bind(view).also { binding ->
                        binding.toggle.text = field.label

                        // Remove any previous listener preventing an infinite loop.
                        binding.toggle.setOnCheckedChangeListener(null)

                        binding.toggle.isChecked = field.value.toBoolean()

                        binding.toggle.setOnCheckedChangeListener { _, checked ->
                            userDataFields.first { it.key == field.key }.value = checked.toString()
                            viewModel.userDataFieldChanges.tryEmit(userDataFields)
                        }
                    }
                }
                else -> {
                    FragmentProfileFieldUnsupportedBinding.bind(view).also { binding ->
                        binding.nameLabel.text = field.label
                    }
                }
            }
        }

        binding.userDataFieldsTitleLabel.isVisible = userDataFields.isNotEmpty()
        binding.userDataFieldsContainer.isVisible = userDataFields.isNotEmpty()

        binding.deleteAccountButton.isVisible = true
    }

    private fun authenticate(filterAuthorizedAccounts: Boolean = true) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
            .setServerClientId(BuildConfig.GOOGLE_AUTH_SERVER_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = viewModel.credentialManager.getCredential(requireContext(), request)

                try {
                    viewModel.handleAuthenticationResult(result)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to handle the authentication result.")
                    Snackbar.make(binding.root, R.string.intro_login_error_message, Snackbar.LENGTH_SHORT).show()

                    return@launch
                }

                try {
                    viewModel.deleteAccount()
                    showIntro()
                } catch (_: Exception) {
                    Snackbar.make(
                        binding.root,
                        R.string.profile_delete_account_failure,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                if (e is GetCredentialCancellationException) {
                    Timber.i("User dismissed the login popup.")
                    return@launch
                }

                if (filterAuthorizedAccounts) {
                    authenticate(filterAuthorizedAccounts = false)
                    return@launch
                }

                Timber.e(e, "Failed to authenticate the user.")
                Snackbar.make(binding.root, R.string.intro_login_error_message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showIntro() {
        // Access the parent NavController.
        // Using findNavController will yield a reference to the Bottom Navigation NavController.
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        navController.navigate(R.id.global_to_intro_action)
    }

    companion object {
        private val dateParser = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.sss'Z'", Locale.ROOT)
        private val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.ROOT)
    }
}
