package com.davek.tictactoe.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.davek.tictactoe.fragments.SearchOpponentFragment
import com.davek.tictactoe.fragments.SavedGamesFragment

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragmentList = listOf(
        SearchOpponentFragment(),
        SavedGamesFragment()
    )

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment = fragmentList[position]
}