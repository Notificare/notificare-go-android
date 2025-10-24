package com.actito.go.storage.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.actito.go.storage.db.entities.CartEntryEntity
import com.actito.go.storage.db.entities.CartEntryWithProduct

@Dao
interface CartEntriesDao {
    @Query("SELECT * FROM cart_entries")
    suspend fun getAll(): List<CartEntryEntity>

    @Query("SELECT * FROM cart_entries")
    fun getAllFlow(): Flow<List<CartEntryEntity>>

    @Transaction
    @Query("SELECT * FROM cart_entries")
    suspend fun getEntriesWithProduct(): List<CartEntryWithProduct>

    @Transaction
    @Query("SELECT * FROM cart_entries")
    fun getEntriesWithProductFlow(): Flow<List<CartEntryWithProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: CartEntryEntity)

    @Query("DELETE FROM cart_entries WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM cart_entries")
    suspend fun clear()
}
