package com.example.pong

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MyScore(
    @PrimaryKey val id: Int,
    // Define other properties
    val score: Int
)
