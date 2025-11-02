package com.johnbeo.johnbeo.domain.member.event;

import com.johnbeo.johnbeo.domain.post.entity.Post;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PostCreatedEvent {
    private final Post post;
}

