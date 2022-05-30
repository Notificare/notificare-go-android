package re.notifica.go.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import re.notifica.go.network.assets.AssetsService
import re.notifica.go.storage.db.NotificareDatabase
import re.notifica.go.storage.db.mappers.toEntity
import re.notifica.go.storage.db.mappers.toModel
import timber.log.Timber

@HiltWorker
class UpdateProductsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val database: NotificareDatabase,
    private val assetsService: AssetsService,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        Timber.i("Updating the products database.")

        val databaseProducts = database.products().getAll().map { it.toModel() }
        val networkProducts = assetsService.getProducts()

        val toRemove = databaseProducts.filter { !networkProducts.contains(it) }
        database.products().remove(toRemove.map { it.id })

        networkProducts.forEach { product ->
            database.products().add(product.toEntity())
        }

        return Result.success()
    }
}
