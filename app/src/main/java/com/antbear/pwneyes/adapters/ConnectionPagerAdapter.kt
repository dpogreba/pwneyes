package com.antbear.pwneyes.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.antbear.pwneyes.data.Connection
import com.antbear.pwneyes.fragments.ConnectionWebViewFragment

/**
 * FragmentStateAdapter that maps each saved connection to a
 * [ConnectionWebViewFragment] tab inside the Home ViewPager2.
 *
 * Uses stable item IDs (Room primary keys) so ViewPager2 can
 * correctly reuse / destroy pages when the list changes.
 */
class ConnectionPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val connections = mutableListOf<Connection>()

    /** Replace the current list and notify the pager. */
    fun submitList(newList: List<Connection>) {
        connections.clear()
        connections.addAll(newList)
        notifyDataSetChanged()
    }

    /** Returns the connection at [position] — safe to call from the mediator callback. */
    fun getConnection(position: Int): Connection = connections[position]

    override fun getItemCount(): Int = connections.size

    override fun createFragment(position: Int): Fragment {
        val c = connections[position]
        return ConnectionWebViewFragment.newInstance(c.id, c.url, c.name)
    }

    // Stable IDs so ViewPager2 can diff correctly when items are added/removed.
    override fun getItemId(position: Int): Long = connections[position].id
    override fun containsItem(itemId: Long): Boolean = connections.any { it.id == itemId }
}
