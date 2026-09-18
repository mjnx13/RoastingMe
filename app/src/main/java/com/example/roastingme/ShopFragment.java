package com.example.roastingme;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShopFragment extends Fragment {

    private static final String KEY_QUERY = "shop_query";

    private final List<Product> allProducts = new ArrayList<>();

    private String currentQuery = "";

    private ProductAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView countText;
    private TextView emptyText;

    public ShopFragment() {
        super(R.layout.fragment_shop);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString(KEY_QUERY, "");
        }

        loadSampleProducts();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        SearchView searchView =
                view.findViewById(R.id.search_products);

        recyclerView =
                view.findViewById(R.id.recycler_products);

        swipeRefresh =
                view.findViewById(R.id.swipe_refresh_products);

        countText =
                view.findViewById(R.id.tv_product_count);

        emptyText =
                view.findViewById(R.id.tv_empty_products);

        // 상품 클릭 시 쇼핑몰 링크 열기
        adapter = new ProductAdapter(this::openProductPage);

        recyclerView.setLayoutManager(
                new GridLayoutManager(requireContext(), 2)
        );

        recyclerView.setAdapter(adapter);

        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("상품명 또는 쇼핑몰 검색");
        searchView.setQuery(currentQuery, false);
        searchView.clearFocus();

        filterProducts(currentQuery);

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        currentQuery = query;
                        filterProducts(query);
                        searchView.clearFocus();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        currentQuery = newText;
                        filterProducts(newText);
                        return true;
                    }
                }
        );

        swipeRefresh.setColorSchemeColors(
                Color.parseColor("#7C3AED")
        );

        swipeRefresh.setOnChildScrollUpCallback(
                (parent, child) ->
                        recyclerView.canScrollVertically(-1)
        );

        swipeRefresh.setOnRefreshListener(this::refreshProducts);

        view.findViewById(R.id.btn_refresh_products)
                .setOnClickListener(button -> refreshProducts());
    }

    private void loadSampleProducts() {
        allProducts.clear();

        // 예시 데이터.
        // 마지막 두 값은 이미지 URL, 상품 상세 URL.
        // 실제 주소를 확보하면 빈 문자열을 교체하면 됨.
        allProducts.add(new Product(
                "demo-1",
                "다용도 수납 바스켓",
                "예시 쇼핑몰 A",
                12900,
                "",
                ""
        ));

        allProducts.add(new Product(
                "demo-2",
                "극세사 청소 타월 5매",
                "예시 쇼핑몰 B",
                8900,
                "",
                ""
        ));

        allProducts.add(new Product(
                "demo-3",
                "책상 정리 트레이",
                "예시 쇼핑몰 A",
                15900,
                "",
                ""
        ));

        allProducts.add(new Product(
                "demo-4",
                "틈새 청소 브러시",
                "예시 쇼핑몰 B",
                5900,
                "",
                ""
        ));

        allProducts.add(new Product(
                "demo-5",
                "접이식 빨래 바구니",
                "예시 쇼핑몰 C",
                19900,
                "",
                ""
        ));

        allProducts.add(new Product(
                "demo-6",
                "옷장 수납 정리함",
                "예시 쇼핑몰 C",
                24900,
                "",
                ""
        ));
    }

    private void filterProducts(String query) {
        String keyword = query.trim().toLowerCase(Locale.ROOT);

        List<Product> filtered = new ArrayList<>();

        for (Product product : allProducts) {
            String searchable =
                    (product.getName() + " " + product.getMallName())
                            .toLowerCase(Locale.ROOT);

            if (searchable.contains(keyword)) {
                filtered.add(product);
            }
        }

        adapter.setProducts(filtered);

        countText.setText("예시 상품 " + filtered.size() + "개");

        emptyText.setVisibility(
                filtered.isEmpty() ? View.VISIBLE : View.GONE
        );
    }

    private void refreshProducts() {
        // 이후 이 부분을 상품 API 호출로 교체
        loadSampleProducts();
        filterProducts(currentQuery);

        // API 연결 후에는 성공·실패 응답 처리 시 종료
        swipeRefresh.setRefreshing(false);

        Toast.makeText(
                requireContext(),
                "예시 상품 목록을 새로고침했어요.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void openProductPage(Product product) {
        String url = product.getProductUrl();

        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "아직 연결된 상품 링크가 없어요.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Uri uri = Uri.parse(url.trim());
        String scheme = uri.getScheme();

        boolean isWebUrl =
                "https".equalsIgnoreCase(scheme)
                        || "http".equalsIgnoreCase(scheme);

        if (!isWebUrl
                || uri.getHost() == null
                || uri.getHost().isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "올바른 상품 웹 주소가 아니에요.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    requireContext(),
                    "링크를 열 수 있는 앱이 없어요.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(KEY_QUERY, currentQuery);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        recyclerView.setAdapter(null);

        adapter = null;
        recyclerView = null;
        swipeRefresh = null;
        countText = null;
        emptyText = null;

        super.onDestroyView();
    }
}