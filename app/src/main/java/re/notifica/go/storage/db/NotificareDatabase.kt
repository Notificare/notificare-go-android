package re.notifica.go.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import re.notifica.go.storage.db.converters.DateConverter
import re.notifica.go.storage.db.dao.CartEntriesDao
import re.notifica.go.storage.db.dao.ProductsDao
import re.notifica.go.storage.db.entities.CartEntryEntity
import re.notifica.go.storage.db.entities.ProductEntity

@Database(
    entities = [
        CartEntryEntity::class,
        ProductEntity::class,
    ],
    version = 3
)
@TypeConverters(
    DateConverter::class,
)
abstract class NotificareDatabase : RoomDatabase() {
    abstract fun cartEntries(): CartEntriesDao

    abstract fun products(): ProductsDao
}
