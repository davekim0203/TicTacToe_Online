package com.davek.tictactoe.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.davek.tictactoe.R
import com.davek.tictactoe.databinding.ActivityGameEndDialogBinding
import com.davek.tictactoe.viewmodels.GameEndViewModel
import com.davek.tictactoe.viewmodels.GameResult

class GameEndDialogActivity : AppCompatActivity() {

    private val viewModel: GameEndViewModel by viewModels()
    private lateinit var binding: ActivityGameEndDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_end_dialog)
        this.setFinishOnTouchOutside(false)
        observeViewModel()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_game_end_dialog)
        binding.gameEndVM = viewModel
        binding.lifecycleOwner = this
        val gameResult = intent.getSerializableExtra(GAME_RESULT_EXTRA_ID)
        if (gameResult is GameResult) {
            viewModel.initDialog(gameResult)
        }
    }

    private fun observeViewModel() {
        viewModel.finishDialog.observe(this, {
            val intent = Intent().apply {
                putExtra(NEED_TO_SAVE_EXTRA_ID, it.first)
                putExtra(NEED_TO_SEND_REPLY_REQUEST_EXTRA_ID, it.second)
            }
            setResult(GAME_END_DIALOG_REQUEST_CODE, intent)
            finish()
        })
    }

    companion object {
        const val GAME_RESULT_EXTRA_ID = "game_result"
        const val NEED_TO_SAVE_EXTRA_ID = "need_to_save"
        const val NEED_TO_SEND_REPLY_REQUEST_EXTRA_ID = "need_to_send_replay_request"
        const val GAME_END_DIALOG_REQUEST_CODE = 100
    }
}