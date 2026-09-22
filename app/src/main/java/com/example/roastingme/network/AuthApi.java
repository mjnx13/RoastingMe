package com.example.roastingme.network;

import androidx.annotation.NonNull;
import com.example.roastingme.network.dto.LoginRequest;
import com.example.roastingme.network.dto.LoginResponse;
import com.example.roastingme.network.dto.SignupRequest;
import com.example.roastingme.network.dto.SignupResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthApi {

    /**
     * 로그인 요청
     * Note: ApiClient의 baseUrl 끝에 반드시 '/'가 포함되어야 합니다.
     */
    @Headers("No-Auth: true")
    @POST("api/auth/login")
    Call<LoginResponse> login(@NonNull @Body LoginRequest request);

    /**
     * 회원가입 요청
     */
    @Headers("No-Auth: true")
    @POST("api/auth/signup")
    Call<SignupResponse> signup(@NonNull @Body SignupRequest request);
}