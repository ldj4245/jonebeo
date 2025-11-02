package com.johnbeo.johnbeo.domain.member.event;

import com.johnbeo.johnbeo.domain.comment.entity.Comment;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.vote.entity.Vote;
import lombok.Getter;

@Getter
public class VoteCreatedEvent {
    private final Vote vote;
    private final Post post;
    private final Comment comment;

    public VoteCreatedEvent(Vote vote, Post post) {
        this.vote = vote;
        this.post = post;
        this.comment = null;
    }

    public VoteCreatedEvent(Vote vote, Comment comment) {
        this.vote = vote;
        this.post = null;
        this.comment = comment;
    }
}

