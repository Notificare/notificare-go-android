package re.notifica.go.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import re.notifica.go.R
import re.notifica.go.core.formatPrice
import re.notifica.go.databinding.ViewCartEntryBinding
import re.notifica.go.ktx.dp
import re.notifica.go.storage.db.entities.CartEntryWithProduct

class CartAdapter(
    // private val onEntryRemoved: (CartEntryWithProduct) -> Unit,
) : ListAdapter<CartEntryWithProduct, CartAdapter.CartItemViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        return CartItemViewHolder(
            ViewCartEntryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartItemViewHolder(
        private val binding: ViewCartEntryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: CartEntryWithProduct) {
            Glide.with(binding.showcaseImage)
                .load(entry.product.imageUrl)
                .placeholder(R.drawable.shape_inbox_attachment_placeholder)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(8.dp.toInt())))
                .into(binding.showcaseImage)

            binding.nameLabel.text = entry.product.name
            binding.priceLabel.text = entry.product.price.let(::formatPrice)

//            binding.root.setOnClickListener {
//                onProductClicked(product)
//            }
        }
    }
}

private class CartDiffCallback : DiffUtil.ItemCallback<CartEntryWithProduct>() {
    override fun areItemsTheSame(oldItem: CartEntryWithProduct, newItem: CartEntryWithProduct): Boolean {
        return oldItem.cartEntry.id == newItem.cartEntry.id
    }

    override fun areContentsTheSame(oldItem: CartEntryWithProduct, newItem: CartEntryWithProduct): Boolean {
        return oldItem == newItem
    }
}
