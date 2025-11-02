package com.johnbeo.johnbeo.domain.tag.entity;

import com.johnbeo.johnbeo.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "tags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_name", columnNames = "name")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void decrementUsageCount() {
        if (this.usageCount > 0) {
            this.usageCount--;
        }
    }
}

