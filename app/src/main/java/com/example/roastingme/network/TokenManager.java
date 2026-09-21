package com.example.roastingme.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class TokenManager {

    private static final String TAG = "TokenManager";
    private static final String PREF_NAME = "roastingme_secure_pref";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private static volatile TokenManager instance;
    private final SharedPreferences prefs;
    private volatile String cachedAccessToken;

    private TokenManager(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences securePrefs = null;

        try {
            // MasterKey 생성 (AES256_GCM 방식)
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // EncryptedSharedPreferences 생성
            securePrefs = EncryptedSharedPreferences.create(
                    appContext,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "EncryptedSharedPreferences 초기화 실패. 일반 SharedPreferences로 대처합니다.", e);
            securePrefs = appContext.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
        }

        this.prefs = securePrefs;
        this.cachedAccessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public static TokenManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (TokenManager.class) {
                if (instance == null) {
                    instance = new TokenManager(context);
                }
            }
        }
        return instance;
    }

    @Nullable
    public String getAccessToken() {
        if (cachedAccessToken == null) {
            cachedAccessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        }
        return cachedAccessToken;
    }

    public void saveAccessToken(@Nullable String token) {
        if (token == null || token.trim().isEmpty()) {
            clear();
            return;
        }

        String cleanToken = token.trim();
        this.cachedAccessToken = cleanToken;
        prefs.edit().putString(KEY_ACCESS_TOKEN, cleanToken).apply();
    }

    public void clear() {
        this.cachedAccessToken = null;
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply();
    }
}