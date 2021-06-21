package com.davek.tictactoe.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Move(
    var moveIndex: Int,
    var row: Int,
    var col: Int,
    var playerNumber: Long
) : Parcelable {
    constructor() : this(0, -1, -1, -1)
}