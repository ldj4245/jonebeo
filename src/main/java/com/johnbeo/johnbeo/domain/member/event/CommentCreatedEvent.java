package com.johnbeo.johnbeo.domain.member.event;

import com.johnbeo.johnbeo.domain.comment.entity.Comment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommentCreatedEvent {
    private final Comment comment;
}

