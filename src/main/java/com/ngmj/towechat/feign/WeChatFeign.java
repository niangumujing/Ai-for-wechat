package com.ngmj.towechat.feign;

import com.ngmj.towechat.entity.dto.AccessTokenDto;
import com.ngmj.towechat.entity.dto.UserAppInfo;
import com.ngmj.towechat.entity.dto.WeChatResponse;
import com.ngmj.towechat.entity.vo.ToUserMessage;
import feign.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import static com.ngmj.towechat.common.CommonUtils.WECHAT_URL;

@FeignClient(name = "wechat",url = WECHAT_URL)
public interface WeChatFeign {
    @PostMapping("/cgi-bin/stable_token")
    AccessTokenDto getStableAccessToken(@RequestBody UserAppInfo userAppInfo);


//    @PostMapping("/cgi-bin/message/custom/send")
//    WeChatResponse sendMessage(@RequestParam("access_token") String accessToken, @RequestBody ToUserMessage toUserMessage);
}
