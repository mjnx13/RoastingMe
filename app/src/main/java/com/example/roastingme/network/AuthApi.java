package com.example.roastingme.network;

import androidx.annotation.NonNull;
import com.example.roastingme.network.dto.LoginRequest;
import com.example.roastingme.network.dto.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    /**
     * 로그인 요청
     * Note: ApiClient의 baseUrl 끝에 반드시 '/'가 포함되어야 정상 동작합니다.
     */
    @POST("api/auth/login")
    Call<LoginResponse> login(@NonNull @Body LoginRequest request);

    /*
    // 추후 확장 예시
    @POST("api/auth/signup")
    Call<SignUpResponse> signUp(@NonNull @Body SignUpRequest request);

    @POST("api/auth/refresh")
    Call<LoginResponse> refreshToken();
    */
}