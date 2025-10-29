package com.actito.go.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.actito.go.R
import com.actito.go.core.formatPrice
import com.actito.go.databinding.ViewProductHighlightBinding
import com.actito.go.ktx.dp
import com.actito.go.models.Product
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

class ProductsAdapter(
    private val onProductClicked: (Product) -> Unit,
) : ListAdapter<Product, ProductsAdapter.ViewHolder>(ProductsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ViewProductHighlightBinding.inflate(
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
        private val binding: ViewProductHighlightBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            Glide.with(binding.showcaseImage)
                .load(product.imageUrl)
                .placeholder(R.drawable.shape_inbox_attachment_placeholder)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(8.dp.toInt())))
                .into(binding.showcaseImage)

            binding.nameLabel.text = product.name
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
