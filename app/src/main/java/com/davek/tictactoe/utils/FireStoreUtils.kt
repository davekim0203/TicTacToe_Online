package com.davek.tictactoe.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

object FireStoreUtils {

    val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val authInstance: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val currentUserId = authInstance.currentUser?.uid ?: throw NullPointerException("UID is null.")

    val usersCollectionRef: CollectionReference
        get() = firestoreInstance.collection("users")

    val currentUserDocRef: DocumentReference
        get() = firestoreInstance.document(
            "users/$currentUserId"
        )

    val gamesCollectionRef: CollectionReference
        get() = firestoreInstance.collection("games")

    val waitingListCollectionRef: CollectionReference
        get() = firestoreInstance.collection("waitingList")

    val gameRequestCollectionRef: CollectionReference
        get() = firestoreInstance.collection("gameRequests")
}