package com.davek.tictactoe.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.davek.tictactoe.models.SavedGame
import com.davek.tictactoe.utils.FireStoreUtils.currentUserDocRef
import com.davek.tictactoe.utils.SingleLiveEvent
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class SavedGamesViewModel : ViewModel() {

    private val _navigateToGameBoard = SingleLiveEvent<SavedGame>()
    val navigateToGameBoard: LiveData<SavedGame>
        get() = _navigateToGameBoard

    private val _showDeleteSavedGameDialog = SingleLiveEvent<String>()
    val showDeleteSavedGameDialog: LiveData<String>
        get() = _showDeleteSavedGameDialog

    fun onSavedGameClick(savedGame: SavedGame) {
        _navigateToGameBoard.value = savedGame
    }

    fun onSavedGameLongPress(savedGameDocId: String) {
        _showDeleteSavedGameDialog.value = savedGameDocId
    }

    fun deleteSavedGame(gameDocId: String) {
        val path = currentUserDocRef
            .collection("savedGames")
            .document(gameDocId)
            .path
        val deleteFn = Firebase.functions.getHttpsCallable("recursiveDelete")
        deleteFn.call(hashMapOf("path" to path))
    }
}