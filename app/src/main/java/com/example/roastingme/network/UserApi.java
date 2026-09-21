package com.example.roastingme.network;

import com.example.roastingme.network.dto.MeResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface UserApi {

    @GET("api/users/me")
    Call<MeResponse> getMe();
}