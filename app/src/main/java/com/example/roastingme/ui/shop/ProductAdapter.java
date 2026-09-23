package com.example.roastingme.ui.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.roastingme.R;
import com.example.roastingme.data.model.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter
        extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private final List<Product> products = new ArrayList<>();

    private final OnProductClickListener clickListener;

    private final NumberFormat priceFormat =
            NumberFormat.getNumberInstance(Locale.KOREA);

    public ProductAdapter(OnProductClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setProducts(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);

        // 현재는 소량의 예시 상품 전체 갱신
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);

        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder,
            int position
    ) {
        Product product = products.get(position);

        holder.mallName.setText(product.getMallName());
        holder.name.setText(product.getName());

        holder.price.setText(
                priceFormat.format(product.getPrice()) + "원"
        );

        // 주소가 없거나 로딩에 실패하면 기본 이미지 표시
        Glide.with(holder.image)
                .load(product.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .fallback(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.image);

        holder.itemView.setOnClickListener(view ->
                clickListener.onProductClick(product)
        );
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        final ImageView image;
        final TextView mallName;
        final TextView name;
        final TextView price;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.iv_product_image);
            mallName = itemView.findViewById(R.id.tv_product_mall);
            name = itemView.findViewById(R.id.tv_product_name);
            price = itemView.findViewById(R.id.tv_product_price);
        }
    }
}