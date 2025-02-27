package com.ngmj.towechat.entity.dto;

import lombok.Data;

@Data
public class UserAppInfo {
    private String grant_type;
    private String appid;
    private String secret;
}
