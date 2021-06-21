package com.davek.tictactoe.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.davek.tictactoe.models.PlayerItem
import com.davek.tictactoe.models.User
import com.davek.tictactoe.utils.FireStoreUtils.currentUserDocRef
import com.davek.tictactoe.utils.FireStoreUtils.currentUserId
import com.davek.tictactoe.utils.FireStoreUtils.gameRequestCollectionRef
import com.davek.tictactoe.utils.FireStoreUtils.usersCollectionRef
import com.davek.tictactoe.utils.FireStoreUtils.waitingListCollectionRef
import com.davek.tictactoe.utils.SingleLiveEvent
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class SearchOpponentViewModel : ViewModel() {

    private val mTag = this::class.java.simpleName

    private val _currentGameId = SingleLiveEvent<String>()
    val currentGameId: LiveData<String>
        get() = _currentGameId

    private val _currentAndOpponentNicknames = MutableLiveData<Pair<String, String>>()
    val currentAndOpponentNicknames: LiveData<Pair<String, String>>
        get() = _currentAndOpponentNicknames

    private val _showAddNicknameDialog = SingleLiveEvent<String?>()
    val showAddNicknameDialog: LiveData<String?>
        get() = _showAddNicknameDialog

    private val _showSendingRequestDialog = SingleLiveEvent<PlayerItem>()
    val showSendingRequestDialog: LiveData<PlayerItem>
        get() = _showSendingRequestDialog

    private val _refreshList = SingleLiveEvent<Any>()
    val refreshList: LiveData<Any>
        get() = _refreshList

    private var token: String? = null
    private var isUserInitialized = false
    private lateinit var currentUser: User

    /**
     * Exposing MutableLiveData for two-way data binding
     * Should be only set from xml or test
     */
    val opponentNickname = MutableLiveData<String>()

    init {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.i(mTag, "Error: Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            val registrationToken: String? = task.result
            if (registrationToken == null) {
                Log.i(mTag, "Error: FCM registration token is null")
                return@OnCompleteListener
            }
            if (registrationToken == "") {
                Log.i(mTag, "Error: FCM registration token is empty")
                return@OnCompleteListener
            }
            token = registrationToken
            Log.i(mTag, "FCM token: $token")
            initUser(registrationToken)
        })
    }

    fun addNewNickname(nickname: String) {
        if (nickname == "") {
            _showAddNicknameDialog.call()
        } else {
            usersCollectionRef
                .whereEqualTo("nickname", nickname)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        token?.let {
                            currentUser = User(
                                nickname,
                                0,
                                "",
                                it
                            )
                            currentUserDocRef.set(currentUser).addOnSuccessListener {
                                setupCurrentGameListener()
                                addToWaitingList(currentUser)
                                isUserInitialized = true
                                _currentAndOpponentNicknames.value = Pair(currentUser.nickname, "")
                            }
                        }
                    } else {
                        _showAddNicknameDialog.value = nickname
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(mTag, "Error: getting documents: $exception")
                }
        }
    }

    fun onSearchButtonClick() {
        if (isUserInitialized && this::currentUser.isInitialized) {
            val nicknameToSearch = opponentNickname.value
            if (nicknameToSearch == null) {
                _currentAndOpponentNicknames.value = Pair(currentUser.nickname, "")
            } else {
                _currentAndOpponentNicknames.value = Pair(currentUser.nickname, nicknameToSearch)
            }
        }
    }

    fun onOpponentClick(opponentPlayer: PlayerItem) {
        waitingListCollectionRef
            .whereEqualTo("nickname", opponentPlayer.player.nickname)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(mTag, "Warning: this opponent is not in waiting list anymore")
                    _refreshList.call()
                } else {
                    val request = mapOf(
                        "senderNickname" to currentUser.nickname,
                        "senderUserId" to currentUserId,
                        "recipientNickname" to opponentPlayer.player.nickname,
                        "recipientToken" to opponentPlayer.player.registrationToken
                    )

                    gameRequestCollectionRef
                        .document(currentUserId)
                        .set(request, SetOptions.merge())
                        .addOnCompleteListener {
                            _showSendingRequestDialog.value = opponentPlayer
                        }
                }
            }
    }

    fun addToWaitingListIfInitialised() {
        if (isUserInitialized && this::currentUser.isInitialized) {
            addToWaitingList(currentUser)
        }
    }

    private fun initUser(registrationToken: String) {
        currentUserDocRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                _showAddNicknameDialog.call()
            } else {
                currentUser = snapshot.toObject(User::class.java) ?: User()
                currentUser.registrationToken = registrationToken
                val user = mapOf("registrationToken" to registrationToken)
                currentUserDocRef.update(user).addOnSuccessListener {
                    setupCurrentGameListener()
                    addToWaitingList(currentUser)
                    isUserInitialized = true
                    _currentAndOpponentNicknames.value = Pair(currentUser.nickname, "")
                }
            }
        }
    }

    private fun setupCurrentGameListener() {
        currentUserDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(mTag, "setupCurrentGameListener listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.i(mTag, "Current game data: ${snapshot.data}")
                val data: MutableMap<String, Any>? = snapshot.data
                if (data != null) {
                    val currentGameId = data["currentGame"]
                    if (currentGameId != null && currentGameId is String && currentGameId != "") {
                        removeFromWaitingList()
                        _currentGameId.value = currentGameId!!
                    }
                }
            } else {
                Log.d(mTag, "Error: Current game data is null")
            }
        }
    }

    private fun addToWaitingList(user: User) {
        val waitingListDocRef = waitingListCollectionRef.document(currentUserId)
        waitingListDocRef.get()
            .addOnSuccessListener { document ->
                if (document.data == null) {
                    waitingListDocRef.set(user)
                    Log.i(mTag, "Added to waiting list")
                }
            }
    }

    fun removeFromWaitingList() {
        waitingListCollectionRef.document(currentUserId).delete()
    }

    override fun onCleared() {
        super.onCleared()
        removeFromWaitingList()
        Log.i(mTag, "onCleared() called")
    }
}