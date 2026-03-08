package com.wiki.app.doc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;

public interface WikiDocumentRepository extends JpaRepository<WikiDocument, Long> {
    List<WikiDocument> findByDeletedAtIsNull();

    List<WikiDocument> findByIdInAndDeletedAtIsNull(List<Long> ids);

    List<WikiDocument> findByKbIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long kbId);

    List<WikiDocument> findTop10ByKbIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long kbId);

    List<WikiDocument> findTop10ByKbIdAndDeletedAtIsNullOrderByViewCountDesc(Long kbId);

    List<WikiDocument> findByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Long ownerId);

    List<WikiDocument> findByDeletedAtBefore(LocalDateTime time);

    @Query("""
            select d from WikiDocument d
            where d.deletedAt is null
              and d.kbId = :kbId
              and (lower(d.title) like lower(concat('%', :keyword, '%'))
              or d.markdownContent like concat('%', :keyword, '%'))
            order by d.updatedAt desc
            """)
    List<WikiDocument> search(@Param("kbId") Long kbId, @Param("keyword") String keyword);
}
