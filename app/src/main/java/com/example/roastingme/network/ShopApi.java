package com.example.roastingme.network;

import com.example.roastingme.network.dto.CleaningPreferenceRequestDto;
import com.example.roastingme.network.dto.ProductResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ShopApi {
    @POST("api/v1/products/recommend")
    Call<List<ProductResponse>> getCleaningRecommendations(@Body CleaningPreferenceRequestDto request);
}