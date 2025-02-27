package com.ngmj.towechat.context;


import com.ngmj.towechat.entity.dto.WechatToken;
import com.ngmj.towechat.entity.po.UserInfo;

public class BaseContext {

    public static ThreadLocal<WechatToken> threadLocal = new ThreadLocal<>();

    public static void setUserInfo(WechatToken userInfoDto) {
        threadLocal.set(userInfoDto);
    }

    public static WechatToken getUserInfo() {
        return threadLocal.get();
    }

    public static void removeUserInfo() {
        threadLocal.remove();
    }

}