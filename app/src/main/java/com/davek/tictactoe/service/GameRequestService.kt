package com.davek.tictactoe.service

import android.content.Intent
import android.util.Log
import com.davek.tictactoe.activities.RequestDialogActivity
import com.davek.tictactoe.activities.RequestDialogActivity.Companion.IS_SENDING_REQUEST_EXTRA_ID
import com.davek.tictactoe.activities.RequestDialogActivity.Companion.RECIPIENT_NICKNAME_EXTRA_ID
import com.davek.tictactoe.activities.RequestDialogActivity.Companion.SENDER_NICKNAME_EXTRA_ID
import com.davek.tictactoe.activities.RequestDialogActivity.Companion.SENDER_USER_ID_EXTRA_ID
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GameRequestService : FirebaseMessagingService() {

    private val mTag = this::class.java.simpleName

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(mTag, "New Token added: $token")
//        val tokenUpdate = mapOf("registrationToken" to token)
//        try {
//            currentUserDocRef.update(tokenUpdate)
//        } catch (e: ExceptionInInitializerError) {
//            Log.e("TAG", "Error: current user token not updated: $e")
//        }
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        val data = p0.data
        val senderNickname = data["senderNickname"]
        val senderUserId = data["senderUserId"]
        val recipientNickname = data["recipientNickname"]

        Log.i(mTag, "Received senderNickname: $senderNickname")
        Log.i(mTag, "Received senderUserId: $senderUserId")
        Log.i(mTag, "Received recipientNickname: $recipientNickname")

        val intent = Intent(this, RequestDialogActivity::class.java).apply {
            putExtra(IS_SENDING_REQUEST_EXTRA_ID, false)
            putExtra(SENDER_NICKNAME_EXTRA_ID, senderNickname)
            putExtra(SENDER_USER_ID_EXTRA_ID, senderUserId)
            putExtra(RECIPIENT_NICKNAME_EXTRA_ID, recipientNickname)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}