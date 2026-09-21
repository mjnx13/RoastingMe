package com.example.roastingme.network;

import android.content.Context;
import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static final HttpUrl SERVER_URL = HttpUrl.get(BASE_URL);

    private static volatile ApiClient instance;

    private final AuthApi authApi;
    private final UserApi userApi;

    private ApiClient(@NonNull Context context) {
        // 네트워크 로그 출력을 위한 Interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(
                        TokenManager.getInstance(context),
                        SERVER_URL
                ))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.authApi = retrofit.create(AuthApi.class);
        this.userApi = retrofit.create(UserApi.class);
    }

    public static ApiClient getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient(context);
                }
            }
        }
        return instance;
    }

    public AuthApi getAuthApi() {
        return authApi;
    }

    public UserApi getUserApi() {
        return userApi;
    }
}