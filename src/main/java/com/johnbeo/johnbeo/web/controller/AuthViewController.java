package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.auth.dto.AuthResponse;
import com.johnbeo.johnbeo.auth.dto.LoginRequest;
import com.johnbeo.johnbeo.auth.dto.RegisterRequest;
import com.johnbeo.johnbeo.auth.service.AuthService;
import com.johnbeo.johnbeo.common.exception.ResourceAlreadyExistsException;
import com.johnbeo.johnbeo.security.jwt.JwtCookieUtils;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthViewController {

    private final AuthService authService;

    @Value("${app.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @GetMapping("/login")
    public String loginForm(
        @RequestParam(value = "redirect", required = false) String redirect,
        @AuthenticationPrincipal MemberPrincipal principal,
        Model model
    ) {
        if (principal != null) {
            return "redirect:" + (StringUtils.hasText(redirect) ? redirect : "/");
        }
        model.addAttribute("pageTitle", "로그인");
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm(null, null));
        }
        model.addAttribute("redirect", redirect);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
        @Valid @ModelAttribute("loginForm") LoginForm form,
        BindingResult bindingResult,
        @RequestParam(value = "redirect", required = false) String redirect,
        HttpServletResponse response,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "로그인");
            model.addAttribute("redirect", redirect);
            return "auth/login";
        }
        try {
            AuthResponse authResponse = authService.login(new LoginRequest(form.username(), form.password()));
            Cookie accessCookie = JwtCookieUtils.createAccessTokenCookie(authResponse.accessToken(), authResponse.expiresIn(), cookieSecure);
            response.addCookie(accessCookie);
            return "redirect:" + (StringUtils.hasText(redirect) ? redirect : "/");
        } catch (AuthenticationException ex) {
            bindingResult.reject("login.failed", "아이디 또는 비밀번호가 올바르지 않습니다.");
            model.addAttribute("pageTitle", "로그인");
            model.addAttribute("redirect", redirect);
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerForm(
        @AuthenticationPrincipal MemberPrincipal principal,
        Model model
    ) {
        if (principal != null) {
            return "redirect:/";
        }
        model.addAttribute("pageTitle", "회원가입");
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm(null, null, null, null, null));
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute("registerForm") RegisterForm form,
        BindingResult bindingResult,
        HttpServletResponse response,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "회원가입");
            return "auth/register";
        }
        try {
            authService.register(new RegisterRequest(form.username(), form.password(), form.email(), form.nickname()));
            AuthResponse authResponse = authService.login(new LoginRequest(form.username(), form.password()));
            Cookie accessCookie = JwtCookieUtils.createAccessTokenCookie(authResponse.accessToken(), authResponse.expiresIn(), cookieSecure);
            response.addCookie(accessCookie);
            return "redirect:/";
        } catch (ResourceAlreadyExistsException ex) {
            bindingResult.reject("register.duplicate", ex.getMessage());
        } catch (AuthenticationException ex) {
            redirectAttributes.addFlashAttribute("loginMessage", "회원가입은 완료되었지만 자동 로그인을 진행하지 못했습니다. 다시 로그인해주세요.");
            return "redirect:/auth/login";
        }
        model.addAttribute("pageTitle", "회원가입");
        return "auth/register";
    }

    @PostMapping("/logout")
    public String logout(@RequestParam(value = "redirect", required = false) String redirect, HttpServletResponse response) {
        Cookie clearingCookie = JwtCookieUtils.createClearingCookie(cookieSecure);
        response.addCookie(clearingCookie);
        return "redirect:" + (StringUtils.hasText(redirect) ? redirect : "/");
    }

    public record LoginForm(
        @NotBlank String username,
        @NotBlank String password
    ) {
    }

    public record RegisterForm(
        @NotBlank @Size(min = 4, max = 50) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String confirmPassword,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 2, max = 30) String nickname
    ) {
        @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
        public boolean isPasswordConfirmed() {
            return password != null && password.equals(confirmPassword);
        }
    }
}
