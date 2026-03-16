package com.wiki.app.favorite;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.favorite.dto.FavoriteDocumentResponse;
import com.wiki.app.kb.KnowledgeBase;
import com.wiki.app.kb.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {
    private final FavoriteDocumentRepository favoriteRepository;
    private final WikiDocumentRepository documentRepository;
    private final KnowledgeBaseRepository kbRepository;

    public FavoriteService(FavoriteDocumentRepository favoriteRepository,
                          WikiDocumentRepository documentRepository,
                          KnowledgeBaseRepository kbRepository) {
        this.favoriteRepository = favoriteRepository;
        this.documentRepository = documentRepository;
        this.kbRepository = kbRepository;
    }

    @Transactional
    public void addFavorite(Long userId, Long docId) {
        if (favoriteRepository.existsByUserIdAndDocId(userId, docId)) {
            return;
        }

        WikiDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));

        FavoriteDocument favorite = new FavoriteDocument();
        favorite.setUserId(userId);
        favorite.setDocId(docId);
        favorite.setKbId(doc.getKbId());
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long userId, Long docId) {
        favoriteRepository.deleteByUserIdAndDocId(userId, docId);
    }

    public List<FavoriteDocumentResponse> getFavoritesByKb(Long userId, Long kbId) {
        List<FavoriteDocument> favorites = favoriteRepository.findByUserIdAndKbId(userId, kbId);
        return favorites.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<FavoriteDocumentResponse> getAllFavorites(Long userId) {
        List<FavoriteDocument> favorites = favoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public boolean isFavorite(Long userId, Long docId) {
        return favoriteRepository.existsByUserIdAndDocId(userId, docId);
    }

    private FavoriteDocumentResponse toResponse(FavoriteDocument favorite) {
        WikiDocument doc = documentRepository.findById(favorite.getDocId()).orElse(null);
        KnowledgeBase kb = kbRepository.findById(favorite.getKbId()).orElse(null);

        FavoriteDocumentResponse response = new FavoriteDocumentResponse();
        response.setId(favorite.getId());
        response.setDocId(favorite.getDocId());
        response.setDocTitle(doc != null ? doc.getTitle() : "已删除");
        response.setKbId(favorite.getKbId());
        response.setKbName(kb != null ? kb.getName() : "已删除");
        response.setCreatedAt(favorite.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }
}
