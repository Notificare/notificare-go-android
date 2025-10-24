package com.actito.go.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.actito.geo.models.ActitoBeacon
import com.actito.go.R
import com.actito.go.databinding.ViewBeaconBinding

class BeaconsAdapter : ListAdapter<ActitoBeacon, BeaconsAdapter.BeaconViewHolder>(BeaconDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
        return BeaconViewHolder(ViewBeaconBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BeaconViewHolder(
        private val binding: ViewBeaconBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(beacon: ActitoBeacon) {
            binding.nameLabel.text = beacon.name

            binding.identifierLabel.text =
                if (beacon.minor == null) "${beacon.major}"
                else "${beacon.major} • ${beacon.minor}"

            binding.triggersImage.isVisible = beacon.triggers

            binding.signalImage.setImageResource(
                when (beacon.proximity) {
                    ActitoBeacon.Proximity.IMMEDIATE -> R.drawable.ic_baseline_wifi_signal_3_24
                    ActitoBeacon.Proximity.NEAR -> R.drawable.ic_baseline_wifi_signal_2_24
                    ActitoBeacon.Proximity.FAR -> R.drawable.ic_baseline_wifi_signal_1_24
                    ActitoBeacon.Proximity.UNKNOWN -> R.drawable.ic_baseline_wifi_signal_off_24
                }
            )
        }
    }
}

private class BeaconDiffCallback : DiffUtil.ItemCallback<ActitoBeacon>() {
    override fun areItemsTheSame(oldItem: ActitoBeacon, newItem: ActitoBeacon): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ActitoBeacon, newItem: ActitoBeacon): Boolean {
        return oldItem == newItem
    }
}
