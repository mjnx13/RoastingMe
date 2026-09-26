package com.example.roastingme.ui.shop;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.roastingme.R;
import com.example.roastingme.data.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ShopFragment extends Fragment {

    private static final String KEY_QUERY = "shop_query";

    private ShopViewModel viewModel;

    private ProductAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView countText;
    private TextView emptyText;

    public ShopFragment() {
        super(R.layout.fragment_shop);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(ShopViewModel.class);

        // 2. View 참조 가져오기
        SearchView searchView = view.findViewById(R.id.search_products);
        recyclerView = view.findViewById(R.id.recycler_products);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_products);
        countText = view.findViewById(R.id.tv_product_count);
        emptyText = view.findViewById(R.id.tv_empty_products);

        // 3. Adapter 및 RecyclerView 설정
        adapter = new ProductAdapter(this::openProductPage);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(adapter);

        // 4. ViewModel 데이터 초기화 및 저장된 검색 상태 복원
        viewModel.init();
        if (savedInstanceState != null) {
            String savedQuery = savedInstanceState.getString(KEY_QUERY, "");
            viewModel.setSearchQuery(savedQuery);
        }

        // 5. LiveData 관찰 (Observe)
        setupObservers();

        // 6. SearchView 설정
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("상품명 또는 쇼핑몰 검색");
        searchView.setQuery(viewModel.getCurrentQuery(), false);
        searchView.clearFocus();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setSearchQuery(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });

        // 7. SwipeRefreshLayout 및 버튼 이벤트
        swipeRefresh.setColorSchemeColors(Color.parseColor("#7C3AED"));
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> recyclerView.canScrollVertically(-1));
        swipeRefresh.setOnRefreshListener(() -> viewModel.refreshProducts());

        view.findViewById(R.id.btn_refresh_products)
                .setOnClickListener(button -> viewModel.refreshProducts());

        // 8. AI 맞춤 추천 버튼 클릭 이벤트 연결 (레이아웃에 btn_ai_recommend 가 있는 경우)
        View btnAiRecommend = view.findViewById(R.id.btn_ai_recommend);
        if (btnAiRecommend != null) {
            btnAiRecommend.setOnClickListener(v -> showCategorySelectionDialog());
        }
    }

    private void setupObservers() {
        // 필터링된 상품 데이터 변경 감지
        viewModel.getFilteredProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null && adapter != null) {
                adapter.setProducts(products);
                countText.setText("상품 " + products.size() + "개");
                emptyText.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        // 로딩 상태 감지 (SwipeRefresh 스피너)
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(Boolean.TRUE.equals(isLoading));
            }
        });

        // 알림 메시지 토스트 출력 (Event Wrapper 적용)
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                String message = event.getContentIfNotHandled();
                if (message != null && !message.isEmpty()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // =========================================================================
    // AI 취향 맞춤 추천 선택 다이얼로그 로직
    // =========================================================================

    // Step 1: 대분류 카테고리 선택
    private void showCategorySelectionDialog() {
        String[] categories = {"청소 도구", "정리함", "청소 약품/세제", "인테리어 용품"};

        new AlertDialog.Builder(requireContext())
                .setTitle("어떤 종류의 제품이 필요하신가요?")
                .setItems(categories, (dialog, which) -> {
                    String selectedCategory = categories[which];
                    showLocationSelectionDialog(selectedCategory);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // Step 2: 공간 선택
    private void showLocationSelectionDialog(String category) {
        String[] locations = {"욕실/화장실", "주방", "침실/옷장", "거실", "베란다/창문"};

        new AlertDialog.Builder(requireContext())
                .setTitle("[" + category + "] 필요한 공간 선택")
                .setItems(locations, (dialog, which) -> {
                    String selectedLocation = locations[which];
                    showConcernsSelectionDialog(category, selectedLocation);
                })
                .setNegativeButton("이전", (dialog, which) -> showCategorySelectionDialog())
                .show();
    }

    // Step 3: 고민사항 선택 (다중 선택 가능)
    private void showConcernsSelectionDialog(String category, String location) {
        String[] concernsOptions = {"곰팡이/물때 제거", "기름때/탄 자국", "좁은 틈새 청소", "수납 공간 부족", "탈취/좋은 향기"};
        boolean[] checkedItems = new boolean[concernsOptions.length];
        List<String> selectedConcerns = new ArrayList<>();

        new AlertDialog.Builder(requireContext())
                .setTitle("해결하고 싶은 고민을 모두 선택해주세요")
                .setMultiChoiceItems(concernsOptions, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("다음", (dialog, which) -> {
                    for (int i = 0; i < concernsOptions.length; i++) {
                        if (checkedItems[i]) {
                            selectedConcerns.add(concernsOptions[i]);
                        }
                    }
                    showPrioritySelectionDialog(category, location, selectedConcerns);
                })
                .setNegativeButton("이전", (dialog, which) -> showLocationSelectionDialog(category))
                .show();
    }

    // Step 4: 우선순위 선택 및 최종 AI 추천 API 호출
    private void showPrioritySelectionDialog(String category, String location, List<String> concerns) {
        String[] priorities = {"가성비 중심", "빠르고 쉬운 사용", "디자인/감성", "강력한 성능"};

        new AlertDialog.Builder(requireContext())
                .setTitle("가장 중요하게 생각하는 조건은?")
                .setItems(priorities, (dialog, which) -> {
                    String selectedPriority = priorities[which];

                    // ViewModel에 Context와 함께 추천 요청 전달
                    viewModel.fetchRecommendations(
                            requireContext(),
                            category,
                            location,
                            concerns,
                            selectedPriority
                    );
                })
                .setNegativeButton("이전", (dialog, which) -> showConcernsSelectionDialog(category, location))
                .show();
    }

    // =========================================================================

    private void openProductPage(Product product) {
        String url = product.getProductUrl();

        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(requireContext(), "아직 연결된 상품 링크가 없어요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(url.trim());
        String scheme = uri.getScheme();

        boolean isWebUrl = "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);

        if (!isWebUrl || uri.getHost() == null || uri.getHost().isEmpty()) {
            Toast.makeText(requireContext(), "올바른 상품 웹 주소가 아니에요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "링크를 열 수 있는 앱이 없어요.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (viewModel != null) {
            outState.putString(KEY_QUERY, viewModel.getCurrentQuery());
        }
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