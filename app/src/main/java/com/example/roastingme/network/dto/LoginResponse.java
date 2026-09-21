package com.example.roastingme.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("accessToken")
    @Nullable
    private final String accessToken;

    @SerializedName("tokenType")
    @Nullable
    private final String tokenType;

    @SerializedName("expiresIn")
    private final long expiresIn;

    // 테스트 및 더미 데이터 생성을 위한 생성자
    public LoginResponse(@Nullable String accessToken, @Nullable String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * HTTP Authorization 헤더에 바로 넣을 수 있는 형태("Bearer {token}")를 반환합니다.
     */
    @NonNull
    public String getFormattedToken() {
        String type = (tokenType != null && !tokenType.trim().isEmpty()) ? tokenType : "Bearer";
        String token = (accessToken != null) ? accessToken : "";
        return type + " " + token;
    }
}