package com.ngmj.towechat.common.enums;

public enum PlatformEnum {

    ALIBAILIAN("1", "阿里百练"
            ,"https://bailian.console.aliyun.com/","1:modle:阿里百练APIKey");
    private String code;
    private String platform;
    private String url;
    private String format;
    PlatformEnum(String code, String platform, String url, String format) {
        this.code = code;
        this.platform = platform;
        this.url = url;
        this.format = format;
    }
    public String getCode() {
        return code;
    }
    public String getPlatform() {
        return platform;
    }
    public String getUrl() {
        return url;
    }
    public String getFormat() {
        return format;
    }
    public static PlatformEnum getPlatformEnum(String code) {
        for (PlatformEnum platformEnum : PlatformEnum.values()) {
            if (platformEnum.getCode().equals(code)) {
                return platformEnum;
            }
        }
        return null;
    }
}
