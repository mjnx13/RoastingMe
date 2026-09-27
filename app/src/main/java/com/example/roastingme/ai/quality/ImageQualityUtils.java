package com.example.roastingme.ai.quality;

import android.graphics.Bitmap;
import com.example.roastingme.ai.model.ImageQualityResult;
import com.example.roastingme.ai.model.ImageQualityResult.IssueStatus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ImageQualityUtils {

    // 1. 밝기 임계값
    private static final double MIN_BRIGHTNESS_THRESHOLD = 45.0;  // 최소 밝기
    private static final double MAX_BRIGHTNESS_THRESHOLD = 210.0; // 최대 밝기

    // 2. 선명도(흔들림) 임계값
    private static final double BLUR_VARIANCE_THRESHOLD = 120.0;  // 라플라시안 분산 기준

    // 3. 공간 유효성(복잡도/엣지 밀도) 임계값
    private static final double MIN_SPACE_EDGE_DENSITY = 0.03;   // 너무 구체적인 물체/벽 클로즈업 방지 (최소 3% 이상 엣지 존재)

    /**
     * AI 입력용 512x512 비트맵을 대상으로 다중 검증(밝기/흔들림/공간 복잡도)을 종합 수행합니다.
     */
    public static ImageQualityResult evaluateQuality(Bitmap aiBitmap) {
        if (aiBitmap == null) {
            EnumSet<IssueStatus> issues = EnumSet.of(IssueStatus.PARTIAL_SPACE_ONLY);
            return new ImageQualityResult(issues, 0, 0, 0, 1.0f, 1.0f, 1.0f, "이미지 데이터를 읽을 수 없습니다.");
        }

        int width = aiBitmap.getWidth();
        int height = aiBitmap.getHeight();
        int totalPixels = width * height;

        int[] pixels = new int[totalPixels];
        aiBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // 1. 휘도(Luma) 변환 및 평준화
        float[] grayPixels = new float[totalPixels];
        double totalLuma = 0;

        for (int i = 0; i < totalPixels; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;

            // 표준 휘도 산출 공식 ($Y = 0.299R + 0.587G + 0.114B$)
            float luma = (float) (0.299 * r + 0.587 * g + 0.114 * b);
            grayPixels[i] = luma;
            totalLuma += luma;
        }

        double avgBrightness = totalLuma / totalPixels;

        // 2. 라플라시안 분산(선명도) 산출
        double blurVariance = calculateLaplacianVariance(grayPixels, width, height);

        // 3. 공간 유효성 (소벨 엣지 밀도로 공간/클로즈업 판단)
        double spaceScore = calculateEdgeDensity(grayPixels, width, height);

        // 4. 결함 및 페널티 종합 산출 (독립 평가)
        EnumSet<IssueStatus> detectedIssues = EnumSet.noneOf(IssueStatus.class);
        List<String> messages = new ArrayList<>();

        float brightnessPenalty = 0.0f;
        float blurPenalty = 0.0f;
        float spacePenalty = 0.0f;

        // [검사 A] 밝기 판단
        if (avgBrightness < MIN_BRIGHTNESS_THRESHOLD) {
            detectedIssues.add(IssueStatus.TOO_DARK);
            brightnessPenalty = (float) Math.min(0.5, (MIN_BRIGHTNESS_THRESHOLD - avgBrightness) / MIN_BRIGHTNESS_THRESHOLD);
            messages.add("조명이 어두워 정확한 분석이 어려울 수 있습니다.");
        } else if (avgBrightness > MAX_BRIGHTNESS_THRESHOLD) {
            detectedIssues.add(IssueStatus.TOO_BRIGHT);
            brightnessPenalty = (float) Math.min(0.4, (avgBrightness - MAX_BRIGHTNESS_THRESHOLD) / (255.0 - MAX_BRIGHTNESS_THRESHOLD));
            messages.add("빛번짐이 심합니다. 반사를 피해서 촬영해 주세요.");
        }

        // [검사 B] 초점/흔들림 판단
        if (blurVariance < BLUR_VARIANCE_THRESHOLD) {
            detectedIssues.add(IssueStatus.BLURRY);
            blurPenalty = (float) Math.min(0.6, (BLUR_VARIANCE_THRESHOLD - blurVariance) / BLUR_VARIANCE_THRESHOLD);
            messages.add("초점이 맞지 않거나 카메라가 흔들렸습니다.");
        }

        // [검사 C] 공간 일부 찍힘 및 피사체 과도 밀착 판단
        if (spaceScore < MIN_SPACE_EDGE_DENSITY) {
            detectedIssues.add(IssueStatus.PARTIAL_SPACE_ONLY);
            spacePenalty = (float) Math.min(0.5, (MIN_SPACE_EDGE_DENSITY - spaceScore) / MIN_SPACE_EDGE_DENSITY);
            messages.add("공간의 너무 일부만 찍혔거나 민무늬 벽/바닥이 너무 가깝습니다. 공간이 더 넓게 보이도록 촬영해 주세요.");
        }

        // 5. 최종 결과 조합
        if (detectedIssues.isEmpty()) {
            return ImageQualityResult.createPassResult(avgBrightness, blurVariance, spaceScore);
        } else {
            String combinedMessage = String.join("\n• ", messages);
            combinedMessage = "• " + combinedMessage;

            return new ImageQualityResult(
                    detectedIssues,
                    avgBrightness,
                    blurVariance,
                    spaceScore,
                    brightnessPenalty,
                    blurPenalty,
                    spacePenalty,
                    combinedMessage
            );
        }
    }

    /**
     * 3x3 라플라시안 커널 기반 선명도 분산 산출
     */
    private static double calculateLaplacianVariance(float[] gray, int width, int height) {
        double sum = 0;
        double sumSquare = 0;
        int count = 0;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int idx = y * width + x;

                float lapValue = gray[idx - width]
                        + gray[idx - 1]
                        - (4 * gray[idx])
                        + gray[idx + 1]
                        + gray[idx + width];

                sum += lapValue;
                sumSquare += lapValue * lapValue;
                count++;
            }
        }

        if (count == 0) return 0.0;

        double mean = sum / count;
        return (sumSquare / count) - (mean * mean);
    }

    /**
     * 소벨(Sobel) 경계선 검출로 유효한 공간 정보(엣지 밀도)를 비율(0.0 ~ 1.0)로 계산합니다.
     * 민무늬 클로즈업이나 카메라 가림 현상을 감지합니다.
     */
    private static double calculateEdgeDensity(float[] gray, int width, int height) {
        int edgeCount = 0;
        int count = 0;
        float edgeThreshold = 30.0f; // 경계선 최소 변화 수치

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int idx = y * width + x;

                // 수평/수직 변화량 간이 산출
                float gx = gray[idx + 1] - gray[idx - 1];
                float gy = gray[idx + width] - gray[idx - width];

                float gradient = (float) Math.sqrt(gx * gx + gy * gy);
                if (gradient > edgeThreshold) {
                    edgeCount++;
                }
                count++;
            }
        }

        return count > 0 ? (double) edgeCount / count : 0.0;
    }
}