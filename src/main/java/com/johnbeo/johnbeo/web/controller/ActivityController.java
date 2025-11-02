package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.common.response.PageResponse;
import com.johnbeo.johnbeo.domain.member.dto.MemberActivityResponse;
import com.johnbeo.johnbeo.domain.member.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 사용자 랭킹 페이지
     */
    @GetMapping("/ranking")
    public String ranking(
        @RequestParam(defaultValue = "level") String type,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        Pageable pageable = PageRequest.of(page, 20);
        PageResponse<MemberActivityResponse> rankings;

        switch (type) {
            case "exp":
                rankings = PageResponse.from(
                    activityService.getExperienceRanking(pageable)
                        .map(MemberActivityResponse::from)
                );
                break;
            case "posts":
                rankings = PageResponse.from(
                    activityService.getPostRanking(pageable)
                        .map(MemberActivityResponse::from)
                );
                break;
            case "upvotes":
                rankings = PageResponse.from(
                    activityService.getUpvoteRanking(pageable)
                        .map(MemberActivityResponse::from)
                );
                break;
            default:
                rankings = PageResponse.from(
                    activityService.getLevelRanking(pageable)
                        .map(MemberActivityResponse::from)
                );
        }

        model.addAttribute("pageTitle", "사용자 랭킹");
        model.addAttribute("rankings", rankings);
        model.addAttribute("rankingType", type);
        return "ranking";
    }
}

