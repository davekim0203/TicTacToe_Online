package com.davek.tictactoe.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.davek.tictactoe.utils.FirebaseUserLiveData
import com.davek.tictactoe.utils.SingleLiveEvent

class PlayOptionViewModel : ViewModel() {

    private val _navigateToMainViewPager = SingleLiveEvent<Any>()
    val navigateToMainViewPager: LiveData<Any>
        get() = _navigateToMainViewPager

    private val _navigateToOfflineGame = SingleLiveEvent<Any>()
    val navigateToOfflineGame: LiveData<Any>
        get() = _navigateToOfflineGame

    private val _launchLogin = SingleLiveEvent<Any>()
    val launchLogin: LiveData<Any>
        get() = _launchLogin

    private val _launchLogout = SingleLiveEvent<Any>()
    val launchLogout: LiveData<Any>
        get() = _launchLogout

    private val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    val isLoggedIn = Transformations.map(authenticationState) {
        it == AuthenticationState.AUTHENTICATED
    }

    fun onPlayOfflineClick() {
        _navigateToOfflineGame.call()
    }

    fun onPlayOnlineClick() {
        _navigateToMainViewPager.call()
    }

    fun onLoginClick() {
        when (authenticationState.value) {
            AuthenticationState.AUTHENTICATED -> _launchLogout.call()
            else -> _launchLogin.call()
        }
    }

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }
}