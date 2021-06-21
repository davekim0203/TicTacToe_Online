package com.davek.tictactoe.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.davek.tictactoe.models.Game
import com.davek.tictactoe.utils.FireStoreUtils.currentUserDocRef
import com.davek.tictactoe.utils.FireStoreUtils.currentUserId
import com.davek.tictactoe.utils.FireStoreUtils.gameRequestCollectionRef
import com.davek.tictactoe.utils.FireStoreUtils.gamesCollectionRef
import com.davek.tictactoe.utils.FireStoreUtils.usersCollectionRef
import com.davek.tictactoe.utils.SingleLiveEvent
import com.google.firebase.firestore.ListenerRegistration

class GameRequestViewModel : ViewModel() {

    private val mTag = this::class.java.simpleName

    private val _isSending = MutableLiveData<Boolean>()
    val isSending: LiveData<Boolean>
        get() = _isSending

    private val _requestInfoForMessage = MutableLiveData<Pair<Boolean, String>>()
    val requestInfoForMessage: LiveData<Pair<Boolean, String>>
        get() = _requestInfoForMessage

    private val _dismissRequestDialog = SingleLiveEvent<Any>()
    val dismissRequestDialog: LiveData<Any>
        get() = _dismissRequestDialog

    private var opponentUserId: String? = null
    private var opponentNickname: String? = null
    private var currentUserNickname: String? = null
    private lateinit var requestDocListenerRegistration: ListenerRegistration

    fun initDialog(
        isSendingRequest: Boolean,
        senderNickname: String?,
        senderId: String?,
        recipientNickname: String?
    ) {
        _isSending.value = isSendingRequest
        if (recipientNickname == null) {
            Log.e(mTag, "Error: recipientNickname is null")
            return
        }
        if (isSendingRequest) {
            _requestInfoForMessage.value = Pair(isSendingRequest, recipientNickname)
            addGameRequestDocListener(isSendingRequest, "")
        } else {
            if (senderId == null || senderNickname == null) {
                Log.e(
                    mTag,
                    "Error: null value of opponentId:$senderId or opponentNickname:$senderNickname"
                )
                return
            }
            _requestInfoForMessage.value = Pair(isSendingRequest, senderNickname)
            opponentUserId = senderId
            opponentNickname = senderNickname
            currentUserNickname = recipientNickname
            addGameRequestDocListener(isSendingRequest, senderId)
        }
    }

    fun onDeclineButtonClick() {
        Log.i(mTag, "onDeclineButtonClick")
        removeGameRequestDoc()
    }

    fun onAcceptButtonClick() {
        Log.i(mTag, "onAcceptButtonClick")
        val currentIsSending = isSending.value
        if (currentIsSending == false) {
            val currentOpponentUserId = opponentUserId
            val currentOpponentNickname = opponentNickname
            val currentCurrentUserNickname = currentUserNickname


            if (currentOpponentUserId != null &&
                currentOpponentNickname != null &&
                currentCurrentUserNickname != null
            ) {
                createGame(
                    currentOpponentUserId,
                    currentOpponentNickname,
                    currentCurrentUserNickname
                )
                removeGameRequestDoc()
            }
        }
    }

    fun removeGameRequestDoc() {
        val currentIsSending = isSending.value ?: return
        val documentId = if (currentIsSending) {
            currentUserId
        } else {
            opponentUserId ?: return
        }
        gameRequestCollectionRef.document(documentId).delete().addOnCompleteListener {
            Log.i(mTag, "removed game request document")
            removeGameRequestDocListener()
            _dismissRequestDialog.call()
        }
    }

    fun removeGameRequestDocListener() {
        requestDocListenerRegistration.remove()
    }

    private fun createGame(
        opponentUserId: String,
        opponentNickname: String,
        currentUserNickname: String
    ) {
        Log.i(mTag, "createGame")

        val gameDocRef = gamesCollectionRef.document()
        val newGameId = gameDocRef.id
        gameDocRef.set(
            Game(
                currentUserId,
                currentUserNickname,
                opponentUserId,
                opponentNickname,
                DEFAULT_LAST_MOVE_BY,
                0,
                GameSize.THREE_BY_THREE.sizeId
            )
        )

        val userUpdate = mapOf("currentGame" to newGameId)
        currentUserDocRef.update(userUpdate)
        usersCollectionRef.document(opponentUserId).update(userUpdate)
    }

    private fun addGameRequestDocListener(isSendingRequest: Boolean, opponentUserId: String) {
        val documentId = if (isSendingRequest) currentUserId else opponentUserId
        requestDocListenerRegistration = gameRequestCollectionRef
            .document(documentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(mTag, "Error: addGameRequestDocListener listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    Log.i(mTag, "Game request removed from list")
                    _dismissRequestDialog.call()
                }
            }
    }

    companion object {
        const val DEFAULT_LAST_MOVE_BY: Long = -1
    }
}