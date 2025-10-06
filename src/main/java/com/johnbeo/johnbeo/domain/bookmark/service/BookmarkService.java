package com.johnbeo.johnbeo.domain.bookmark.service;

import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.domain.bookmark.entity.Bookmark;
import com.johnbeo.johnbeo.domain.bookmark.repository.BookmarkRepository;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    public boolean isBookmarked(Long memberId, Long postId) {
        if (memberId == null || postId == null) {
            return false;
        }
        return bookmarkRepository.existsByMemberIdAndPostId(memberId, postId);
    }

    public long countByPost(Long postId) {
        return bookmarkRepository.countByPostId(postId);
    }

    @Transactional
    public void addBookmark(Long memberId, Long postId) {
        Member member = requireMember(memberId);
        Post post = requirePost(postId);
        if (bookmarkRepository.existsByMemberIdAndPostId(memberId, postId)) {
            return;
        }
        Bookmark bookmark = Bookmark.builder()
            .member(member)
            .post(post)
            .build();
        bookmarkRepository.save(bookmark);
    }

    @Transactional
    public void removeBookmark(Long memberId, Long postId) {
        requireMember(memberId);
        requirePost(postId);
        bookmarkRepository.deleteByMemberIdAndPostId(memberId, postId);
    }

    private Member requireMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("회원 정보를 찾을 수 없습니다: " + memberId));
    }

    private Post requirePost(Long postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다: " + postId));
    }
}
