package com.wiki.app.favorite;

import com.wiki.app.common.BusinessException;
import com.wiki.app.doc.DocumentService;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.favorite.dto.FavoriteDocumentResponse;
import com.wiki.app.kb.KnowledgeBase;
import com.wiki.app.kb.KnowledgeBaseRepository;
import com.wiki.app.security.CurrentUser;
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
    private final DocumentService documentService;

    public FavoriteService(FavoriteDocumentRepository favoriteRepository,
                           WikiDocumentRepository documentRepository,
                           KnowledgeBaseRepository kbRepository,
                           DocumentService documentService) {
        this.favoriteRepository = favoriteRepository;
        this.documentRepository = documentRepository;
        this.kbRepository = kbRepository;
        this.documentService = documentService;
    }

    @Transactional
    public void addFavorite(CurrentUser user, Long docId) {
        Long userId = user.getUserId();
        if (favoriteRepository.existsByUserIdAndDocId(userId, docId)) {
            return;
        }

        WikiDocument doc = documentService.requireReadable(docId, user);
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

    public List<FavoriteDocumentResponse> getFavoritesByKb(CurrentUser user, Long kbId) {
        List<FavoriteDocument> favorites = favoriteRepository.findByUserIdAndKbId(user.getUserId(), kbId);
        return favorites.stream()
                .filter(favorite -> canReadFavorite(user, favorite))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<FavoriteDocumentResponse> getAllFavorites(CurrentUser user) {
        List<FavoriteDocument> favorites = favoriteRepository.findByUserId(user.getUserId());
        return favorites.stream()
                .filter(favorite -> canReadFavorite(user, favorite))
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

    private boolean canReadFavorite(CurrentUser user, FavoriteDocument favorite) {
        try {
            documentService.requireReadable(favorite.getDocId(), user);
            return true;
        } catch (BusinessException ignored) {
            return false;
        }
    }
}
