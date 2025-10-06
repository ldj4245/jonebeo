package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.domain.board.service.BoardService;
import com.johnbeo.johnbeo.domain.post.dto.CreatePostRequest;
import com.johnbeo.johnbeo.domain.post.dto.PostResponse;
import com.johnbeo.johnbeo.domain.post.dto.UpdatePostRequest;
import com.johnbeo.johnbeo.domain.post.service.PostService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostFormController {

    private final PostService postService;
    private final BoardService boardService;

    @GetMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String newPostForm(@RequestParam(name = "boardId", required = false) Long boardId, Model model) {
        model.addAttribute("pageTitle", "새 게시글 작성");
        model.addAttribute("boards", boardService.getBoards());
        if (!model.containsAttribute("postForm")) {
            model.addAttribute("postForm", new PostForm(boardId, null, null));
        }
        return "post/form";
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public String createPost(
        @Valid @ModelAttribute("postForm") PostForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal MemberPrincipal principal,
        RedirectAttributes attributes
    ) {
        if (bindingResult.hasErrors()) {
            attributes.addFlashAttribute("org.springframework.validation.BindingResult.postForm", bindingResult);
            attributes.addFlashAttribute("postForm", form);
            String redirectUrl = "redirect:/posts/new";
            if (form.boardId() != null) {
                redirectUrl += "?boardId=" + form.boardId();
            }
            return redirectUrl;
        }
        PostResponse created = postService.createPost(new CreatePostRequest(form.boardId(), form.title(), form.content()), principal);
        return "redirect:/posts/" + created.id();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editPostForm(
        @PathVariable Long id,
        @AuthenticationPrincipal MemberPrincipal principal,
        Model model
    ) {
        PostResponse post = postService.getPost(id);
        if (!post.author().id().equals(principal.getId())) {
            throw new AccessDeniedException("게시글 수정 권한이 없습니다.");
        }
        model.addAttribute("pageTitle", "게시글 수정");
        model.addAttribute("boards", boardService.getBoards());
        if (!model.containsAttribute("postForm")) {
            model.addAttribute("postForm", new PostForm(post.board().id(), post.title(), post.content()));
        }
        model.addAttribute("postId", id);
        return "post/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String updatePost(
        @PathVariable Long id,
        @Valid @ModelAttribute("postForm") PostForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal MemberPrincipal principal,
        RedirectAttributes attributes
    ) {
        if (bindingResult.hasErrors()) {
            attributes.addFlashAttribute("org.springframework.validation.BindingResult.postForm", bindingResult);
            attributes.addFlashAttribute("postForm", form);
            return "redirect:/posts/" + id + "/edit";
        }
        postService.updatePost(id, new UpdatePostRequest(form.title(), form.content()), principal);
        return "redirect:/posts/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deletePost(
        @PathVariable Long id,
        @AuthenticationPrincipal MemberPrincipal principal,
        RedirectAttributes attributes
    ) {
        PostResponse post;
        try {
            post = postService.getPost(id);
        } catch (Exception ex) {
            attributes.addFlashAttribute("postError", ex.getMessage());
            return "redirect:/";
        }

        String redirectTarget = post.board() != null && post.board().slug() != null
            ? "/boards/" + post.board().slug()
            : "/";

        try {
            postService.deletePost(id, principal);
            attributes.addFlashAttribute("postMessage", "게시글이 삭제되었습니다.");
            return "redirect:" + redirectTarget;
        } catch (AccessDeniedException ex) {
            attributes.addFlashAttribute("postError", ex.getMessage());
        }
        return "redirect:/posts/" + id;
    }

    public record PostForm(
        @NotNull Long boardId,
        @NotBlank @Size(min = 3, max = 150) String title,
        @NotBlank String content
    ) {
    }
}
