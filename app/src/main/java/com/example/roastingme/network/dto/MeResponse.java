package com.example.roastingme.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class MeResponse {

    @SerializedName("id")
    @Nullable
    private final String id; // 서버 식별자가 숫자 형태라면 Long 타입 변경 고려

    @SerializedName("email")
    @Nullable
    private final String email;

    @SerializedName("nickname")
    @Nullable
    private final String nickname;

    // 테스트 및 더미 데이터 생성을 위한 생성자
    public MeResponse(@Nullable String id, @Nullable String email, @Nullable String nickname) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
    }

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public String getNickname() {
        return nickname;
    }

    @NonNull
    @Override
    public String toString() {
        return "MeResponse{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", nickname='" + nickname + '\'' +
                '}';
    }
}