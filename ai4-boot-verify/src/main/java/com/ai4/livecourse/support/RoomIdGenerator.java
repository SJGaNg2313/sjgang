package com.ai4.livecourse.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoomIdGenerator {
    private static final String ROOM_SEQUENCE_KEY = "ai4:live:room:seq:1";
    private static final long INITIAL_SEQUENCE = 10000L;
    private static final String ROOM_TYPE_PREFIX = "1";

    private final StringRedisTemplate stringRedisTemplate;

    public RoomIdGenerator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 生成普通直播课房间号。 */
    public String nextRoomId() {
        stringRedisTemplate.opsForValue().setIfAbsent(
                ROOM_SEQUENCE_KEY,
                String.valueOf(INITIAL_SEQUENCE)
        );

        Long sequence = stringRedisTemplate.opsForValue().increment(ROOM_SEQUENCE_KEY);

        if (sequence == null) {
            throw new IllegalStateException("生成直播课房间号失败");
        }

        return ROOM_TYPE_PREFIX + sequence;
    }
}
