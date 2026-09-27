package com.example.roastingme.ai.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ImageQualityResult {

    public enum IssueStatus {
        PASS,                // 정상 분석 가능
        TOO_DARK,            // 너무 어두움
        TOO_BRIGHT,          // 너무 밝음/빛번짐
        BLURRY,              // 초점 불량/흔들림
        PARTIAL_SPACE_ONLY,  // 공간의 너무 일부만 찍힘
        OBSTACLE_BLOCKED,    // 장애물로 인해 판단 불가
        NOT_CLEANING_TARGET  // 청소/정리 대상 공간이 아님
    }

    private final EnumSet<IssueStatus> issues; // 다중 결함 수집
    private final double brightnessScore;      // 밝기 (0~255 Luma)
    private final double blurScore;            // 선명도 (Laplacian Variance)
    private final double spaceScore;           // 공간 유효성 점수 (0.0~1.0)

    // 6단계 신뢰도 보정용 세부 페널티 (0.0f ~ 1.0f)
    private final float brightnessPenalty;
    private final float blurPenalty;
    private final float spacePenalty;

    private final String message;              // 사용자 종합 안내 메시지

    public ImageQualityResult(EnumSet<IssueStatus> issues,
                              double brightnessScore, double blurScore, double spaceScore,
                              float brightnessPenalty, float blurPenalty, float spacePenalty,
                              String message) {
        this.issues = issues != null ? issues : EnumSet.of(IssueStatus.PASS);
        this.brightnessScore = brightnessScore;
        this.blurScore = blurScore;
        this.spaceScore = spaceScore;
        this.brightnessPenalty = brightnessPenalty;
        this.blurPenalty = blurPenalty;
        this.spacePenalty = spacePenalty;
        this.message = message;
    }

    // 최종 통과 여부 (PASS 상태만 포함하거나 결함 목록이 없는 경우)
    public boolean isPass() {
        return issues.contains(IssueStatus.PASS) || issues.isEmpty();
    }

    // 다중 결함 목록 반환
    public EnumSet<IssueStatus> getIssues() {
        return issues;
    }

    public double getBrightnessScore() { return brightnessScore; }
    public double getBlurScore() { return blurScore; }
    public double getSpaceScore() { return spaceScore; }

    // 6단계에서 사용할 총 품질 감점 계수 (최대 1.0 제한)
    public float getTotalQualityPenalty() {
        return Math.min(1.0f, brightnessPenalty + blurPenalty + spacePenalty);
    }

    public float getBrightnessPenalty() { return brightnessPenalty; }
    public float getBlurPenalty() { return blurPenalty; }
    public float getSpacePenalty() { return spacePenalty; }

    public String getMessage() { return message; }

    // 성공 결과 생성을 위한 정적 팩토리 메서드
    public static ImageQualityResult createPassResult(double brightnessScore, double blurScore, double spaceScore) {
        return new ImageQualityResult(
                EnumSet.of(IssueStatus.PASS),
                brightnessScore, blurScore, spaceScore,
                0.0f, 0.0f, 0.0f,
                "분석에 적합한 품질의 이미지입니다."
        );
    }
}