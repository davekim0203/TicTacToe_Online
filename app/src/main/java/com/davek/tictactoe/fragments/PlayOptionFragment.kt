package com.davek.tictactoe.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.davek.tictactoe.R
import com.davek.tictactoe.databinding.FragmentPlayOptionBinding
import com.davek.tictactoe.viewmodels.PlayOptionViewModel
import com.firebase.ui.auth.AuthUI

class PlayOptionFragment : Fragment() {

    private val mTag = this::class.java.simpleName

    private lateinit var binding: FragmentPlayOptionBinding
    private val signInResultHandler = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        result?.let {
            if (it.resultCode == Activity.RESULT_OK) {
                Log.i(mTag, "Login successfully done")
            } else {
                Log.i(mTag, "Login failed")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_play_option, container, false
        )
        val playOptionViewModel: PlayOptionViewModel by viewModels()
        observeViewModel(playOptionViewModel)
        binding.optionVM = playOptionViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    private fun observeViewModel(vm: PlayOptionViewModel) {
        vm.launchLogin.observe(viewLifecycleOwner, {
            launchSignInFlow()
        })

        vm.launchLogout.observe(viewLifecycleOwner, {
            AuthUI.getInstance().signOut(requireContext())
        })

        vm.navigateToMainViewPager.observe(viewLifecycleOwner, {
            findNavController().navigate(
                PlayOptionFragmentDirections.actionPlayOptionFragmentToMainOnlineFragment()
            )
        })

        vm.navigateToOfflineGame.observe(viewLifecycleOwner, {
            findNavController().navigate(
                PlayOptionFragmentDirections.actionPlayOptionFragmentToBoardFragment()
            )
        })
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
//            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        signInResultHandler.launch(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        )
    }
}