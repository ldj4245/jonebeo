package com.johnbeo.johnbeo.domain.board.entity;

import com.johnbeo.johnbeo.common.entity.BaseEntity;
import com.johnbeo.johnbeo.domain.board.model.BoardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "boards",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_board_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_board_slug", columnNames = "slug")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoardType type;

    @Column(nullable = false, length = 100)
    private String slug;
}
