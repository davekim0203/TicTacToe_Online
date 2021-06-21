package com.davek.tictactoe.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.davek.tictactoe.utils.SingleLiveEvent

class GameEndViewModel : ViewModel() {

    private val mTag = this::class.java.simpleName

    private val _gameResult = MutableLiveData<GameResult>()
    val gameResult: LiveData<GameResult>
        get() = _gameResult

    private val _isSaveButtonEnabled = MutableLiveData<Boolean>()
    val isSaveButtonEnabled: LiveData<Boolean>
        get() = _isSaveButtonEnabled

    /**
     * first: needToSave
     * second: needToSendReplayRequest
     */
    private val _finishDialog = SingleLiveEvent<Pair<Boolean, Boolean>>()
    val finishDialog: LiveData<Pair<Boolean, Boolean>>
        get() = _finishDialog

    fun initDialog(gameResult: GameResult) {
        _gameResult.value = gameResult
        _isSaveButtonEnabled.value = true
    }

    fun onSaveButtonClick() {
        _isSaveButtonEnabled.value = false
    }

    fun onFinishButtonClick() {
        val currentNeedToSave = isSaveButtonEnabled.value
        if (currentNeedToSave == null) {
            Log.e(mTag, "Error: currentNeedToSave is null at onFinishButtonClick")
            return
        }
        _finishDialog.value = Pair(!currentNeedToSave, false)
    }

    fun onReplayButtonClick() {
        val currentNeedToSave = isSaveButtonEnabled.value
        if (currentNeedToSave == null) {
            Log.e(mTag, "Error: currentNeedToSave is null at onReplayButtonClick")
            return
        }
        _finishDialog.value = Pair(!currentNeedToSave, true)
    }
}