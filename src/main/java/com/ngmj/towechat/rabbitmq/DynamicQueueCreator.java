package com.ngmj.towechat.rabbitmq;

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


    // 创建临时队列（自动删除 + 非持久化）
    public String createTempQueue() {
        Queue queue = new Queue("", false, true, true); // 匿名队列
        return rabbitAdmin.declareQueue(queue);
    }

    // 创建带过期时间的用户专属队列（推荐方案）
    public String createUserQueue(String userId) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-expires", 3600000); // 1小时无访问自动删除（单位：毫秒）
        
        Queue queue = new Queue(userId,
            true,  // 持久化
            false, // 非独占
            false, // 不自动删除
            args);
        
        rabbitAdmin.declareQueue(queue);
        return queue.getName();
    }
}
