package com.example.scrollorstudy.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*

class UserRepository() {
    private val database = FirebaseDatabase.getInstance("https://scrollorstudy-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getAiMotivation(): Flow<String> = callbackFlow {
        val uid = getCurrentUserId() ?: return@callbackFlow
        val ref = database.getReference("ai_motivation").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg = snapshot.child("message").getValue(String::class.java) ?: "Keep studying to train the AI!"
                trySend(msg)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getAiInsights(): Flow<String> = callbackFlow {
        val uid = getCurrentUserId() ?: return@callbackFlow
        val ref = database.getReference("ai_insights").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg = snapshot.child("message").getValue(String::class.java) ?: "Analyzing patterns..."
                trySend(msg)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    data class LeaderboardData(val rank: String, val score: Int)

    fun getLeaderboardStats(): Flow<LeaderboardData> = callbackFlow {
        val uid = getCurrentUserId() ?: return@callbackFlow
        val ref = database.getReference("leaderboard").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rank = snapshot.child("rank").getValue(String::class.java) ?: "Unranked"
                val score = snapshot.child("score").getValue(Int::class.java) ?: 0
                trySend(LeaderboardData(rank, score))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun syncDailyProgress(studyTime: Long, scrollTime: Long, streak: Int, userName: String) {
        val uid = getCurrentUserId() ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val data = mapOf(
            "studyTime" to studyTime,
            "scrollTime" to scrollTime,
            "date" to date,
            "streak" to streak,
            "lastUpdated" to System.currentTimeMillis(),
            "userName" to userName
        )
        
        database.getReference("user_data").child(uid).child(date).setValue(data)
            .addOnSuccessListener {
                Log.d("UserRepository", "Sync Success for $uid")
            }
            .addOnFailureListener {
                Log.e("UserRepository", "Sync Error: ${it.message}")
            }
            
        val statsRef = database.getReference("user_stats").child(uid)
        statsRef.child("streak").setValue(streak)
        statsRef.child("lastStudyDate").setValue(date)
        statsRef.child("userName").setValue(userName)
        statsRef.child("role").setValue("student")
    }
}
