package com.actito.go.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.actito.go.network.assets.AssetsService
import com.actito.go.storage.db.ActitoDatabase
import com.actito.go.storage.db.mappers.toEntity
import com.actito.go.storage.db.mappers.toModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class UpdateProductsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val database: ActitoDatabase,
    private val assetsService: AssetsService,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        Timber.i("Updating the products database.")

        val databaseProducts = database.products().getAll().map { it.toModel() }
        val networkProducts = assetsService.getProducts()

        val toRemove = databaseProducts.filter { db -> !networkProducts.any { it.id == db.id } }
        database.products().remove(toRemove.map { it.id })

        networkProducts.forEach { product ->
            database.products().add(product.toEntity())
        }

        return Result.success()
    }
}
