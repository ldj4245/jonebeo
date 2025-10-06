package com.johnbeo.johnbeo.domain.notice.controller;

import com.johnbeo.johnbeo.domain.notice.dto.NoticeResponse;
import com.johnbeo.johnbeo.domain.notice.service.NoticeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getNotices(@RequestParam(defaultValue = "5") int limit) {
        if (limit <= 0) {
            limit = 5;
        }
        return ResponseEntity.ok(noticeService.getActiveNotices(limit));
    }
}
