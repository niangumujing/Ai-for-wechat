package com.ngmj.towechat.Task;

import com.ngmj.towechat.common.enums.GrantTypeEnum;
import com.ngmj.towechat.config.AppConfig;
import com.ngmj.towechat.entity.dto.AccessTokenDto;
import com.ngmj.towechat.entity.dto.UserAppInfo;
import com.ngmj.towechat.feign.WeChatFeign;
import com.ngmj.towechat.tool.RedisTool;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@AllArgsConstructor
public class AccessTokenTask {
    private final AppConfig appConfig;
    private final WeChatFeign weChatFeign;
    private final RedisTool redisTool;
    private static final Logger logger = LoggerFactory.getLogger(AccessTokenTask.class);
    @Scheduled(fixedDelay = 60 * 60 * 1000 - 1000*10)
    public void getAccessToken() throws IOException {
        if (redisTool.getAccessToken() != null){
            logger.info("access_token: {}", redisTool.getAccessToken());
            return;
        }
        UserAppInfo userAppInfo = new UserAppInfo();
        userAppInfo.setAppid(appConfig.getAppid());
        userAppInfo.setSecret(appConfig.getAppSecret());
        userAppInfo.setGrant_type(GrantTypeEnum.ACCESS_TOKEN.getGrantType());
        AccessTokenDto accessToken = weChatFeign.getStableAccessToken(userAppInfo);
        if (accessToken.getAccess_token() != null) {
            logger.info("access_token: {}", accessToken.getAccess_token());
            logger.info("expires_in: {}", accessToken.getExpires_in());
            redisTool.setAccessToken(accessToken);
        } else {
            logger.error("access_token获取失败");
        }
    }
}
