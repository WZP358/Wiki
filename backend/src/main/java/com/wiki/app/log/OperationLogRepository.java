package com.wiki.app.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    Page<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select count(l) from OperationLog l where l.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("""
            select function('date', l.createdAt) as d, count(l) as c
            from OperationLog l
            where l.createdAt >= :fromTime and l.createdAt <= :toTime
            group by function('date', l.createdAt)
            order by d asc
            """)
    List<Object[]> dailyCounts(@Param("fromTime") LocalDateTime fromTime,
                               @Param("toTime") LocalDateTime toTime);

    @Query("""
            select l from OperationLog l
            where (:userId is null or l.userId = :userId)
              and (:action is null or :action = '' or l.action = :action)
              and (:targetType is null or :targetType = '' or l.targetType = :targetType)
              and (:targetId is null or :targetId = '' or l.targetId = :targetId)
              and (:ip is null or :ip = '' or l.ip = :ip)
              and (:fromTime is null or l.createdAt >= :fromTime)
              and (:toTime is null or l.createdAt <= :toTime)
            """)
    Page<OperationLog> adminSearch(@Param("userId") Long userId,
                                  @Param("action") String action,
                                  @Param("targetType") String targetType,
                                  @Param("targetId") String targetId,
                                  @Param("ip") String ip,
                                  @Param("fromTime") LocalDateTime fromTime,
                                  @Param("toTime") LocalDateTime toTime,
                                  Pageable pageable);
}
