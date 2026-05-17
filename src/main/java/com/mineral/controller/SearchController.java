package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.dto.ChatSessionResponse;
import com.mineral.dto.MineralInfoResponse;
import com.mineral.dto.SearchResult;
import com.mineral.service.ChatService;
import com.mineral.service.MineralService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final MineralService mineralService;
    private final ChatService chatService;

    @GetMapping
    public ApiResponse<SearchResult> search(
            @RequestParam String keyword,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");

        List<MineralInfoResponse> minerals = mineralService.searchMinerals(keyword);
        List<ChatSessionResponse> sessions = chatService.searchSessions(userId, keyword);

        SearchResult result = new SearchResult();
        result.setMinerals(minerals);
        result.setSessions(sessions);

        return ApiResponse.success(result);
    }
}
