package com.ngmj.towechat.common.enums;

public enum GrantTypeEnum {
    ACCESS_TOKEN("client_credential");

    private String grantType;

    GrantTypeEnum(String grantType) {
        this.grantType = grantType;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
}
