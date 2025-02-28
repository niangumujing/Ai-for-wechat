package com.ngmj.towechat.controller;

import com.alibaba.dashscope.common.Message;
import com.ngmj.towechat.common.CommonUtils;
import com.ngmj.towechat.common.enums.MessageType;
import com.ngmj.towechat.common.enums.PlatformEnum;
import com.ngmj.towechat.config.AppConfig;
import com.ngmj.towechat.context.BaseContext;
import com.ngmj.towechat.entity.po.TextMessage;
import com.ngmj.towechat.entity.dto.WechatToken;
import com.ngmj.towechat.entity.po.BaseAi;
import com.ngmj.towechat.entity.po.UserInfo;
import com.ngmj.towechat.exceptions.BusinessException;
import com.ngmj.towechat.feign.WeChatFeign;
import com.ngmj.towechat.service.AiService;
import com.ngmj.towechat.service.UserinfoService;
import com.ngmj.towechat.tool.RabbitMqTool;
import com.ngmj.towechat.tool.RedisTool;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@AllArgsConstructor
@RequestMapping("wx")
public class WeChatValidate {

    private final AppConfig appConfig;

    private final Map<String, AiService> aiServiceMap;

    private final UserinfoService userinfoService;

    private final RabbitMqTool rabbitMqTool;

    private final RedisTool redisTool;
    private final Logger logger = LoggerFactory.getLogger(WeChatValidate.class);

    // 验证微信服务器有效性
    @GetMapping()
    public String validate(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {

        // 1. 将token、timestamp、nonce按字典序排序
        String[] arr = {appConfig.getToken(), timestamp, nonce};
        Arrays.sort(arr);

        // 2. SHA1加密
        String sha1 = DigestUtils.sha1Hex(String.join("", arr));
        return sha1.equals(signature) ? echostr : "";
    }

    /**
     * 处理接收到的微信文本消息，根据消息内容调用对应AI服务生成回复
     *
     * @param textMessage 接收到的微信消息对象，包含消息内容、发送方等信息
     * @return TextMessage 包装后的回复消息对象
     * @throws Exception 处理过程中可能抛出的业务异常或系统异常
     */
    @PostMapping()
    public TextMessage receptMeg(@RequestBody TextMessage textMessage) throws Exception {
        // 初始化请求日志记录和用户上下文
        logger.info("textMessage:{}", textMessage);
        WechatToken wechatToken = new WechatToken();
        wechatToken.setToUserName(textMessage.getFromUserName());
        wechatToken.setFromUserName(textMessage.getToUserName());
        BaseContext.setUserInfo(wechatToken);
        // Redis双重锁处理逻辑：检查消息处理状态
        if (!redisTool.processWithDoubleLock(textMessage.getFromUserName())) {
            // 当无法立即处理请求时，检查AI状态和消息计数
            boolean haveAILock = redisTool.haveAILock(textMessage.getFromUserName());
            boolean haveMessageCount = redisTool.haveMessageCount(textMessage.getFromUserName());
            if (haveMessageCount) {
                // 返回队列中已存在的消息
                return getReturnMessage(textMessage, rabbitMqTool.getMessageFromRabbitMq(textMessage.getFromUserName()));
            } else if (haveAILock) {
                // 返回AI生成中的中间状态
                return getReturnMessage(textMessage, "AI正在生成中,但不足400字符");
            }
        }
        // 解析消息内容中的AI指令
        String content = textMessage.getContent();
        String[] split = content.split(CommonUtils.BOT_SUFFIX);
        AiService aiService = null;
        if (split[0].equals(CommonUtils.BOT_PREFIX)) {
            // 根据指令前缀选择对应的AI服务
            aiService = getAiService(split[1]);
            if (aiService == null) {
                throw new BusinessException("请先配置AI接口");
            }
            aiService.setApiInfo(textMessage.getFromUserName(), textMessage.getContent());
        }
        // 用户信息不存在时的默认处理逻辑
        UserInfo byId = userinfoService.getById(textMessage.getFromUserName());
        if (byId == null) {
            // 使用阿里云作为默认AI平台
            aiService = aiServiceMap.get("aliyun");
            aiService.setApiInfo(textMessage.getFromUserName(), appConfig.getDefaultAPIInfo());
            byId = userinfoService.getById(textMessage.getFromUserName());
        }

        // 最终确认使用的AI服务
        aiService = getAiService(byId.getPlatform());
        if (aiService == null) {
            throw new BusinessException("您的apikey失效了");
        }
        // 调用AI接口生成最终回复内容
        String finalContent = aiService.callWithMessage(content);
        return getReturnMessage(textMessage, finalContent);
    }

    /**
     * 创建并返回一个回复消息对象
     * 该方法用于生成一个回复文本消息，根据接收到的消息和最终的回复内容
     * 主要功能包括设置消息内容、接收方用户名、发送方用户名、创建时间及消息类型
     *
     * @param textMessage  接收到的文本消息对象，用于获取回复消息的接收方和发送方信息
     * @param finalContent 回复消息的最终内容
     * @return 返回一个填充好相关信息的文本消息对象
     */
    private static TextMessage getReturnMessage(TextMessage textMessage, String finalContent) {
        // 创建一个新的文本消息对象作为回复消息
        TextMessage returnMessage = new TextMessage();

        // 设置回复消息的内容为最终的回复内容
        returnMessage.setContent(finalContent);

        // 设置回复消息的接收方用户名为原消息的发送方用户名
        returnMessage.setToUserName(textMessage.getFromUserName());

        // 设置回复消息的发送方用户名为原消息的接收方用户名
        returnMessage.setFromUserName(textMessage.getToUserName());

        // 设置回复消息的创建时间为当前系统时间戳
        returnMessage.setCreateTime(System.currentTimeMillis());

        // 设置回复消息的消息类型为文本类型
        returnMessage.setMsgType(MessageType.TEXT.getType());

        // 返回填充好信息的回复消息对象
        return returnMessage;
    }

    /**
     * 根据平台名称获取对应的AI服务实现
     *
     * @param platform 平台名称，用于识别不同的AI服务提供商
     * @return 返回对应平台的AiService实现，如果平台不支持则返回null
     */
    private AiService getAiService(String platform) {
        // 根据平台名称获取平台枚举，如果平台名称为空或不存在，则会抛出异常
        switch (Objects.requireNonNull(PlatformEnum.getPlatformEnum(platform))) {
            case ALIBAILIAN -> {
                // 当平台为阿里百灵时，从服务映射中获取阿里云的AI服务实现
                return aiServiceMap.get("aliyun");
            }
        }
        // 如果平台不在switch案例中，则返回null
        return null;
    }


}
