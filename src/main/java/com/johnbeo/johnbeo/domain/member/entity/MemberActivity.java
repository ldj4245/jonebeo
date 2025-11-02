package com.johnbeo.johnbeo.domain.member.entity;

import com.johnbeo.johnbeo.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "member_activities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", unique = true)
    private Member member;

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(nullable = false)
    @Builder.Default
    private Long experiencePoints = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long totalPosts = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long totalComments = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long totalUpvotes = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long totalDownvotes = 0L;

    // 경험치 추가 및 레벨업 로직
    public void addExperiencePoints(long points) {
        this.experiencePoints += points;
        updateLevel();
    }

    // 레벨 계산: 100 경험치당 1레벨 (최대 10레벨)
    private void updateLevel() {
        int newLevel = Math.min((int) (experiencePoints / 100) + 1, 10);
        this.level = newLevel;
    }

    // 통계 증가 메서드
    public void incrementTotalPosts() {
        this.totalPosts++;
    }

    public void incrementTotalComments() {
        this.totalComments++;
    }

    public void incrementTotalUpvotes() {
        this.totalUpvotes++;
    }

    public void incrementTotalDownvotes() {
        this.totalDownvotes++;
    }

    // 레벨 티어 계산
    public String getLevelTier() {
        if (level <= 3) {
            return "BRONZE";
        } else if (level <= 6) {
            return "SILVER";
        } else if (level <= 9) {
            return "GOLD";
        } else {
            return "DIAMOND";
        }
    }

    // 다음 레벨까지 필요한 경험치
    public long getExperienceToNextLevel() {
        if (level >= 10) {
            return 0L;
        }
        long nextLevelExp = level * 100L;
        return nextLevelExp - experiencePoints;
    }

    // 현재 레벨의 경험치 진행률 (0-100%)
    public int getExperienceProgress() {
        if (level >= 10) {
            return 100;
        }
        long currentLevelMinExp = (level - 1) * 100L;
        long currentLevelMaxExp = level * 100L;
        long currentLevelExp = experiencePoints - currentLevelMinExp;
        return (int) ((currentLevelExp * 100) / (currentLevelMaxExp - currentLevelMinExp));
    }
}

