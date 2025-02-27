package com.ngmj.towechat.common.enums;

public enum MessageType {
    TEXT("text"),
    IMAGE("image"),
    VOICE("voice"),
    VIDEO("video"),
    SHORTVIDEO("shortvideo"),
    LOCATION("location"),
    LINK("link"),
    EVENT("event"),
    UNKNOWN("unknown");

    private String type;

    MessageType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
