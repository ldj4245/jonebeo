package com.johnbeo.johnbeo.domain.vote.service;

import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.domain.comment.entity.Comment;
import com.johnbeo.johnbeo.domain.comment.repository.CommentRepository;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import com.johnbeo.johnbeo.domain.vote.dto.VoteSummaryResponse;
import com.johnbeo.johnbeo.domain.vote.entity.Vote;
import com.johnbeo.johnbeo.domain.vote.model.VoteTargetType;
import com.johnbeo.johnbeo.domain.vote.repository.VoteRepository;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public VoteSummaryResponse votePost(Long postId, int value, MemberPrincipal principal) {
        Member member = requireMember(principal);
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다: " + postId));
        return applyVote(member, post.getId(), VoteTargetType.POST, value);
    }

    @Transactional
    public VoteSummaryResponse voteComment(Long commentId, int value, MemberPrincipal principal) {
        Member member = requireMember(principal);
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + commentId));
        return applyVote(member, comment.getId(), VoteTargetType.COMMENT, value);
    }

    @Transactional(readOnly = true)
    public VoteSummaryResponse getPostVotes(Long postId, MemberPrincipal principal) {
        postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다: " + postId));
        Member member = resolveMember(principal);
        return createSummary(postId, VoteTargetType.POST, member);
    }

    @Transactional(readOnly = true)
    public VoteSummaryResponse getCommentVotes(Long commentId, MemberPrincipal principal) {
        commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + commentId));
        Member member = resolveMember(principal);
        return createSummary(commentId, VoteTargetType.COMMENT, member);
    }

    private VoteSummaryResponse applyVote(Member member, Long targetId, VoteTargetType targetType, int value) {
        validateValue(value);
        Vote vote = voteRepository.findByMemberAndTargetIdAndTargetType(member, targetId, targetType)
            .orElse(null);

        boolean deleted = false;
        if (vote == null) {
            Vote newVote = Vote.builder()
                .member(member)
                .targetId(targetId)
                .targetType(targetType)
                .value(value)
                .build();
            voteRepository.save(newVote);
        } else if (Objects.equals(vote.getValue(), value)) {
            voteRepository.delete(vote);
            deleted = true;
        } else {
            vote.updateValue(value);
        }

        Integer userVote = deleted ? null : value;
        return createSummary(targetId, targetType, member, userVote);
    }

    private VoteSummaryResponse createSummary(Long targetId, VoteTargetType targetType, Member member) {
        return createSummary(targetId, targetType, member, null);
    }

    private VoteSummaryResponse createSummary(Long targetId, VoteTargetType targetType, Member member, Integer overrideUserVote) {
        long upVotes = voteRepository.countByTargetIdAndTargetTypeAndValue(targetId, targetType, 1);
        long downVotes = voteRepository.countByTargetIdAndTargetTypeAndValue(targetId, targetType, -1);
        Integer userVote = overrideUserVote;
        if (userVote == null && member != null) {
            userVote = voteRepository.findByMemberAndTargetIdAndTargetType(member, targetId, targetType)
                .map(Vote::getValue)
                .orElse(null);
        }
        long score = upVotes - downVotes;
        return new VoteSummaryResponse(targetId, targetType, upVotes, downVotes, score, userVote);
    }

    private void validateValue(int value) {
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("투표 값은 1 또는 -1 이어야 합니다.");
        }
    }

    private Member resolveMember(MemberPrincipal principal) {
        if (principal == null) {
            return null;
        }
        return memberRepository.findById(principal.getId()).orElse(null);
    }

    private Member requireMember(MemberPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("투표하려면 로그인이 필요합니다.");
        }
        return memberRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("회원 정보를 찾을 수 없습니다: " + principal.getId()));
    }
}
