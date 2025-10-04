package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.domain.member.dto.MemberProfileResponse;
import com.johnbeo.johnbeo.domain.member.service.MemberProfileService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileViewController {

    private final MemberProfileService memberProfileService;

    @GetMapping
    public String profile(@AuthenticationPrincipal MemberPrincipal principal, Model model) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/profile";
        }
        MemberProfileResponse profile = memberProfileService.getProfile(principal.getId());
        model.addAttribute("pageTitle", "내 프로필");
        model.addAttribute("profile", profile);
        return "member/profile";
    }
}
