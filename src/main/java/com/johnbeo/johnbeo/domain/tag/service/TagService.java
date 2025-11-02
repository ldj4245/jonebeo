package com.johnbeo.johnbeo.domain.tag.service;

import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.tag.entity.PostTag;
import com.johnbeo.johnbeo.domain.tag.entity.Tag;
import com.johnbeo.johnbeo.domain.tag.repository.PostTagRepository;
import com.johnbeo.johnbeo.domain.tag.repository.TagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;

    /**
     * 태그 이름으로 조회 또는 생성
     */
    @Transactional
    public Tag getOrCreateTag(String tagName) {
        String normalized = normalizeTagName(tagName);
        return tagRepository.findByName(normalized)
            .orElseGet(() -> {
                Tag tag = Tag.builder()
                    .name(normalized)
                    .build();
                return tagRepository.save(tag);
            });
    }

    /**
     * 게시글에 태그 추가
     */
    @Transactional
    public void addTagsToPost(Post post, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        // 중복 제거 및 정규화
        List<String> normalizedNames = tagNames.stream()
            .map(this::normalizeTagName)
            .distinct()
            .collect(Collectors.toList());

        // 기존 태그 조회 및 새 태그 생성
        List<Tag> tags = new ArrayList<>();
        for (String name : normalizedNames) {
            Tag tag = getOrCreateTag(name);
            tags.add(tag);
        }

        // PostTag 생성
        for (Tag tag : tags) {
            PostTag postTag = PostTag.builder()
                .post(post)
                .tag(tag)
                .build();
            postTagRepository.save(postTag);
            tag.incrementUsageCount();
        }

        log.debug("게시글 {}에 태그 {} 추가", post.getId(), normalizedNames);
    }

    /**
     * 게시글의 태그 업데이트
     */
    @Transactional
    public void updatePostTags(Post post, List<String> newTagNames) {
        // 기존 태그 삭제
        List<PostTag> existingPostTags = postTagRepository.findByPostId(post.getId());
        for (PostTag postTag : existingPostTags) {
            postTag.getTag().decrementUsageCount();
            postTagRepository.delete(postTag);
        }

        // 새 태그 추가
        addTagsToPost(post, newTagNames);
    }

    /**
     * 게시글의 태그 조회
     */
    public List<Tag> getPostTags(Long postId) {
        return postTagRepository.findTagsByPostId(postId);
    }

    /**
     * 인기 태그 조회
     */
    public List<Tag> getPopularTags(int limit) {
        return tagRepository.findPopularTags(PageRequest.of(0, limit));
    }

    /**
     * 태그로 게시글 검색
     */
    public List<Post> findPostsByTag(String tagName) {
        String normalized = normalizeTagName(tagName);
        Tag tag = tagRepository.findByName(normalized)
            .orElse(null);
        
        if (tag == null) {
            return List.of();
        }

        List<PostTag> postTags = postTagRepository.findByTagId(tag.getId());
        return postTags.stream()
            .map(PostTag::getPost)
            .collect(Collectors.toList());
    }

    /**
     * 태그 이름 정규화 (소문자, 공백 제거)
     */
    private String normalizeTagName(String tagName) {
        return tagName.trim().toLowerCase();
    }

    /**
     * 태그 검색 (자동완성용)
     */
    public List<Tag> searchTags(String keyword, int limit) {
        return tagRepository.searchByKeyword(keyword, PageRequest.of(0, limit));
    }
}

