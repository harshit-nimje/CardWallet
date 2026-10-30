package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM business_cards")
    fun getAllCards(): Flow<List<LocalBusinessCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<LocalBusinessCard>)

    @Query("DELETE FROM business_cards")
    suspend fun deleteAllCards()
}
