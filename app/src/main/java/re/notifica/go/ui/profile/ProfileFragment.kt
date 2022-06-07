package re.notifica.go.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import re.notifica.go.R
import re.notifica.go.databinding.*
import re.notifica.go.models.UserInfo
import java.text.SimpleDateFormat
import java.util.*

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

                            // Access the parent NavController.
                            // Using findNavController will yield a reference to the Bottom Navigation NavController.
                            val navController = requireActivity().findNavController(R.id.nav_host_fragment)
                            navController.navigate(R.id.global_to_intro)
                        } catch (e: Exception) {
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
    }


    private fun render(userInfo: UserInfo) {
        Glide.with(this)
            .load(userInfo.pictureUrl)
            .circleCrop()
            .into(binding.avatarImage)

        binding.idLabel.text = userInfo.id
        binding.nameLabel.text = userInfo.name
    }

    private fun render(userDataFields: List<ProfileViewModel.UserDataField>) {
        // The field views are only created once.
        // The logic rests on the assumption the field index and type are not mutable.
        if (binding.userDataFieldsContainer.childCount == 0) {
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
                        } catch (e: Exception) {
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

    companion object {
        private val dateParser = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.sss'Z'", Locale.ROOT)
        private val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.ROOT)
    }
}
