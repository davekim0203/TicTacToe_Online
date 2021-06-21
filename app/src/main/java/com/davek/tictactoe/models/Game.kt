package com.davek.tictactoe.models

import com.davek.tictactoe.viewmodels.GameRequestViewModel.Companion.DEFAULT_LAST_MOVE_BY
import com.davek.tictactoe.viewmodels.GameSize

data class Game(
        val player1: String,
        val nicknamePlayer1: String,
        val player2: String,
        val nicknamePlayer2: String,
        val lastMoveBy: Long,
        val playerCount: Int,
        val gameSizeId: Long
) {
    constructor() : this("", "", "", "", DEFAULT_LAST_MOVE_BY, 0, GameSize.THREE_BY_THREE.sizeId)
}