package com.ngmj.towechat.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class AppConfig {
    @Value("${toWechat.app.appId}")
    private String appid;
    @Value("${toWechat.app.appSecret}")
    private String appSecret;
    @Value("${toWechat.app.token}")
    private String token;
    @Value("${toWechat.app.encodingAESKey}")
    private String encodingAESKey;

    @Value("${toWechat.app.defaultAPIInfo}")
    private String defaultAPIInfo;

}
