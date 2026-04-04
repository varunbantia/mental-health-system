package com.vanaksh.manomitra.ui.selfhelp;

public class SoundModel {
    private int id;
    private String name;
    private int iconResId;
    private int rawResId;
    private boolean isPlaying;
    private int volume; // 0 to 100

    public SoundModel(int id, String name, int iconResId, int rawResId) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
        this.rawResId = rawResId;
        this.isPlaying = false;
        this.volume = 100;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getIconResId() { return iconResId; }
    public int getRawResId() { return rawResId; }
    public boolean isPlaying() { return isPlaying; }
    public void setPlaying(boolean playing) { isPlaying = playing; }
    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = volume; }
}
