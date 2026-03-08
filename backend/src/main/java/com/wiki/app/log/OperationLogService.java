package com.wiki.app.log;

import com.wiki.app.common.SnowflakeIdGenerator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {
    private final OperationLogRepository operationLogRepository;
    private final SnowflakeIdGenerator idGenerator;

    public OperationLogService(OperationLogRepository operationLogRepository, SnowflakeIdGenerator idGenerator) {
        this.operationLogRepository = operationLogRepository;
        this.idGenerator = idGenerator;
    }

    @Async
    public void record(Long userId, String username, String action, String targetType, String targetId, String ip, String detail) {
        OperationLog log = new OperationLog();
        log.setId(idGenerator.nextId());
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setIp(ip);
        log.setDetail(detail);
        operationLogRepository.save(log);
    }
}
