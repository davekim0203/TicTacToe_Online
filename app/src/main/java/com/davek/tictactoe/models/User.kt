package com.davek.tictactoe.models

data class User(
        val nickname: String,
        val record: Int,
        val currentGame: String,
        var registrationToken: String
) {
    constructor() : this("", 0, "", "")
}