package com.antbear.pwneyes.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.antbear.pwneyes.R
import com.antbear.pwneyes.data.Connection
import com.antbear.pwneyes.databinding.ItemConnectionBinding
import java.text.SimpleDateFormat
import java.util.*

class ConnectionAdapter(
    private val onEditClick: (Connection) -> Unit,
    private val onDeleteClick: (Connection) -> Unit
) : ListAdapter<Connection, ConnectionAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemConnectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(connection: Connection) {
            binding.tvDeviceName.text = connection.name
            // Show the IP and the always-8080 URL so the user can confirm at a glance.
            binding.tvIpAddress.text  = connection.url   // "http://192.168.x.x:8080"

            // Status label
            binding.tvStatus.text = when {
                connection.isConnected -> itemView.context.getString(R.string.status_connected)
                connection.lastConnectedMs > 0 -> {
                    val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    itemView.context.getString(
                        R.string.last_connected_format,
                        fmt.format(Date(connection.lastConnectedMs))
                    )
                }
                else -> itemView.context.getString(R.string.never_connected)
            }

            // Status dot colour
            val dotColor = when {
                connection.isConnected ->
                    itemView.context.getColor(R.color.status_connected)
                connection.lastConnectedMs > 0 ->
                    itemView.context.getColor(R.color.status_disconnected)
                else ->
                    itemView.context.getColor(R.color.status_unknown)
            }
            binding.statusDot.background.mutate().setTint(dotColor)

            binding.btnEdit.setOnClickListener   { onEditClick(connection) }
            binding.btnDelete.setOnClickListener { onDeleteClick(connection) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConnectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Connection>() {
            override fun areItemsTheSame(old: Connection, new: Connection) =
                old.id == new.id

            override fun areContentsTheSame(old: Connection, new: Connection) =
                old == new
        }
    }
}
