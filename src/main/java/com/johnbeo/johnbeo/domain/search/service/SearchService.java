package com.johnbeo.johnbeo.domain.search.service;

import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final PostRepository postRepository;
    private final EntityManager entityManager;

    /**
     * 통합 검색 (제목, 내용, 작성자)
     */
    public Page<Post> search(
        String query,
        Long boardId,
        Instant startDate,
        Instant endDate,
        Long minViews,
        Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Post> cq = cb.createQuery(Post.class);
        Root<Post> post = cq.from(Post.class);

        List<Predicate> predicates = new ArrayList<>();

        // 검색어 필터 (제목 또는 내용)
        if (query != null && !query.isBlank()) {
            String likePattern = "%" + query.toLowerCase() + "%";
            Predicate titleMatch = cb.like(cb.lower(post.get("title")), likePattern);
            Predicate contentMatch = cb.like(cb.lower(post.get("content")), likePattern);
            predicates.add(cb.or(titleMatch, contentMatch));
        }

        // 게시판 필터
        if (boardId != null) {
            predicates.add(cb.equal(post.get("board").get("id"), boardId));
        }

        // 날짜 범위 필터
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(post.get("createdAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(post.get("createdAt"), endDate));
        }

        // 최소 조회수 필터
        if (minViews != null) {
            predicates.add(cb.greaterThanOrEqualTo(post.get("viewCount"), minViews));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(post.get("createdAt")));

        // 페이징 처리
        List<Post> results = entityManager.createQuery(cq)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        // 전체 개수 조회
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Post> countRoot = countQuery.from(Post.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    /**
     * 간단한 제목/내용 검색
     */
    public Page<Post> simpleSearch(String query, Pageable pageable) {
        return search(query, null, null, null, null, pageable);
    }
}

