package com.ngmj.towechat.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ngmj.towechat.common.CommonUtils;
import com.ngmj.towechat.context.BaseContext;
import com.ngmj.towechat.entity.dto.WechatToken;
import com.ngmj.towechat.entity.po.AliyunBaiLian;
import com.ngmj.towechat.entity.po.BaseAi;
import com.ngmj.towechat.entity.po.UserInfo;
import com.ngmj.towechat.exceptions.BusinessException;
import com.ngmj.towechat.mapper.UserinfoMapper;
import com.ngmj.towechat.service.AiService;
import com.ngmj.towechat.service.UserinfoService;
import com.ngmj.towechat.tool.RabbitMqTool;
import com.ngmj.towechat.tool.RedisTool;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service("aliyun")
public class AliyunServiceImpl implements AiService {
    @Autowired
    private UserinfoMapper userinfoMapper;
    private final Logger logger = LoggerFactory.getLogger(AliyunServiceImpl.class);
    @Autowired
    private Generation gen;
    @Autowired
    private RabbitMqTool rabbitMqTool;

    @Autowired
    private RedisTool redisTool;

    @Autowired
    private UserinfoService userinfoService;


    // 阿里百练 获取api信息  格式: BOT_PREFIX BOT_SUFFIX 1 BOT_SUFFIX modle BOT_SUFFIX 阿里百练APIKey
    @Override
    public BaseAi setApiInfo(String openId, String apiInfo) {
        String[] split = apiInfo.split(CommonUtils.BOT_SUFFIX);
        if (split.length < 4) {
            return null;
        }
        UserInfo userInfo1 = userinfoMapper.selectById(openId);
        if (userInfo1 == null) {
            UserInfo userInfo = new UserInfo();
            userInfo.setOpenId(openId);
            userInfo.setApiInfo(apiInfo);
            userInfo.setPlatform(split[1]);
            userinfoMapper.insert(userInfo);
        } else {
            userinfoMapper.update(userInfo1, Wrappers.<UserInfo>lambdaUpdate()
                    .set(UserInfo::getApiInfo, apiInfo)
                    .set(UserInfo::getPlatform, split[1]));
        }
        return AliyunBaiLian.builder().model(split[2]).accessKeyId(split[3]).build();
    }


    private GenerationParam buildGenerationParam(AliyunBaiLian ai, List<Message> messages) {
        return GenerationParam.builder()
                .apiKey(ai.getAccessKeyId())
                .model(ai.getModel())
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();


    private static class RequestContext {
        int reasonLastSentIndex = 0;
        int lastSentIndex = 0;
        int reasoningLength = 0;
        boolean isFirstPrint = true;
        String reasoning = "";
        String content = "";
    }

    /**
     * 使用用户消息调用AI模型，并异步处理返回结果
     * 该方法首先获取用户信息和AI模型参数，然后通过RabbitMQ和Redis进行消息队列和会话上下文的管理
     * 最后，使用异步处理方式生成AI回复，并将结果发送回用户
     *
     * @param userMessage 用户输入的消息
     * @return 告知用户请求正在处理中的系统消息
     */
    @Override
    public String callWithMessage(String userMessage) {
        // 获取用户信息
        WechatToken userInfo = BaseContext.getUserInfo();
        String openId = userInfo.getToUserName();
        UserInfo byId = userinfoService.getById(openId);

        // 获取AI模型信息
        BaseAi ai = getApiInfo(byId);

        // 从Redis获取消息上下文
        List<Message> messageContext = redisTool.getMessageContext(openId);
        // 将用户消息添加到上下文中
        messageContext.add(Message.builder().role("user").content(userMessage).build());

        // 创建请求上下文对象
        RequestContext context = new RequestContext(); // 每个请求创建新上下文

        // 创建临时队列
        rabbitMqTool.createTempQueue(openId);

        try {
            // 启动异步处理任务
            CompletableFuture.runAsync(() -> {
                try {
                    // 尝试获取Redis锁，防止并发处理
                    redisTool.tryLockAI(openId, CommonUtils.AI_WAIT_TIME);

                    // 构建生成参数
                    GenerationParam param = buildGenerationParam((AliyunBaiLian) ai, messageContext);

                    // 调用AI模型生成回复
                    Flowable<GenerationResult> flowable = gen.streamCall(param);
                    // 发送思考中的消息到RabbitMQ
                    rabbitMqTool.sendMessageToRabbitMq(openId, "🤷‍♂️思考信息\n");
                    // 流式处理结果
                    flowable.blockingForEach(result -> {
                        // 更新请求上下文中的回复内容
                        context.reasoning = result.getOutput().getChoices().get(0).getMessage().getReasoningContent();
                        context.content = result.getOutput().getChoices().get(0).getMessage().getContent();

                        // 处理并发送回复内容中的reasoning部分
                        if (!context.reasoning.isEmpty() && context.reasoning.length() > context.reasoningLength) {
                            if (context.isFirstPrint) {
                                context.isFirstPrint = false;
                            }
                            int currentLength = context.reasoning.length();
                            while (currentLength - context.reasonLastSentIndex >= 400) {
                                // 计算截取窗口
                                int endIndex = context.reasonLastSentIndex + 400;
                                String segment = context.reasoning.substring(context.reasonLastSentIndex, endIndex);
                                System.out.println(segment);
                                // 发送并更新索引
                                rabbitMqTool.sendMessageToRabbitMq(openId, segment);
                                context.reasonLastSentIndex = endIndex;
                                // 打印日志便于调试
                                logger.info("已发送 {} - {} 的字符", context.reasonLastSentIndex - 400, endIndex);
                            }
                        }
                        context.reasoningLength = context.reasoning.length();

                        // 处理并发送回复内容中的content部分
                        if (!context.content.isEmpty()) {
                            if (!context.isFirstPrint) {
                                context.isFirstPrint = true;
                                if (context.reasoningLength > 400) {
                                    logger .info(context.reasoning.substring(context.reasonLastSentIndex) + "\n🤷‍♀️完整回复\n");
                                    rabbitMqTool.sendMessageToRabbitMq(openId, context.reasoning.substring(context.reasonLastSentIndex) + "\n🤷‍♀️完整回复\n");
                                } else {

                                    logger .info(context.reasoning + "\n🤷‍♀️完整回复\n");

                                    rabbitMqTool.sendMessageToRabbitMq(openId, context.reasoning + "\n🤷‍♀️完整回复\n");
                                }
                            }
                            int currentLength = context.content.length();
                            while (currentLength - context.lastSentIndex >= 400) {
                                // 计算截取窗口
                                int endIndex = context.lastSentIndex + 400;
                                String segment = context.content.substring(context.lastSentIndex, endIndex);
                                System.out.println(segment);
                                // 发送并更新索引
                                rabbitMqTool.sendMessageToRabbitMq(openId, segment);
                                context.lastSentIndex = endIndex;
                                // 打印日志便于调试
                                logger.info("已发送 {} - {} 的字符", context.lastSentIndex - 400, endIndex);
                            }
                        }
                    });

                    // 发送剩余的回复内容
                    if (context.content.length() > 400) {
                        rabbitMqTool.sendMessageToRabbitMq(openId, context.content.substring(context.lastSentIndex) + "[END]");
                    } else {
                        rabbitMqTool.sendMessageToRabbitMq(openId, context.content+ "[END]");
                    }

                    // 更新消息上下文
                    messageContext.add(Message.builder().role("assistant").content(context.content).build());
                    redisTool.setMessageContext(openId, messageContext);

                    // 解锁
                    redisTool.unlockAI(openId);
                } catch (Exception e) {
                    // 异常处理
                    logger.error("异步任务执行异常", e);
                    rabbitMqTool.sendMessageToRabbitMq(openId, "[ERROR] 处理失败：" + e.getMessage());
                    redisTool.unlockDoubleLock(openId);
                    rabbitMqTool.purgeQueue(openId);
                }
            }, executor);

            // 返回系统消息，告知用户请求正在处理中
            return "[系统] 已开始处理您的请求，请连续输入任意字符获取实时结果";
        } catch (Exception e) {
            // 解锁并清除队列
            redisTool.unlockDoubleLock(openId);
            rabbitMqTool.purgeQueue(openId);
            // 抛出业务异常
            throw new BusinessException("系统异常：" + e.getMessage());
        }
    }



    /**
     * 重写getApiInfo方法以获取API信息
     * 该方法根据用户信息中的API信息字符串，提取出阿里云的API密钥ID和模型信息，并构建一个阿里云API对象
     *
     * @param byId 用户信息对象，包含API信息字符串
     * @return 返回一个BaseAi对象，该对象是根据用户信息中的API信息构建的阿里云API对象
     */
    @Override
    public BaseAi getApiInfo(UserInfo byId) {
        // 分割用户信息中的API信息字符串，以获取各个部分的信息
        String[] split = byId.getApiInfo().split(CommonUtils.BOT_SUFFIX);
        // 使用分割得到的信息，构建并返回阿里云API对象
        // 这里假设split数组中的第3个元素是访问密钥ID，第2个元素是模型信息
        return AliyunBaiLian.builder().accessKeyId(split[3]).model(split[2]).build();
    }


}
