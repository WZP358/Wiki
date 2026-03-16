package com.wiki.app.favorite;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.favorite.dto.FavoriteDocumentResponse;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/docs/{docId}")
    public ApiResponse<Void> addFavorite(@PathVariable Long docId) {
        CurrentUser user = SecurityUtils.currentUser();
        favoriteService.addFavorite(user.getId(), docId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/docs/{docId}")
    public ApiResponse<Void> removeFavorite(@PathVariable Long docId) {
        CurrentUser user = SecurityUtils.currentUser();
        favoriteService.removeFavorite(user.getId(), docId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/mine")
    public ApiResponse<List<FavoriteDocumentResponse>> myFavorites(@RequestParam(required = false) Long kbId) {
        CurrentUser user = SecurityUtils.currentUser();
        if (kbId != null) {
            return ApiResponse.ok(favoriteService.getFavoritesByKb(user.getId(), kbId));
        }
        return ApiResponse.ok(favoriteService.getAllFavorites(user.getId()));
    }

    @GetMapping("/check/{docId}")
    public ApiResponse<Boolean> checkFavorite(@PathVariable Long docId) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(favoriteService.isFavorite(user.getId(), docId));
    }
}
