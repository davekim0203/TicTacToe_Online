package com.davek.tictactoe.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.davek.tictactoe.models.Cell
import com.davek.tictactoe.models.Game
import com.davek.tictactoe.models.Move
import com.davek.tictactoe.models.SavedGame
import com.davek.tictactoe.utils.FireStoreUtils.authInstance
import com.davek.tictactoe.utils.FireStoreUtils.currentUserDocRef
import com.davek.tictactoe.utils.FireStoreUtils.currentUserId
import com.davek.tictactoe.utils.FireStoreUtils.gamesCollectionRef
import com.davek.tictactoe.utils.FireStoreUtils.usersCollectionRef
import com.davek.tictactoe.utils.SingleLiveEvent
import com.davek.tictactoe.viewmodels.GameRequestViewModel.Companion.DEFAULT_LAST_MOVE_BY
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import java.util.*

class BoardViewModel : ViewModel() {

    private val mTag = this::class.java.simpleName

    private val _onlineGameResult = MutableLiveData<GameResult>()
    val onlineGameResult: LiveData<GameResult>
        get() = _onlineGameResult

    private val _showQuitConfirmDialog = SingleLiveEvent<Any>()
    val showQuitConfirmDialog: LiveData<Any>
        get() = _showQuitConfirmDialog

    private val _offlineGameResult = MutableLiveData<OfflineGameResult>()
    val offlineGameResult: LiveData<OfflineGameResult>
        get() = _offlineGameResult

    private val _navigateUp = SingleLiveEvent<Any>()
    val navigateUp: LiveData<Any>
        get() = _navigateUp

    //TODO: Start new game in board fragment
    private val _requestedReplay = MutableLiveData<Boolean>()
    val requestedReplay: LiveData<Boolean>
        get() = _requestedReplay

    private val _gameSize = MutableLiveData<GameSize>()
    val gameSize: LiveData<GameSize>
        get() = _gameSize

    private val gameType = MutableLiveData<GameType>()

    private val _myNickname = MutableLiveData<String>()
    val myNickname: LiveData<String>
        get() = _myNickname

    private val _opponentNickname = MutableLiveData<String>()
    val opponentNickname: LiveData<String>
        get() = _opponentNickname

    private val _isCurrentPlayerO = MutableLiveData<Boolean>()
    val isCurrentPlayerO: LiveData<Boolean>
        get() = _isCurrentPlayerO

    private val _isCurrentPlayerTurn = MutableLiveData<Boolean?>()
    val isCurrentPlayerTurn: LiveData<Boolean?>
        get() = _isCurrentPlayerTurn

    private val allMovesLiveData = MutableLiveData<List<Move>>()
    private val allMoves = mutableListOf<Move>()
    private val currentOnlinePlayerMoves = mutableListOf<Move>()
    private val offlinePlayerOMoves = mutableListOf<Move>()
    private val offlinePlayerXMoves = mutableListOf<Move>()
    private val savedGameMoves = mutableListOf<Move>()
    private var savedGameMoveIndex = 0

    val isQuitButtonVisible = Transformations.map(gameType) {
        it == GameType.ONLINE || it == GameType.OFFLINE
    }
    val areArrowButtonsVisible = Transformations.map(gameType) {
        it == GameType.SAVED_GAME
    }
    val isGameSizeToggleEnabled = Transformations.map(allMovesLiveData) {
        val currentGameType = gameType.value
        if (currentGameType == null) {
            Log.e(mTag, "Error: game size value is null")
            false
        } else {
            (currentGameType == GameType.ONLINE || currentGameType == GameType.OFFLINE) && it.isEmpty()
        }
    }

    val cells: LiveData<List<Cell>> = Transformations.map(allMovesLiveData) {
        val cellList = mutableListOf<Cell>()
        val currentGameSize = gameSize.value
        if (currentGameSize == null) {
            Log.e(mTag, "Error: game size value is null")
        } else {
            for (r in 0 until currentGameSize.boardSpan) {
                for (c in 0 until currentGameSize.boardSpan) {
                    val moveToAdd = it.firstOrNull { move -> move.row == r && move.col == c }
                    if (moveToAdd == null) {
                        cellList.add(Cell(r, c, CellState.NONE))
                    } else {
                        val cellToSelect = Cell(
                            moveToAdd.row,
                            moveToAdd.col,
                            if (moveToAdd.playerNumber == 1L) {
                                CellState.SELECTED_O
                            } else {
                                CellState.SELECTED_X
                            }
                        )
                        cellList.add(cellToSelect)
                    }
                }
            }
        }
        cellList
    }

    private lateinit var gameDocRef: DocumentReference
    private lateinit var gameDocListenerRegistration: ListenerRegistration
    private lateinit var gameMovesCollectionRef: CollectionReference
    private lateinit var gameMovesListenerRegistration: ListenerRegistration

    private var currentPlayerNumber: Long = DEFAULT_PLAYER_NUMBER
    private var opponentPlayerNumber: Long = DEFAULT_PLAYER_NUMBER

    private var isGameInitialised = false

    fun initGame(gameId: String?, savedGame: SavedGame?, offlineReplay: Boolean) {
        if (offlineReplay) {
            initOfflineGame()
            return
        }

        if (!isGameInitialised) {
            if (gameId == null) {
                if (savedGame == null) {
                    initOfflineGame()
                } else {
                    initSavedGame(savedGame)
                }
            } else {
                initOnlineGame(gameId)
            }
            isGameInitialised = true
        }
    }

    fun onGameSizeThreeToggleClick() {
        setGameSize(GameSize.THREE_BY_THREE)
        if (gameType.value == GameType.OFFLINE) {
            _isCurrentPlayerTurn.value = true
        }
        if (gameType.value == GameType.ONLINE) {
            val gameSizeUpdate = mapOf("gameSizeId" to GameSize.THREE_BY_THREE.sizeId)
            gameDocRef.update(gameSizeUpdate)
        }
    }

    fun onGameSizeTenToggleClick() {
        setGameSize(GameSize.TEN_BY_TEN)
        if (gameType.value == GameType.OFFLINE) {
            _isCurrentPlayerTurn.value = true
        }
        if (gameType.value == GameType.ONLINE) {
            val gameSizeUpdate = mapOf("gameSizeId" to GameSize.TEN_BY_TEN.sizeId)
            gameDocRef.update(gameSizeUpdate)
        }
    }

    fun onCellClick(cell: Cell) {
        Log.i(mTag, "Clicked cell: $cell")
        val currentGameSize = gameSize.value
        if (currentGameSize == null) {
            Log.e(mTag, "Error: game size value is null")
            return
        }
        when (gameType.value) {
            null, GameType.SAVED_GAME -> { }
            GameType.ONLINE -> onCellClickOnline(cell, currentGameSize.boardSpan)
            GameType.OFFLINE -> onCellClickOffline(cell, currentGameSize.boardSpan)
        }
    }

    fun onQuitButtonClick() {
        _showQuitConfirmDialog.call()
    }

    fun onPreviousMoveClick() {
        if (savedGameMoveIndex > 0) {
            savedGameMoveIndex--
            allMoves.removeAt(savedGameMoveIndex)
            allMovesLiveData.value = allMoves
        }
    }

    fun onNextMoveClick() {
        if (savedGameMoveIndex >= 0 && savedGameMoveIndex < savedGameMoves.size) {
            allMoves.add(savedGameMoves[savedGameMoveIndex])
            allMovesLiveData.value = allMoves
            savedGameMoveIndex++
        }
    }

    fun quitGame() {
        when (gameType.value) {
            GameType.ONLINE -> concedeGame()
            GameType.OFFLINE -> initGame(null, null, true)
            else -> {
            }
        }
    }

    fun handleGameEndDialogResponse(wantSave: Boolean?, wantReplay: Boolean?) {
        wantSave?.let {
            if (it) saveGame()
        }

        if (wantReplay != null && wantReplay) {
            sendReplayRequest()
        } else {
            leaveGameRoom()
            _navigateUp.call()
        }
    }

    private fun initOnlineGame(gameId: String) {
        gameType.value = GameType.ONLINE
        _onlineGameResult.value = GameResult.IN_GAME
        _requestedReplay.value = false
        setGameSize(DEFAULT_GAME_SIZE)
        currentOnlinePlayerMoves.clear()
        gameDocRef = gamesCollectionRef.document(gameId)
        gameDocRef.update("playerCount", FieldValue.increment(1))
        setGameDocListener(gameDocRef)
        gameMovesCollectionRef = gameDocRef.collection("moves")
        setMovesDocListener(gameMovesCollectionRef)
    }

    private fun initOfflineGame() {
        gameType.value = GameType.OFFLINE
        setGameSize(gameSize.value ?: DEFAULT_GAME_SIZE)
        _isCurrentPlayerTurn.value = true
        _isCurrentPlayerO.value = true
        _offlineGameResult.value = OfflineGameResult.IN_GAME
        offlinePlayerOMoves.clear()
        offlinePlayerXMoves.clear()
    }

    private fun initSavedGame(savedGame: SavedGame) {
        Log.i(mTag, "Saved game: $savedGame")
        gameType.value = GameType.SAVED_GAME
        setGameSize(savedGame.gameSizeId)
        currentPlayerNumber = savedGame.myPlayerNumber
        if (currentPlayerNumber == 1L) {
            _isCurrentPlayerO.value = true
            opponentPlayerNumber = 2L
        } else {
            _isCurrentPlayerO.value = false
            opponentPlayerNumber = 1L
        }
        _myNickname.value = savedGame.myNickname
        _opponentNickname.value = savedGame.opponentNickname

        for (move in savedGame.moves) {
            savedGameMoves.add(move)
        }
    }

    private fun resetMoves() {
        allMoves.clear()
        allMovesLiveData.value = allMoves
    }

    private fun onCellClickOnline(cell: Cell, boardSize: Int) {
        val isAlreadySelected = allMoves.any { it.row == cell.row && it.col == cell.col }
        if (isAlreadySelected) return
        val isYourTurn = isCurrentPlayerTurn.value
        if (isYourTurn == null || isYourTurn) {
            val newMove = mapOf(
                "moveIndex" to allMoves.size + 1,
                "row" to cell.row,
                "col" to cell.col,
                "playerNumber" to currentPlayerNumber,
            )
            gameMovesCollectionRef.add(newMove)

            val gameDocUpdate = if (checkWin(cell, currentOnlinePlayerMoves)) {
                mapOf(
                    "lastMoveBy" to currentPlayerNumber,
                    "winner" to currentPlayerNumber
                )
            } else {
                if (allMoves.size == boardSize * boardSize - 1) {
                    mapOf(
                        "lastMoveBy" to currentPlayerNumber,
                        "winner" to DEFAULT_PLAYER_NUMBER
                    )
                } else {
                    mapOf("lastMoveBy" to currentPlayerNumber)
                }
            }
            gameDocRef.update(gameDocUpdate)
        }
    }

    private fun onCellClickOffline(cell: Cell, boardSize: Int) {
        val isAlreadySelected = allMoves.any { it.row == cell.row && it.col == cell.col }
        if (isAlreadySelected) return
        if (allMoves.size % 2 == 0) {
            val newMove = Move(
                allMoves.size + 1,
                cell.row,
                cell.col,
                1
            )
            allMoves.add(newMove)
            allMovesLiveData.value = allMoves
            offlinePlayerOMoves.add(newMove)
            if (checkWin(cell, offlinePlayerOMoves)) {
                _offlineGameResult.value = OfflineGameResult.WIN_O
                return
            }
            _isCurrentPlayerTurn.value = false
        } else {
            val newMove = Move(
                allMoves.size + 1,
                cell.row,
                cell.col,
                2
            )
            allMoves.add(newMove)
            allMovesLiveData.value = allMoves
            offlinePlayerXMoves.add(newMove)
            if (checkWin(cell, offlinePlayerXMoves)) {
                _offlineGameResult.value = OfflineGameResult.WIN_X
                return
            }
            _isCurrentPlayerTurn.value = true
        }

        if (allMoves.size == boardSize * boardSize) {
            _offlineGameResult.value = OfflineGameResult.TIE
        }
    }

    private fun setGameSize(size: GameSize) {
        _gameSize.value = size
        resetMoves()
    }

    private fun setGameSize(sizeId: Long) {
        setGameSize(
            if (sizeId == GameSize.THREE_BY_THREE.sizeId) GameSize.THREE_BY_THREE
            else GameSize.TEN_BY_TEN
        )
    }

    private fun setGameDocListener(docRef: DocumentReference) {
        gameDocListenerRegistration = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(mTag, "Error: setGameDocListener Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.i(mTag, "Game data: ${snapshot.data}")

                val winnerPlayerNumber = snapshot.data?.get("winner")
                if (winnerPlayerNumber != null) {
                    val currentOnlineGameResult = onlineGameResult.value
                    if (currentOnlineGameResult == null || currentOnlineGameResult == GameResult.IN_GAME) {
                        if (winnerPlayerNumber is Long) {
                            val userUpdate = mapOf("currentGame" to "")
                            currentUserDocRef.update(userUpdate)
                            when (winnerPlayerNumber) {
                                DEFAULT_PLAYER_NUMBER -> _onlineGameResult.value = GameResult.TIE
                                currentPlayerNumber -> _onlineGameResult.value = GameResult.WIN
                                opponentPlayerNumber -> _onlineGameResult.value = GameResult.LOSE
                            }
                        } else {
                            Log.e(
                                mTag,
                                "Error: winnerPlayerNumber is not valid: $winnerPlayerNumber"
                            )
                        }
                    }
                } else {
                    if (currentPlayerNumber == DEFAULT_PLAYER_NUMBER ||
                        opponentPlayerNumber == DEFAULT_PLAYER_NUMBER
                    ) {
                        val nicknamePlayer1 = snapshot.data?.get("nicknamePlayer1")
                        val nicknamePlayer2 = snapshot.data?.get("nicknamePlayer2")

                        authInstance.currentUser?.uid?.let {
                            if (it == snapshot.data?.get("player1")) {
                                currentPlayerNumber = 1L
                                _isCurrentPlayerO.value = true
                                if (nicknamePlayer1 != null && nicknamePlayer1 is String) {
                                    _myNickname.value = nicknamePlayer1!!
                                }
                                opponentPlayerNumber = 2L
                                if (nicknamePlayer2 != null && nicknamePlayer2 is String) {
                                    _opponentNickname.value = nicknamePlayer2!!
                                }
                            } else if (it == snapshot.data?.get("player2")) {
                                currentPlayerNumber = 2L
                                _isCurrentPlayerO.value = false
                                if (nicknamePlayer2 != null && nicknamePlayer2 is String) {
                                    _myNickname.value = nicknamePlayer2!!
                                }
                                opponentPlayerNumber = 1L
                                if (nicknamePlayer1 != null && nicknamePlayer1 is String) {
                                    _opponentNickname.value = nicknamePlayer1!!
                                }
                            }
                            val lastMoveBy = snapshot.data?.get("lastMoveBy")
                            if (lastMoveBy == DEFAULT_LAST_MOVE_BY) {
                                _isCurrentPlayerTurn.value = null
                            }
                            if (lastMoveBy == opponentPlayerNumber) {
                                _isCurrentPlayerTurn.value = true
                            }
                        }
                    }

                    val gameSizeIdFromDb = snapshot.data?.get("gameSizeId")
                    val currentGameSize = gameSize.value
                    if (gameSizeIdFromDb == null) {
                        if (currentGameSize != DEFAULT_GAME_SIZE) {
                            setGameSize(DEFAULT_GAME_SIZE)
                        }
                    } else {
                        if (gameSizeIdFromDb is Long && currentGameSize?.sizeId != gameSizeIdFromDb) {
                            setGameSize(gameSizeIdFromDb)
                        }
                    }
                }
            } else {
                Log.i(mTag, "Game data: null")
                removeListeners()
                val userUpdate = mapOf("currentGame" to "")
                currentUserDocRef.update(userUpdate)
                _navigateUp.call()
            }
        }
    }

    private fun setMovesDocListener(colRef: CollectionReference) {
        gameMovesListenerRegistration = colRef
            .orderBy("moveIndex")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(mTag, "Error: setMovesDocListener listen failed", e)
                    return@addSnapshotListener
                }
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            Log.d(mTag, "Added move doc: ${dc.document.data}")
                            val move = dc.document.toObject(Move::class.java)
                            if (move.playerNumber == currentPlayerNumber) {
                                currentOnlinePlayerMoves.add(move)
                            }
                            allMoves.add(move)
                            _isCurrentPlayerTurn.value =
                                currentPlayerNumber != DEFAULT_PLAYER_NUMBER &&
                                        currentPlayerNumber != move.playerNumber
                        }
                        else -> {
                            Log.e(
                                mTag,
                                "Error: Modify or Delete is not allowed: ${dc.document.data}"
                            )
                        }
                    }
                }
                allMovesLiveData.value = allMoves
            }
    }

    private fun sendReplayRequest() {
        gameDocRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val playerCount = document.data?.get("playerCount")
                if (playerCount != null && playerCount is Long) {
                    if (playerCount < 2L) {
                        //The opponent already left the current game
                        val replayRequestBy = document.data?.get("replayRequestBy")
                        if (replayRequestBy == null) {
                            //The opponent decided to finish
                            leaveGameRoom()
                            _navigateUp.call()
                        } else {
                            //The opponent requested replay
                            val opponentUserId = document.data?.get(
                                if (opponentPlayerNumber == 1L) "player1" else "player2"
                            )
                            if (opponentUserId is String) {
                                if (replayRequestBy is String && replayRequestBy == opponentUserId) {
                                    deleteAtPath(gameDocRef.path)       //Delete the current game
                                    createGame(opponentUserId)      //Create new game
                                    _navigateUp.call()
                                } else {
                                    Log.e(
                                        mTag,
                                        "Error: replayRequestBy is not valid: $replayRequestBy"
                                    )
                                }
                            } else {
                                Log.e(mTag, "Error: opponent id is not valid")
                            }
                        }
                    } else {
                        //The opponent hasn't decided to replay or quit
                        val updateRequest = mapOf(
                            "replayRequestBy" to currentUserId,
                            "playerCount" to FieldValue.increment(-1)
                        )
                        gameDocRef.update(updateRequest)
                        _requestedReplay.value = true
                        //TODO: waiting for the opponent dialog
                        _navigateUp.call()
                    }
                } else {
                    Log.e(mTag, "Error: playerCount is not valid: $playerCount")
                }
            }
        }
    }

    private fun createGame(opponentUserId: String) {
        val gameDocRef = gamesCollectionRef.document()
        val newGameId = gameDocRef.id
        val currentMyNickname = myNickname.value
        val currentOpponentNickname = opponentNickname.value
        if (currentMyNickname == null || currentOpponentNickname == null) {
            Log.e(
                mTag,
                "Error: null of currentMyNickname:$currentMyNickname, currentOpponentNickname:$currentOpponentNickname"
            )
            return
        }

        gameDocRef.set(
            Game(
                currentUserId,
                currentMyNickname,
                opponentUserId,
                currentOpponentNickname,
                DEFAULT_LAST_MOVE_BY,
                0,
                GameSize.THREE_BY_THREE.sizeId
            )
        )

        val userUpdate = mapOf("currentGame" to newGameId)
        currentUserDocRef.update(userUpdate)
        usersCollectionRef.document(opponentUserId).update(userUpdate)
    }

    private fun concedeGame() {
        val gameDocUpdate = mapOf(
            "conceded" to true,
            "winner" to opponentPlayerNumber
        )
        gameDocRef.update(gameDocUpdate)
    }

    private fun saveGame() {
        val currentGameSize = gameSize.value
        if (currentGameSize == null) {
            Log.e(mTag, "Error: currentGameSize is null")
            return
        }
        val currentOnlineGameResult = onlineGameResult.value
        val currentMyNickname = myNickname.value
        val currentOpponentNickname = opponentNickname.value
        if (currentOnlineGameResult == null) {
            Log.e(mTag, "Error: currentOnlineGameResult is null")
            return
        }
        if (currentMyNickname == null) {
            Log.e(mTag, "Error: currentMyNickname is null")
            return
        }
        if (currentOpponentNickname == null) {
            Log.e(mTag, "Error: currentOpponentNickname is null")
            return
        }
        val gameToSave = SavedGame(
            currentOnlineGameResult.resultId,
            currentGameSize.sizeId,
            currentPlayerNumber,
            currentMyNickname,
            currentOpponentNickname,
            allMoves,
            Timestamp(Date())
        )
        currentUserDocRef.collection("savedGames").document().set(gameToSave)
    }

    private fun checkWin(newSelectedCell: Cell, alreadyTakenMoves: MutableList<Move>): Boolean {
        val currentGameSize = gameSize.value
        if (currentGameSize == null) {
            Log.e(mTag, "Error: game size value is null")
            return false
        }
        if (checkLeftDiagonal(
                newSelectedCell,
                alreadyTakenMoves,
                currentGameSize.winLineSize
            )
        ) return true
        if (checkRightDiagonal(
                newSelectedCell,
                alreadyTakenMoves,
                currentGameSize.winLineSize
            )
        ) return true
        if (checkVertical(
                newSelectedCell,
                alreadyTakenMoves,
                currentGameSize.winLineSize
            )
        ) return true
        if (checkHorizontal(
                newSelectedCell,
                alreadyTakenMoves,
                currentGameSize.winLineSize
            )
        ) return true
        return false
    }

    private fun checkLeftDiagonal(
        newSelectedCell: Cell,
        alreadyTakenMoves: MutableList<Move>,
        winLineSize: Int
    ): Boolean {
        val filterLeftTop = { row: Int, col: Int, index: Int ->
            row == newSelectedCell.row - index && col == newSelectedCell.col - index
        }
        val filterRightBottom = { row: Int, col: Int, index: Int ->
            row == newSelectedCell.row + index && col == newSelectedCell.col + index
        }
        return getConnectedCellsCount(
            getConnectedCellsCount(0, filterLeftTop, alreadyTakenMoves, winLineSize),
            filterRightBottom,
            alreadyTakenMoves,
            winLineSize
        ) == winLineSize - 1
    }

    private fun checkRightDiagonal(
        newSelectedCell: Cell,
        alreadyTakenMoves: MutableList<Move>,
        winLineSize: Int
    ): Boolean {
        val filterRightTop = { row: Int, col: Int, index: Int ->
            row == newSelectedCell.row - index && col == newSelectedCell.col + index
        }
        val filterLeftBottom = { row: Int, col: Int, index: Int ->
            row == newSelectedCell.row + index && col == newSelectedCell.col - index
        }
        return getConnectedCellsCount(
            getConnectedCellsCount(0, filterRightTop, alreadyTakenMoves, winLineSize),
            filterLeftBottom,
            alreadyTakenMoves,
            winLineSize
        ) == winLineSize - 1
    }

    private fun checkVertical(
        newSelectedCell: Cell,
        alreadyTakenMoves: MutableList<Move>,
        winLineSize: Int
    ): Boolean {
        val filterTop = { row: Int, col: Int, index: Int ->
            row == newSelectedCell.row - index && col == newSelectedCell.col
        }
        val filterBottom = { row: Int, col: Int, index: Int ->
            row == newSelectedCell.row + index && col == newSelectedCell.col
        }
        return getConnectedCellsCount(
            getConnectedCellsCount(0, filterTop, alreadyTakenMoves, winLineSize),
            filterBottom,
            alreadyTakenMoves,
            winLineSize
        ) == winLineSize - 1
    }

    private fun checkHorizontal(
        newSelectedCell: Cell,
        alreadyTakenMoves: MutableList<Move>,
        winLineSize: Int
    ): Boolean {
        val filterTop = { row: Int, col: Int, index: Int ->
            row == newSelectedCell.row && col == newSelectedCell.col - index
        }
        val filterBottom = { row: Int, col: Int, index: Int ->
            row == newSelectedCell.row && col == newSelectedCell.col + index
        }
        return getConnectedCellsCount(
            getConnectedCellsCount(0, filterTop, alreadyTakenMoves, winLineSize),
            filterBottom,
            alreadyTakenMoves,
            winLineSize
        ) == winLineSize - 1
    }

    private fun getConnectedCellsCount(
        startCount: Int,
        filter: (Int, Int, Int) -> Boolean,
        alreadyTakenMoves: MutableList<Move>,
        winLineSize: Int
    ): Int {
        var i = 1
        var count = startCount
        while (true) {
            if (!alreadyTakenMoves.any { filter(it.row, it.col, i) }) {
                break
            }
            count++
            if (count == winLineSize - 1) {
                Log.i(mTag, "Game over!")
                break
            }
            i++
        }

        return count
    }

    private fun removeListeners() {
        _isCurrentPlayerTurn.value = false
        if (gameType.value == GameType.ONLINE) {
            if (this::gameDocListenerRegistration.isInitialized) {
                gameDocListenerRegistration.remove()
            }
            if (this::gameMovesListenerRegistration.isInitialized) {
                gameMovesListenerRegistration.remove()
            }
        }
    }

    private fun leaveGameRoom() {
        gameDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val gameRoomPlayerCount = document.data?.get("playerCount")
                    if (gameRoomPlayerCount is Long) {
                        if (gameRoomPlayerCount <= 1L) {
                            deleteAtPath(gameDocRef.path)
                        } else {
                            gameDocRef.update("playerCount", FieldValue.increment(-1))
                        }
                    } else {
                        Log.e(mTag, "Error: playerCount must be Long")
                    }
                }
            }
    }

    private fun deleteAtPath(path: String) {
        val deleteFn = Firebase.functions.getHttpsCallable("recursiveDelete")
        deleteFn.call(hashMapOf("path" to path))
    }

    enum class GameType {
        ONLINE, OFFLINE, SAVED_GAME
    }

    companion object {
        private const val DEFAULT_PLAYER_NUMBER: Long = 0
        private val DEFAULT_GAME_SIZE = GameSize.THREE_BY_THREE
    }
}

enum class CellState {
    NONE, SELECTED_O, SELECTED_X
}

enum class GameResult(val resultId: Long) {
    WIN(0L),
    LOSE(1L),
    TIE(2L),
    IN_GAME(3L)
}

enum class OfflineGameResult {
    WIN_O, WIN_X, TIE, IN_GAME
}

enum class GameSize(val sizeId: Long, val boardSpan: Int, val winLineSize: Int) {
    THREE_BY_THREE(0L, 3, 3),
    TEN_BY_TEN(1L, 10, 5)
}