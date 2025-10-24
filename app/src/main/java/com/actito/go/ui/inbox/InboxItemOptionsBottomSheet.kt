package com.actito.go.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.actito.go.databinding.BottomSheetInboxItemOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InboxItemOptionsBottomSheet(
    private val onOpenClicked: () -> Unit,
    private val onMarkAsReadClicked: () -> Unit,
    private val onRemoveClicked: () -> Unit,
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetInboxItemOptionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetInboxItemOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.openButton.setOnClickListener {
            onOpenClicked()
            dismiss()
        }

        binding.markAsReadButton.setOnClickListener {
            onMarkAsReadClicked()
            dismiss()
        }

        binding.removeButton.setOnClickListener {
            onRemoveClicked()
            dismiss()
        }
    }
}
