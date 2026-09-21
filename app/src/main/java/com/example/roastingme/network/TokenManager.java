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
        SharedPreferences securePrefs;

        try {
            // 1차 시도: EncryptedSharedPreferences 정상 생성
            securePrefs = createEncryptedSharedPreferences(appContext);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "EncryptedSharedPreferences 초기화 실패. 오염된 저장소 복구 시도", e);

            try {
                // 2차 시도: 깨진 암호화 파일 삭제 후 재생성 (Self-Healing)
                deleteCorruptedPrefs(appContext);
                securePrefs = createEncryptedSharedPreferences(appContext);
                Log.i(TAG, "EncryptedSharedPreferences 복구 및 재생성 성공");
            } catch (GeneralSecurityException | IOException retryException) {
                // 복구 시도마저 실패한 경우: 평문 전환을 차단하고 명시적 예외 발생
                throw new IllegalStateException(
                        "안전한 토큰 저장소를 초기화할 수 없습니다. 앱 데이터를 삭제 후 다시 시도해 주세요.",
                        retryException
                );
            }
        }

        this.prefs = securePrefs;
        this.cachedAccessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    private SharedPreferences createEncryptedSharedPreferences(Context appContext)
            throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                appContext,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    // 오염된 SharedPreferences 파일 삭제 헬퍼 메서드
    private void deleteCorruptedPrefs(Context context) {
        try {
            context.deleteSharedPreferences(PREF_NAME);
        } catch (Exception e) {
            Log.e(TAG, "오염된 SharedPreferences 파일 삭제 중 에러 발생", e);
        }
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