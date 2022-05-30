package re.notifica.go.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import re.notifica.geo.models.NotificareBeacon
import re.notifica.go.R
import re.notifica.go.databinding.ViewBeaconBinding

class BeaconsAdapter : ListAdapter<NotificareBeacon, BeaconsAdapter.BeaconViewHolder>(BeaconDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
        return BeaconViewHolder(ViewBeaconBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BeaconViewHolder(
        private val binding: ViewBeaconBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(beacon: NotificareBeacon) {
            binding.nameLabel.text = beacon.name

            binding.identifierLabel.text =
                if (beacon.minor == null) "${beacon.major}"
                else "${beacon.major} • ${beacon.minor}"

            binding.triggersImage.isVisible = beacon.triggers

            binding.signalImage.setImageResource(
                when (beacon.proximity) {
                    NotificareBeacon.Proximity.IMMEDIATE -> R.drawable.ic_baseline_wifi_signal_3_24
                    NotificareBeacon.Proximity.NEAR -> R.drawable.ic_baseline_wifi_signal_2_24
                    NotificareBeacon.Proximity.FAR -> R.drawable.ic_baseline_wifi_signal_1_24
                    NotificareBeacon.Proximity.UNKNOWN -> R.drawable.ic_baseline_wifi_signal_off_24
                }
            )
        }
    }
}

private class BeaconDiffCallback : DiffUtil.ItemCallback<NotificareBeacon>() {
    override fun areItemsTheSame(oldItem: NotificareBeacon, newItem: NotificareBeacon): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NotificareBeacon, newItem: NotificareBeacon): Boolean {
        return oldItem == newItem
    }
}
