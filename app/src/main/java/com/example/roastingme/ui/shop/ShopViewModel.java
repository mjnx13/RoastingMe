package com.example.roastingme.ui.shop;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roastingme.data.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShopViewModel extends ViewModel {

    private final List<Product> allProducts = new ArrayList<>();

    private final MutableLiveData<List<Product>> filteredProducts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // 일회성 Toast 이벤트를 위한 Event Wrapper 적용
    private final MutableLiveData<Event<String>> toastMessage = new MutableLiveData<>();

    private String currentQuery = "";
    private boolean isInitialized = false; // 중복 초기화 방지 플래그

    public LiveData<List<Product>> getFilteredProducts() {
        return filteredProducts;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Event<String>> getToastMessage() {
        return toastMessage;
    }

    public String getCurrentQuery() {
        return currentQuery;
    }

    // 최초 1회만 초기화 수행 (화면 회전 시 중복 재로드 방지)
    public void init() {
        if (isInitialized) {
            return;
        }
        loadSampleProducts();
        applyFilter();
        isInitialized = true;
    }

    // 검색어 변경 및 필터링
    public void setSearchQuery(String query) {
        String newQuery = query != null ? query : "";
        if (!this.currentQuery.equals(newQuery)) {
            this.currentQuery = newQuery;
            applyFilter();
        }
    }

    // 새로고침 요청
    public void refreshProducts() {
        isLoading.setValue(true);

        // TODO: 추후 백엔드 API / ShopRepository 호출로 교체
        loadSampleProducts();
        applyFilter();

        isLoading.setValue(false);
        // Event 객체로 감싸서 전달 (회전 시 중복 실행 방지)
        toastMessage.setValue(new Event<>("예시 상품 목록을 새로고침했어요."));
    }

    // 검색어 기반 데이터 필터링
    private void applyFilter() {
        String keyword = currentQuery.trim().toLowerCase(Locale.ROOT);
        List<Product> filtered = new ArrayList<>();

        for (Product product : allProducts) {
            if (product == null) continue;

            // Null Safety 처리
            String name = product.getName() != null ? product.getName() : "";
            String mallName = product.getMallName() != null ? product.getMallName() : "";

            String searchable = (name + " " + mallName).toLowerCase(Locale.ROOT);

            if (searchable.contains(keyword)) {
                filtered.add(product);
            }
        }

        filteredProducts.setValue(filtered);
    }

    // 샘플 데이터 로드
    private void loadSampleProducts() {
        allProducts.clear();

        allProducts.add(new Product("demo-1", "다용도 수납 바스켓", "예시 쇼핑몰 A", 12900, "", ""));
        allProducts.add(new Product("demo-2", "극세사 청소 타월 5매", "예시 쇼핑몰 B", 8900, "", ""));
        allProducts.add(new Product("demo-3", "책상 정리 트레이", "예시 쇼핑몰 A", 15900, "", ""));
        allProducts.add(new Product("demo-4", "틈새 청소 브러시", "예시 쇼핑몰 B", 5900, "", ""));
        allProducts.add(new Product("demo-5", "접이식 빨래 바구니", "예시 쇼핑몰 C", 19900, "", ""));
        allProducts.add(new Product("demo-6", "옷장 수납 정리함", "예시 쇼핑몰 C", 24900, "", ""));
    }
}