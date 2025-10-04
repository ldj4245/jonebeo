package com.johnbeo.johnbeo.domain.member.service;

import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.domain.comment.repository.CommentRepository;
import com.johnbeo.johnbeo.domain.member.dto.MemberProfileResponse;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public MemberProfileResponse getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("회원 정보를 찾을 수 없습니다: " + memberId));

        long postCount = postRepository.countByAuthorId(memberId);
        long commentCount = commentRepository.countByAuthorId(memberId);
        List<MemberProfileResponse.RecentPost> recentPosts = postRepository.findTop5ByAuthorIdOrderByCreatedAtDesc(memberId)
            .stream()
            .map(this::toRecentPost)
            .collect(Collectors.toList());

        return new MemberProfileResponse(
            member.getId(),
            member.getUsername(),
            member.getEmail(),
            member.getNickname(),
            member.getRole(),
            member.getCreatedAt(),
            postCount,
            commentCount,
            recentPosts
        );
    }

    private MemberProfileResponse.RecentPost toRecentPost(Post post) {
        return new MemberProfileResponse.RecentPost(
            post.getId(),
            post.getTitle(),
            post.getBoard().getName(),
            post.getBoard().getSlug(),
            post.getCreatedAt(),
            post.getViewCount()
        );
    }
}
