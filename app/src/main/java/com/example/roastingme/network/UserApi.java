package com.example.roastingme.network;

import com.example.roastingme.network.dto.MeResponse;
import com.example.roastingme.network.dto.UpdateProfileRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;

public interface UserApi {

    @GET("api/users/me")
    Call<MeResponse> getMe();

    @PATCH("api/users/me")
    Call<MeResponse> updateProfile(
            @Body UpdateProfileRequest request
    );
}