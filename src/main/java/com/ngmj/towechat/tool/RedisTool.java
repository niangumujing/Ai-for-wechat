package com.ngmj.towechat.tool;

import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.common.Message;
import com.ngmj.towechat.common.RedisCommon;
import com.ngmj.towechat.entity.dto.AccessTokenDto;
import lombok.AllArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ngmj.towechat.common.CommonUtils.AI_WAIT_TIME;

@Component
@AllArgsConstructor
public class RedisTool {
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> decrementScript; // 注入脚本
    private final RedissonClient redissonClient;


    /**
     * 获取Access Token
     *
     * 本方法通过访问Redis中的ACCESS_TOKEN键来获取Access Token值
     * 使用Redis存储Access Token是因为它提供了快速访问能力，并且Access Token通常需要频繁获取
     *
     * @return Access Token字符串，如果Redis中没有找到，则返回null
     */
    public String getAccessToken() {
        return stringRedisTemplate.opsForValue().get(RedisCommon.ACCESS_TOKEN);
    }

    /**
     * 设置Access Token
     *
     * 该方法将Access Token及其过期时间存储到Redis中，以便在系统中共享和快速访问
     * 使用Redis操作类stringRedisTemplate的opsForValue方法来执行字符串值的设置操作
     *
     * @param accessTokenDto 包含Access Token信息的DTO对象，包括Access Token字符串和过期时间
     */
    public void setAccessToken(AccessTokenDto accessTokenDto) {
        stringRedisTemplate.opsForValue()
                .set(RedisCommon.ACCESS_TOKEN, accessTokenDto.getAccess_token(), accessTokenDto.getExpires_in(), TimeUnit.SECONDS);
    }

    /**
     * 增加用户的未读消息计数
     *
     * @param openId 用户的唯一标识符，用于识别特定的用户
     * @return 返回更新后的未读消息计数值
     */
    public Long increaseMessageCount(String openId) {
        // 调用deltaMessageCount方法，将指定用户的未读消息计数增加1
        return deltaMessageCount(openId, 1);
    }
    /**
     * 检查用户是否有未读消息数量
     *
     * 该方法通过Redis存储的消息计数键来判断指定用户是否有未读消息
     * 它首先根据用户的openId构建Redis键，然后检查该键是否存在且不为"0"
     *
     * @param openId 用户的唯一标识符，用于构建Redis键
     * @return 如果用户有未读消息，则返回true；否则返回false
     */
    public boolean haveMessageCount(String openId) {
        // 检查Redis中是否存在指定用户的未读消息计数，并且计数不为"0"
        if (stringRedisTemplate.opsForValue().get(RedisCommon.MESSAGE_COUNT + openId) != null && !"0".equals(stringRedisTemplate.opsForValue().get(RedisCommon.MESSAGE_COUNT + openId))){
            // 如果条件满足，表明用户有未读消息
            return true;
        }
        // 如果条件不满足，表明用户没有未读消息
        return false;
    }
    /**
     * 根据指定的增量更新用户的未读消息计数
     * 此方法使用Redis脚本执行，以确保操作的原子性它主要用于处理用户未读消息计数的增加或减少
     *
     * @param openId 用户的唯一标识符，用于识别消息计数的所属者
     * @param delta 未读消息计数的变化量，可以是正数（表示增加消息计数）或负数（表示减少消息计数）
     * @return 更新后的未读消息计数如果返回null，可能表示Redis操作失败或连接问题
     */
    private Long deltaMessageCount(String openId, long delta) {
        return stringRedisTemplate.execute(
                decrementScript,
                Collections.singletonList(RedisCommon.MESSAGE_COUNT + openId),
                String.valueOf(delta),
                String.valueOf(AI_WAIT_TIME)
        );
    }
    /**
     * Decrease the message count for a specific user
     *
     * @param openId The unique identifier of the user
     * @return The new message count after decreasing
     */
    public Long decreaseMessageCount(String openId) {
        // Call the deltaMessageCount method to decrease the message count by 1
        return deltaMessageCount(openId, -1);
    }

    /**
     * 尝试为AI处理流程获取锁
     * 这个方法旨在确保对于特定用户的AI处理是顺序执行的，防止并发问题
     * 它通过在Redis中设置一个键值对来实现锁的功能，如果键不存在，则设置成功，表示获取锁成功
     *
     * @param openId 用户标识，用于区分不同用户的锁
     * @param expireSeconds 锁的过期时间（秒），防止死锁发生
     */
    public void tryLockAI(String openId, long expireSeconds) {
       stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisCommon.USER_AI_LOCK + openId, "LOCKED", expireSeconds, TimeUnit.SECONDS);
    }
    /**
     * 解除用户AI的锁定状态
     * 此方法通过删除Redis中与用户AI锁定相关的键来实现解锁功能
     *
     * @param openId 用户标识，用于标识特定用户的AI锁定状态
     */
    public void unlockAI(String openId) {
        stringRedisTemplate.delete(RedisCommon.USER_AI_LOCK + openId);
    }
    /**
     * 检查用户是否拥有AI锁
     *
     * @param openId 用户标识
     * @return 如果用户拥有AI锁，则返回true；否则返回false
     */
    public boolean haveAILock(String openId) {
        // 判断Redis中是否存在指定用户的AI锁记录
        return !(stringRedisTemplate.opsForValue().get(RedisCommon.USER_AI_LOCK + openId) == null);
    }
    /**
     * 解锁双重锁
     * 该方法用于解锁在Redis中为特定用户设置的双重锁，包括用户AI锁和消息计数锁
     * 它通过删除对应的Redis键来实现解锁
     * 使用pipeline来提高执行效率，减少网络往返次数
     *
     * @param openId 用户标识，用于标识特定用户的锁
     */
    public void unlockDoubleLock(String openId) {
        // 执行Redis操作，使用pipeline提高效率
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            // 打开pipeline，开始批量操作
            connection.openPipeline();
            try {
                // 删除用户AI锁对应的Redis键
                connection.del((RedisCommon.USER_AI_LOCK + openId).getBytes());
                // 删除消息计数锁对应的Redis键
                connection.del((RedisCommon.MESSAGE_COUNT + openId).getBytes());
                // 关闭pipeline，结束批量操作
                connection.closePipeline();
            } catch (Exception e) {
                // 如果发生异常，确保关闭pipeline
                connection.closePipeline();
                // 抛出异常，以便上层处理
                throw e;
            }
            // 返回null，表示操作完成
            return null;
        });
    }

    /**
     * 使用Redisson的双锁机制处理特定业务逻辑
     * 此方法旨在确保在处理过程中同时满足两个条件，通过加锁机制避免并发问题
     *
     * @param openId 用户标识，用于锁的唯一标识
     * @return boolean 处理结果，如果两个条件都不满足则返回true，否则返回false
     */
    public boolean processWithDoubleLock(String openId) {
        // 获取第一个条件锁
        RLock lock1 = redissonClient.getLock(RedisCommon.USER_LOCK_COND1+openId);
        // 获取第二个条件锁
        RLock lock2 = redissonClient.getLock(RedisCommon.USER_LOCK_COND2+ openId);

        // 创建多锁对象，用于同时锁定两个条件
        RLock multiLock = redissonClient.getMultiLock(lock1, lock2);

        try {
            // 尝试获取锁（等待5秒，锁自动释放时间30秒）
            boolean acquired = multiLock.tryLock(0, AI_WAIT_TIME, TimeUnit.SECONDS);
            if (acquired) {
                // 执行需要双条件达成的业务逻辑
                return !haveAILock(openId) && !haveMessageCount(openId);
            }
            return false;
        } catch (InterruptedException e) {
            // 中断当前线程，表明发生了中断异常
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // 释放锁，无论是否成功获取锁
            multiLock.unlock();
        }
    }


    /**
     * 根据用户的唯一标识符获取消息上下文
     *
     * @param openId 用户的唯一标识符，用于区分不同的用户
     * @return 返回用户的消息上下文列表，包含多个Message对象
     */
    public List<Message> getMessageContext(String openId){
        // 从Redis中获取指定用户的消息上下文数据
        String s = stringRedisTemplate.opsForValue().get(RedisCommon.MESSAGE_CONTEXT + openId);
        // 将获取的JSON数组字符串转换为List<Message>对象
        return JSONUtil.toList(JSONUtil.parseArray(s), Message.class);
    }
    /**
     * 将消息上下文信息存储到Redis中
     *
     * @param openId 用户唯一的标识符，用于在Redis中区分不同用户的消息上下文
     * @param messageList 消息列表，包含用户的消息上下文信息
     */
    public void setMessageContext(String openId, List<Message> messageList){
        // 将消息列表转换为JSON字符串，并以特定的键存储到Redis中
        stringRedisTemplate.opsForValue().set(RedisCommon.MESSAGE_CONTEXT + openId, JSONUtil.toJsonStr(messageList), AI_WAIT_TIME, TimeUnit.SECONDS);
    }
}
