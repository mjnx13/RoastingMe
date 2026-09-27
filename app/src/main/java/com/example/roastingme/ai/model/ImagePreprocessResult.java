package com.example.roastingme.ai.model;

import android.graphics.Bitmap;

public class ImagePreprocessResult {

    private final Bitmap originalBitmap;   // 화면 프리뷰용 보정 비트맵 (최대 1080px)
    private final Bitmap aiInputBitmap;    // AI 모델 연산용 표준 비트맵 (512x512)
    private final int originalWidth;
    private final int originalHeight;
    private final long processingTimeMs;   // 전처리 수행 시간(ms)

    public ImagePreprocessResult(Bitmap originalBitmap, Bitmap aiInputBitmap, int originalWidth, int originalHeight, long processingTimeMs) {
        this.originalBitmap = originalBitmap;
        this.aiInputBitmap = aiInputBitmap;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.processingTimeMs = processingTimeMs;
    }

    public Bitmap getOriginalBitmap() {
        return originalBitmap;
    }

    public Bitmap getAiInputBitmap() {
        return aiInputBitmap;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
}