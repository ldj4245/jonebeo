package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.common.exception.ResourceAlreadyExistsException;
import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.domain.watchlist.config.WatchlistProperties;
import com.johnbeo.johnbeo.domain.watchlist.dto.WatchlistEntryResponse;
import com.johnbeo.johnbeo.domain.watchlist.dto.WatchlistView;
import com.johnbeo.johnbeo.domain.watchlist.service.WatchlistService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import com.johnbeo.johnbeo.web.request.WatchlistAddForm;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/watchlist")
public class WatchlistViewController {

    private final WatchlistService watchlistService;
    private final WatchlistProperties watchlistProperties;

    public WatchlistViewController(WatchlistService watchlistService, WatchlistProperties watchlistProperties) {
        this.watchlistService = watchlistService;
        this.watchlistProperties = watchlistProperties;
    }

    @GetMapping("/manage")
    public String manage(@AuthenticationPrincipal MemberPrincipal principal, Model model) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/watchlist/manage";
        }
        populateManageModel(principal, model);
        model.addAttribute("pageTitle", "관심 코인 관리");
        if (!model.containsAttribute("addForm")) {
            model.addAttribute("addForm", new WatchlistAddForm());
        }
        return "watchlist/manage";
    }

    @PostMapping("/manage/add")
    public String add(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @ModelAttribute("addForm") WatchlistAddForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/watchlist/manage";
        }
        if (bindingResult.hasErrors()) {
            populateManageModel(principal, model);
            model.addAttribute("pageTitle", "관심 코인 관리");
            return "watchlist/manage";
        }
        try {
            watchlistService.addEntry(principal.getId(), form.getCoinId(), form.getLabel());
        } catch (ResourceAlreadyExistsException ex) {
            bindingResult.rejectValue("coinId", "watchlist.duplicate", "이미 관심 코인에 추가된 항목입니다.");
        } catch (ResourceNotFoundException ex) {
            bindingResult.rejectValue("coinId", "watchlist.notFound", "유효하지 않은 코인 ID입니다.");
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("coinId", "watchlist.invalid", ex.getMessage());
        } catch (IllegalStateException ex) {
            bindingResult.reject("watchlist.limit", ex.getMessage());
        } catch (Exception ex) {
            bindingResult.reject("watchlist.add", "관심 코인 추가 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
        if (bindingResult.hasErrors()) {
            populateManageModel(principal, model);
            model.addAttribute("pageTitle", "관심 코인 관리");
            return "watchlist/manage";
        }
        redirectAttributes.addFlashAttribute("successMessage", "관심 코인에 추가되었습니다.");
        return "redirect:/watchlist/manage";
    }

    @PostMapping("/manage/{entryId}/delete")
    public String delete(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long entryId,
        RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/watchlist/manage";
        }
        try {
            watchlistService.removeEntry(principal.getId(), entryId);
            redirectAttributes.addFlashAttribute("successMessage", "관심 코인에서 제거되었습니다.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("successMessage", "이미 삭제되었거나 찾을 수 없는 항목입니다.");
        }
        return "redirect:/watchlist/manage";
    }

    private void populateManageModel(MemberPrincipal principal, Model model) {
        List<WatchlistEntryResponse> entries = watchlistService.listEntries(principal.getId());
        WatchlistView view = watchlistService.loadWatchlist(principal);
        model.addAttribute("entries", entries);
        model.addAttribute("preview", view);
        model.addAttribute("defaultCoins", watchlistProperties.getDefaults());
    }
}
