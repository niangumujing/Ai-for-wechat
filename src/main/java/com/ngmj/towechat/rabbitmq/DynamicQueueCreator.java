package com.ngmj.towechat.rabbitmq;

import com.ngmj.towechat.common.CommonUtils;
import com.ngmj.towechat.common.RedisCommon;
import com.ngmj.towechat.tool.RedisTool;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
@AllArgsConstructor
public class DynamicQueueCreator {
    private final RabbitAdmin rabbitAdmin;



    public String createUserQueue(String userId) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-expires", CommonUtils.AI_WAIT_TIME);
        Queue queue = new Queue(userId,
            true,  // 持久化
            false, // 非独占
            false, // 不自动删除
            args);
        rabbitAdmin.declareQueue(queue);
        return queue.getName();
    }
}
