package com.vetri.smartcampus.models.common;

import java.util.ArrayList;
import java.util.List;

public class FeedbackQuestionDTO {
    private long id;
    private int order;
    private String text;
    private String type; // RATING | MCQ | TEXT
    private String optionsText;
    private Integer ratingMax;
    private boolean required;

    // Convenience for Thymeleaf rendering.
    private List<String> options = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOptionsText() {
        return optionsText;
    }

    public void setOptionsText(String optionsText) {
        this.optionsText = optionsText;
    }

    public Integer getRatingMax() {
        return ratingMax;
    }

    public void setRatingMax(Integer ratingMax) {
        this.ratingMax = ratingMax;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
