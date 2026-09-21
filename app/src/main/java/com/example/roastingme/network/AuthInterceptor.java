package com.example.roastingme.network;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class AuthInterceptor implements Interceptor {

    // 토큰이 필요 없는 요청에 사용할 헤더 키
    public static final String NO_AUTH_HEADER = "No-Auth";

    private final TokenManager tokenManager;
    private final HttpUrl serverUrl;

    public AuthInterceptor(
            @NonNull TokenManager tokenManager,
            @NonNull HttpUrl serverUrl) {
        this.tokenManager = Objects.requireNonNull(tokenManager, "tokenManager는 null일 수 없습니다.");
        this.serverUrl = Objects.requireNonNull(serverUrl, "serverUrl은 null일 수 없습니다.");
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();

        // 1. @Headers("No-Auth: true") 어노테이션이 붙은 요청은 토큰 첨부 없이 즉시 진행
        if (original.header(NO_AUTH_HEADER) != null) {
            Request cleanRequest = original.newBuilder()
                    .removeHeader(NO_AUTH_HEADER)
                    .build();
            return chain.proceed(cleanRequest);
        }

        HttpUrl url = original.url();

        // 2. 서버 도메인 및 포트 일치 여부 확인 (외부 서버로 토큰 유출 방지)
        boolean sameServer = url.scheme().equals(serverUrl.scheme())
                && url.host().equals(serverUrl.host())
                && url.port() == serverUrl.port();

        String path = url.encodedPath();

        // 3. 공개 인증 경로 예외 처리 (접두어 검사로 Trailing Slash 대응)
        boolean publicAuthRequest = original.method().equalsIgnoreCase("POST")
                && (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/signup"));

        Request.Builder builder = original.newBuilder();

        if (sameServer && !publicAuthRequest) {
            String token = tokenManager.getAccessToken();

            if (token != null && !token.trim().isEmpty()) {
                builder.header("Authorization", "Bearer " + token);
            }
        }

        return chain.proceed(builder.build());
    }
}