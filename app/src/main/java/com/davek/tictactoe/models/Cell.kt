package com.davek.tictactoe.models

import com.davek.tictactoe.viewmodels.CellState

data class Cell(
        var row: Int,
        var col: Int,
        var currentState: CellState
)