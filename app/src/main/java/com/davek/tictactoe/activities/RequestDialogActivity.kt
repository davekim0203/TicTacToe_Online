package com.davek.tictactoe.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.davek.tictactoe.R
import com.davek.tictactoe.databinding.ActivityRequestDialogBinding
import com.davek.tictactoe.viewmodels.GameRequestViewModel

class RequestDialogActivity : AppCompatActivity() {

    private val viewModel: GameRequestViewModel by viewModels()
    private lateinit var binding: ActivityRequestDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_dialog)
        observeViewModel()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_request_dialog)
        binding.gameRequestVM = viewModel
        binding.lifecycleOwner = this
        val isSendingRequest = intent.getBooleanExtra(IS_SENDING_REQUEST_EXTRA_ID, true)
        val senderNickname = intent.getStringExtra(SENDER_NICKNAME_EXTRA_ID)
        val senderUserId = intent.getStringExtra(SENDER_USER_ID_EXTRA_ID)
        val recipientNickname = intent.getStringExtra(RECIPIENT_NICKNAME_EXTRA_ID)
        viewModel.initDialog(isSendingRequest, senderNickname, senderUserId, recipientNickname)
    }

    private fun observeViewModel() {
        viewModel.dismissRequestDialog.observe(this, {
            viewModel.removeGameRequestDocListener()
            finish()
        })
    }

    override fun onPause() {
        super.onPause()
        viewModel.removeGameRequestDoc()
        finish()
    }

    companion object {
        const val IS_SENDING_REQUEST_EXTRA_ID = "is_sending_request"
        const val SENDER_NICKNAME_EXTRA_ID = "sender_nickname"
        const val SENDER_USER_ID_EXTRA_ID = "sender_user_id"
        const val RECIPIENT_NICKNAME_EXTRA_ID = "recipient_nickname"
    }
}