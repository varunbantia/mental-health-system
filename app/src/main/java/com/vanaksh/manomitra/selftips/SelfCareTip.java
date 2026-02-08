package com.vanaksh.manomitra.selftips;

public class SelfCareTip {
    private String text, backgroundColor, category, tipId;
    private int imageRes; // For local display

    public SelfCareTip() {} // Firebase needs this

    public SelfCareTip(String text, String backgroundColor, String category, String tipId, int imageRes) {
        this.text = text;
        this.backgroundColor = backgroundColor;
        this.category = category;
        this.tipId = tipId;
        this.imageRes = imageRes;
    }

    // Getters
    public String getText() { return text; }
    public String getBackgroundColor() { return backgroundColor; }
    public String getCategory() { return category; }
    public String getTipId() { return tipId; }
    public int getImageRes() { return imageRes; }
}