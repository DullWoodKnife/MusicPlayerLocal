package com.purebeat.model;

public enum PlaybackMode {
    SEQUENCE(0),      // 列表顺序播放
    SHUFFLE(1),       // 随机播放
    REPEAT_ONE(2);    // 单曲循环

    private final int value;

    PlaybackMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PlaybackMode fromValue(int value) {
        for (PlaybackMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        return SEQUENCE;
    }

    public PlaybackMode next() {
        switch (this) {
            case SEQUENCE:
                return SHUFFLE;
            case SHUFFLE:
                return REPEAT_ONE;
            case REPEAT_ONE:
                return SEQUENCE;
            default:
                return SEQUENCE;
        }
    }
}
