package com.ngmj.towechat.entity.dto;

import lombok.Data;

@Data
public class AccessTokenDto {
    private String access_token;
    private Integer expires_in;
}
