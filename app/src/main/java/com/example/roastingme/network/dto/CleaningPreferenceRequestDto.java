package com.example.roastingme.network.dto;

import java.util.List;

public class CleaningPreferenceRequestDto {
    private String category;
    private String location;
    private List<String> concerns;
    private String priority;

    public CleaningPreferenceRequestDto(String category, String location, List<String> concerns, String priority) {
        this.category = category;
        this.location = location;
        this.concerns = concerns;
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public List<String> getConcerns() {
        return concerns;
    }

    public String getPriority() {
        return priority;
    }
}