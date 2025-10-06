package com.johnbeo.johnbeo.domain.watchlist.entity;

import com.johnbeo.johnbeo.common.entity.BaseEntity;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    name = "watchlist_entries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_watchlist_member_coin", columnNames = {"member_id", "coin_id"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WatchlistEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "coin_id", nullable = false, length = 80)
    private String coinId;

    @Column(name = "label", length = 120)
    private String label;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public void updateLabel(String label) {
        this.label = label;
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
