package re.notifica.go.storage.db.entities

import androidx.room.*
import java.util.*

@Entity(tableName = "cart_entries")
data class CartEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val time: Date,
    @ColumnInfo(name = "product_id") val productId: String,
)

data class CartEntryWithProduct(
    @Embedded
    val cartEntry: CartEntryEntity,

    @Relation(parentColumn = "product_id", entityColumn = "id")
    val product: ProductEntity,
)
