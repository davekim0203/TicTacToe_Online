package com.davek.tictactoe.models

import android.os.Parcelable
import com.davek.tictactoe.viewmodels.GameResult
import com.davek.tictactoe.viewmodels.GameSize
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class SavedGame(
        val gameResultId: Long,
        val gameSizeId: Long,
        val myPlayerNumber: Long,
        val myNickname: String,
        val opponentNickname: String,
        val moves: List<Move>,
        val date: Timestamp
) : Parcelable {
    constructor() : this(
            GameResult.IN_GAME.resultId,
            GameSize.THREE_BY_THREE.sizeId,
            0L,
            "",
            "",
            listOf(),
            Timestamp(Date())
    )
}