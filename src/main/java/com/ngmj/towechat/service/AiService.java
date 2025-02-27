package com.ngmj.towechat.service;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.ngmj.towechat.context.BaseContext;
import com.ngmj.towechat.entity.po.BaseAi;
import com.ngmj.towechat.entity.po.UserInfo;

import java.util.List;

public interface AiService {
    BaseAi setApiInfo(String openId, String apiInfo);

    String callWithMessage(String content) throws NoApiKeyException, InputRequiredException;

    BaseAi getApiInfo(UserInfo byId);
}
