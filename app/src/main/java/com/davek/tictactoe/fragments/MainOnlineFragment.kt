package com.davek.tictactoe.fragments

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.davek.tictactoe.R
import com.davek.tictactoe.adapters.ViewPagerAdapter
import com.davek.tictactoe.databinding.FragmentMainOnlineBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainOnlineFragment : Fragment() {

    private lateinit var binding: FragmentMainOnlineBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_main_online, container, false
        )
        val tabLayout = binding.tabLayout
        val viewPager = binding.pager
        val tabTitles = resources.getStringArray(R.array.main_tab_titles)
        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        return binding.root
    }
}