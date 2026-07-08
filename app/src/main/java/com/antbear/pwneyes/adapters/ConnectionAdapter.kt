package com.antbear.pwneyes.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
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
    private val onDeleteClick: (Connection) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<Connection, ConnectionAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemConnectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")   // drag handle forwards ACTION_DOWN only
        fun bind(connection: Connection) {
            binding.tvDeviceName.text = connection.name
            binding.tvIpAddress.text = if (connection.port != 8080)
                "${connection.ipAddress}  ·  port ${connection.port}"
            else
                connection.ipAddress

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

            // Drag starts only from the handle (long-press-anywhere is disabled on the
            // callback), so a tap on Edit/Delete can never turn into a phantom drag.
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(this)
                false
            }
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

    // Cumulative order held during a drag. currentList lags behind submitList (async
    // differ), so a burst of onMove callbacks must swap against the last pending order,
    // not the stale committed one, or swaps get lost / positions desync and crash.
    private var pendingOrder: MutableList<Connection>? = null

    fun moveItem(from: Int, to: Int) {
        val list = pendingOrder ?: currentList.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        Collections.swap(list, from, to)
        pendingOrder = list
        submitList(list.toList())   // fresh copy; differ coalesces rapid moves
    }

    /** Authoritative order at drag end (currentList may still lag the last submit). */
    fun endDrag(): List<Connection> {
        val result = (pendingOrder ?: currentList).toList()
        pendingOrder = null
        return result
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
