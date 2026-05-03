package com.wiki.app.doc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;

public interface WikiDocumentRepository extends JpaRepository<WikiDocument, Long> {
    List<WikiDocument> findByDeletedAtIsNull();

    List<WikiDocument> findByIdInAndDeletedAtIsNull(List<Long> ids);

    List<WikiDocument> findByKbIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long kbId);

    List<WikiDocument> findByKbIdAndTitleStartingWithAndDeletedAtIsNull(Long kbId, String titlePrefix);

    List<WikiDocument> findTop10ByKbIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long kbId);

    List<WikiDocument> findTop10ByKbIdAndDeletedAtIsNullOrderByViewCountDesc(Long kbId);

    List<WikiDocument> findByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Long ownerId);

    List<WikiDocument> findByDeletedAtBefore(LocalDateTime time);

    long countByDeletedAtIsNull();
    long countByDeletedAtIsNotNull();

    @Query("""
            select d from WikiDocument d
            where (:keyword is null or :keyword = ''
                or lower(d.title) like lower(concat('%', :keyword, '%'))
                or d.markdownContent like concat('%', :keyword, '%')
            )
            and (:kbId is null or d.kbId = :kbId)
            and (:ownerId is null or d.ownerId = :ownerId)
            and (:published is null or d.published = :published)
            and (:visibility is null or d.visibility = :visibility)
            and (:deleted is null
                 or (:deleted = true and d.deletedAt is not null)
                 or (:deleted = false and d.deletedAt is null)
            )
            """)
    Page<WikiDocument> adminSearch(@Param("keyword") String keyword,
                                  @Param("kbId") Long kbId,
                                  @Param("ownerId") Long ownerId,
                                  @Param("published") Boolean published,
                                  @Param("visibility") DocVisibility visibility,
                                  @Param("deleted") Boolean deleted,
                                  Pageable pageable);

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
