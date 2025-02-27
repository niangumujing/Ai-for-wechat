package com.ngmj.towechat.entity.dto;

import lombok.Data;

@Data
public class WechatToken {
    private String toUserName;
    private String fromUserName;
}
