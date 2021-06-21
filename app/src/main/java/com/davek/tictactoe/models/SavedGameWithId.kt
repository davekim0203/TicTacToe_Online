package com.davek.tictactoe.models

data class SavedGameWithId(
    val gameDocId: String,
    val savedGame: SavedGame
)
