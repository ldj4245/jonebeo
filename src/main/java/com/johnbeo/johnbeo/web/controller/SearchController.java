package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.domain.search.service.SearchService;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public String search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long boardId,
        @RequestParam(required = false) Long startDate,
        @RequestParam(required = false) Long endDate,
        @RequestParam(required = false) Long minViews,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        Instant start = startDate != null ? Instant.ofEpochMilli(startDate) : null;
        Instant end = endDate != null ? Instant.ofEpochMilli(endDate) : null;

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> results = searchService.search(q, boardId, start, end, minViews, pageable);

        model.addAttribute("pageTitle", "검색 결과");
        model.addAttribute("query", q);
        model.addAttribute("results", results);
        model.addAttribute("boardId", boardId);
        model.addAttribute("minViews", minViews);

        return "search";
    }
}

