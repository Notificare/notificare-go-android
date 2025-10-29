package com.actito.go.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.actito.go.R
import com.actito.go.databinding.FragmentEventsAttributeBinding
import com.actito.go.databinding.FragmentEventsBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EventsFragment : Fragment() {
    private val viewModel: EventsViewModel by viewModels()
    private lateinit var binding: FragmentEventsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.eventNameEdit.addTextChangedListener { text ->
            if (text == null) return@addTextChangedListener
            if (text.toString() == viewModel.eventName.value) return@addTextChangedListener
            viewModel._eventName.tryEmit(text.toString())
        }

        binding.createAttributeButton.setOnClickListener {
            viewModel.createAttribute()
        }

        binding.submitButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    binding.submitButton.isEnabled = false

                    viewModel.submitEvent()

                    Snackbar.make(binding.root, R.string.events_create_event_success_message, Snackbar.LENGTH_SHORT)
                        .show()
                } catch (_: Exception) {
                    Snackbar.make(binding.root, R.string.events_create_event_failure_message, Snackbar.LENGTH_SHORT)
                        .show()
                } finally {
                    binding.submitButton.isEnabled = true
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.eventName.observe(viewLifecycleOwner) { name ->
            if (name == binding.eventNameEdit.text?.toString()) return@observe

            binding.eventNameEdit.setText(name)
            binding.eventNameEdit.setSelection(name.length)
        }

        viewModel.eventAttributes.observe(viewLifecycleOwner) { attributes ->
            attributes.forEachIndexed { index, attribute ->
                if (index > binding.attributesContainer.childCount - 1) {
                    FragmentEventsAttributeBinding.inflate(
                        layoutInflater,
                        binding.attributesContainer,
                        true
                    ).apply {
                        nameEdit.addTextChangedListener { text ->
                            if (text == null) return@addTextChangedListener

                            @Suppress("NAME_SHADOWING")
                            val attributes = viewModel._eventAttributes.value
                            val entry = attributes.elementAt(index)
                            if (text.toString() == entry.name) return@addTextChangedListener

                            entry.name = text.toString()
                            viewModel._eventAttributes.tryEmit(attributes)
                        }

                        valueEdit.addTextChangedListener { text ->
                            if (text == null) return@addTextChangedListener

                            @Suppress("NAME_SHADOWING")
                            val attributes = viewModel._eventAttributes.value
                            val entry = attributes.elementAt(index)
                            if (text.toString() == entry.value) return@addTextChangedListener

                            entry.value = text.toString()
                            viewModel._eventAttributes.tryEmit(attributes)
                        }
                    }
                }

                FragmentEventsAttributeBinding.bind(
                    binding.attributesContainer.getChildAt(index)
                ).apply {
                    nameEdit.setText(attribute.name)
                    valueEdit.setText(attribute.value)
                }
            }

            if (attributes.size < binding.attributesContainer.childCount) {
                for (index in binding.attributesContainer.childCount - 1 downTo attributes.size) {
                    binding.attributesContainer.removeViewAt(index)
                }
            }
        }
    }
}
