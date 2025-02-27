package com.ngmj.towechat.controller;

import com.ngmj.towechat.common.enums.MessageType;
import com.ngmj.towechat.context.BaseContext;
import com.ngmj.towechat.entity.po.TextMessage;
import com.ngmj.towechat.entity.dto.WechatToken;
import com.ngmj.towechat.exceptions.BusinessException;
import com.ngmj.towechat.tool.RabbitMqTool;
import com.ngmj.towechat.tool.RedisTool;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final RedisTool redisTool ;
    private final RabbitMqTool rabbitMqTool ;
    @ExceptionHandler(Exception.class)
    public TextMessage handleException(Exception e) {
        logger.error("服务器异常：", e);
        WechatToken userInfo = BaseContext.getUserInfo();
        TextMessage textMessage =new TextMessage();
        textMessage.setContent("服务器异常，请稍后再试");
        textMessage.setFromUserName(userInfo.getFromUserName());
        textMessage.setMsgType(MessageType.TEXT.getType());
        textMessage.setToUserName(userInfo.getToUserName());
        textMessage.setCreateTime(System.currentTimeMillis());
        redisTool.unlockDoubleLock(userInfo.getToUserName());
        rabbitMqTool.purgeQueue(userInfo.getToUserName());
        return textMessage;
    }


    @ExceptionHandler(BusinessException.class)
    public TextMessage handleBusinessException(BusinessException e) {
        logger.error("业务异常：", e);
        WechatToken userInfo = BaseContext.getUserInfo();
        TextMessage textMessage =new TextMessage();
        textMessage.setContent("业务异常："+e.getMessage());
        textMessage.setFromUserName(userInfo.getFromUserName());
        textMessage.setToUserName(userInfo.getToUserName());
        textMessage.setMsgType(MessageType.TEXT.getType());
        textMessage.setCreateTime(System.currentTimeMillis());
        redisTool.unlockDoubleLock(userInfo.getToUserName());
        rabbitMqTool.purgeQueue(userInfo.getToUserName());
        return textMessage;
    }

}
