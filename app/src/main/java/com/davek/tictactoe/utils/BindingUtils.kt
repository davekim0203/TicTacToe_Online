package com.davek.tictactoe.utils

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.davek.tictactoe.R
import com.davek.tictactoe.adapters.CellAdapter
import com.davek.tictactoe.models.Cell
import com.davek.tictactoe.viewmodels.GameResult
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("cells")
fun setCellList(listView: RecyclerView, items: List<Cell>?) {
    items?.let {
        (listView.adapter as CellAdapter).data = (it)
    }
}

@BindingAdapter("currentUserNickname")
fun TextView.setCurrentUserNicknameText(nickname: String?) {
    nickname?.let {
        text = String.format(
            resources.getString(R.string.search_opponent_fragment_title),
            it
        )
    }
}

@BindingAdapter("requestDialogMessage")
fun TextView.setGameRequestDialogMessage(requestInfoForMessage: Pair<Boolean, String>?) {
    requestInfoForMessage?.let {
        val isSendingRequest = it.first
        val opponentNickname = it.second
        text = String.format(
            resources.getString(
                if (isSendingRequest) R.string.dialog_game_request_sending_message
                else R.string.dialog_game_request_receiving_message
            ),
            opponentNickname
        )
    }
}

@BindingAdapter("gameEndDialogTitle")
fun TextView.setGameEndDialogTitle(gameResult: GameResult?) {
    gameResult?.let {
        text = resources.getString(
            when (it) {
                GameResult.WIN -> R.string.dialog_result_title_win
                GameResult.LOSE -> R.string.dialog_result_title_lose
                GameResult.TIE -> R.string.dialog_result_title_tie
                GameResult.IN_GAME -> return
            }
        )
    }
}

@BindingAdapter("gameResultText")
fun TextView.setGameResultText(resultId: Long?) {
    resultId?.let {
        text = resources.getString(
            when (resultId) {
                GameResult.WIN.resultId -> R.string.saved_games_fragment_game_result_won
                GameResult.LOSE.resultId -> R.string.saved_games_fragment_game_result_lost
                GameResult.TIE.resultId -> R.string.saved_games_fragment_game_result_tie
                else -> R.string.empty_string
            }
        )
    }
}

@BindingAdapter("gameResultBarColor")
fun View.setGameResultBarColor(resultId: Long?) {
    resultId?.let {
        setBackgroundColor(
            ContextCompat.getColor(
                context,
                when (resultId) {
                    GameResult.WIN.resultId -> R.color.game_result_won
                    GameResult.LOSE.resultId -> R.color.game_result_lost
                    GameResult.TIE.resultId -> R.color.game_result_tie
                    else -> R.color.white
                }
            )
        )
    }
}

@BindingAdapter("gameMoveCountText")
fun TextView.setGameMoveCountText(moveCount: Int?) {
    moveCount?.let {
        text = String.format(
            resources.getString(R.string.saved_games_fragment_game_move_count),
            it
        )
    }
}

@BindingAdapter("savedGameDateText")
fun TextView.setSavedGameDateText(date: Date?) {
    date?.let {
        text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
    }
}

@BindingAdapter("playerStatus")
fun TextView.setPlayerStatus(isItsTurn: Boolean?) {
    val background = ContextCompat.getDrawable(
        context,
        if (isItsTurn == null || isItsTurn) R.drawable.player_status_current_turn
        else R.drawable.player_status_not_current_turn
    )
    setBackground(background)
}

@BindingAdapter("currentPlayerSign")
fun TextView.setCurrentPlayerSign(isCurrentPlayerO: Boolean?) {
    isCurrentPlayerO?.let {
        setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            if (it) R.drawable.ic_outline_o_24 else R.drawable.ic_outline_x_24,
            0
        )
    }
}

@BindingAdapter("opponentPlayerSign")
fun TextView.setOpponentPlayerSign(isCurrentPlayerO: Boolean?) {
    isCurrentPlayerO?.let {
        setCompoundDrawablesWithIntrinsicBounds(
            if (it) R.drawable.ic_outline_x_24 else R.drawable.ic_outline_o_24,
            0,
            0,
            0
        )
    }
}

@BindingAdapter("onSavedGameLongPressed")
fun View.setOnSavedGameLongClickListener(
    func: () -> Unit
) {
    setOnLongClickListener {
        func()
        return@setOnLongClickListener true
    }
}