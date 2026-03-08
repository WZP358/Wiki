package com.wiki.app.common;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SnowflakeIdGenerator {
    private static final long EPOCH = 1704067200000L;
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private final long machineId = 1L;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public synchronized long nextId() {
        long current = Instant.now().toEpochMilli();
        if (current < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards");
        }
        if (current == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                while (current <= lastTimestamp) {
                    current = Instant.now().toEpochMilli();
                }
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = current;

        long timestampPart = (current - EPOCH) << (MACHINE_ID_BITS + SEQUENCE_BITS);
        long machinePart = (machineId & MAX_MACHINE_ID) << SEQUENCE_BITS;
        return timestampPart | machinePart | sequence;
    }
}
