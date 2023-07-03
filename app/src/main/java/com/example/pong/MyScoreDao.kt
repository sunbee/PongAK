package com.example.pong

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MyScoreDao {
    @Query("SELECT * FROM MyScore")
    fun getAll(): List<MyScore>

    @Query("SELECT MAX(score) FROM MyScore")
    fun getMaxScore(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: MyScore)

    // Add other necessary DAO methods
}
