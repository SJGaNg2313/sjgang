package com.ai4.common.id;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 单实例内线程安全的雪花ID生成器。 */
@Component
public class SnowflakeIdGenerator {

    private static final long CUSTOM_EPOCH = 1704067200000L;
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long lastTimestamp = -1L;
    private long sequence;

    public SnowflakeIdGenerator(@Value("${ai4.id.worker-id:1}") long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("Snowflake worker ID must be between 0 and 1023");
        }
        this.workerId = workerId;
    }

    /** 生成可存入 varchar(64) 的数字字符串ID。 */
    public synchronized String nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("System clock moved backwards");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        long id = ((timestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
        return Long.toString(id);
    }

    private long waitUntilNextMillis(long previousTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= previousTimestamp) {
            Thread.onSpinWait();
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
