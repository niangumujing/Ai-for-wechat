local key = KEYS[1]
local delta = tonumber(ARGV[1])
local expire = tonumber(ARGV[2])

-- 处理delta为0的情况
if delta == 0 then
    return redis.call('get', key) or 0
end

-- 初始化或更新值
if redis.call('exists', key) == 0 then
    local initVal = delta > 0 and delta or -delta
    redis.call('set', key, initVal)
    redis.call('expire', key, expire)
    return initVal
else
    local newVal
    -- 执行增减操作
    if delta > 0 then
        newVal = redis.call('incrby', key, delta)
    else
        newVal = redis.call('decrby', key, -delta)
    end

    -- 刷新过期时间
    redis.call('expire', key, expire)
    return newVal
end
