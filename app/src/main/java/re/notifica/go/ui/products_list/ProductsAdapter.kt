package re.notifica.go.ui.products_list

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
import re.notifica.go.databinding.ViewProductBinding
import re.notifica.go.ktx.dp
import re.notifica.go.models.Product

class ProductsAdapter(
    private val onProductClicked: (Product) -> Unit,
) : ListAdapter<Product, ProductsAdapter.ViewHolder>(ProductsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ViewProductBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ViewProductBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            Glide.with(binding.showcaseImage)
                .load(product.imageUrl)
                .placeholder(R.drawable.shape_inbox_attachment_placeholder)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(8.dp.toInt())))
                .into(binding.showcaseImage)

            binding.nameLabel.text = product.name
            binding.descriptionLabel.text = product.description
            binding.priceLabel.text = product.price.let(::formatPrice)

            binding.root.setOnClickListener {
                onProductClicked(product)
            }
        }
    }
}

private class ProductsDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem == newItem
    }
}
