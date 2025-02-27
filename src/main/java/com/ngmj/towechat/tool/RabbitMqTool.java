package com.ngmj.towechat.tool;

import com.ngmj.towechat.common.RabbitMqCommon;
import com.ngmj.towechat.rabbitmq.DynamicQueueCreator;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Component
@AllArgsConstructor
public class RabbitMqTool {
    private final DynamicQueueCreator dynamicQueueCreator;

    private final RabbitTemplate rabbitTemplate;

    private final RedisTool redisTool;


    /**
     * 创建临时队列
     *
     * 根据用户的openId创建一个临时的消息队列此方法用于在RabbitMQ中
     * 动态生成一个以QUEUE_PREFIX为前缀，后跟用户openId的队列名称
     *
     * @param openId 用户的唯一标识符，用于确保队列的唯一性
     * @return 返回创建的临时队列的名称
     */
    public String createTempQueue(String openId) {
        return dynamicQueueCreator.createUserQueue(RabbitMqCommon.QUEUE_PREFIX+openId);
    }

    /**
     * 向RabbitMQ发送消息
     *
     * 该方法的作用是将指定的消息发送给RabbitMQ中的特定队列，同时增加Redis中该用户的消总数量
     *
     * @param openId 用户标识符，用于确定消息的接收队列和Redis中消息计数的键
     * @param message 要发送的消息内容
     */
    public void sendMessageToRabbitMq(String openId, String message) {
        // 将消息发送到RabbitMQ中对应的队列
        rabbitTemplate.convertAndSend(RabbitMqCommon.QUEUE_PREFIX+openId, message);

        // 在Redis中增加该用户的消息计数
        redisTool.increaseMessageCount(openId);
    }


    /**
     * 从RabbitMQ中获取消息
     *
     * 该方法通过RabbitMQ的队列名（由QUEUE_PREFIX和openId组成）来接收消息如果队列中没有消息，则返回null
     * 当接收到消息后，会从消息体中提取实际内容，并使用redisTool减少该用户的未读消息计数
     *
     * @param openId 用户的唯一标识符，用于区分不同的消息队列
     * @return 接收到的消息内容如果无消息，则返回null
     */
    public String getMessageFromRabbitMq(String openId) {
        // 从指定队列中接收消息
        Message receive = rabbitTemplate.receive(RabbitMqCommon.QUEUE_PREFIX+openId);
        if (receive == null) {
            // 如果没有消息，则返回null
            return null;
        }
        // 将接收到的消息体转换为字符串形式的实际内容
        String realContent = new String(receive.getBody());
        // 减少用户的未读消息计数
        redisTool.decreaseMessageCount(openId);
        // 返回接收到的消息内容
        return realContent;
    }
    /**
     * 清空指定用户的队列消息
     * 此方法用于处理在特定情况下，如用户取消订阅时，需要清空与该用户关联的消息队列
     * 通过使用RabbitTemplate的execute方法，安全地执行队列清空操作，避免手动管理channel的复杂性
     *
     * @param openId 用户标识，用于确定要清空的队列
     */
    public void purgeQueue(String openId) {
        // 使用模板方法替代手动channel管理
        this.rabbitTemplate.execute(channel -> {
            // 执行队列清空操作，队列名称由前缀和用户标识组成
            channel.queuePurge(RabbitMqCommon.QUEUE_PREFIX+openId);
            return null;
        });
    }


}
