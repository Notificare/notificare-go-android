package com.actito.go.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.actito.go.storage.db.entities.ProductEntity

@Dao
interface ProductsDao {
    @Query("SELECT * FROM products WHERE id = :id")
    fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products")
    fun getAllFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE is_highlighted = 1")
    suspend fun getHighlighted(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE is_highlighted = 1")
    fun getHighlightedFlow(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: ProductEntity)

    @Query("DELETE FROM products WHERE id in (:ids)")
    suspend fun remove(ids: List<String>)
}
