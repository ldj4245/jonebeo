package com.johnbeo.johnbeo.domain.watchlist.repository;

import com.johnbeo.johnbeo.domain.watchlist.entity.WatchlistEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, Long> {

    List<WatchlistEntry> findByMemberIdOrderByDisplayOrderAscIdAsc(Long memberId);

    boolean existsByMemberIdAndCoinIdIgnoreCase(Long memberId, String coinId);

    @Query("select coalesce(max(e.displayOrder), 0) from WatchlistEntry e where e.member.id = :memberId")
    int findMaxDisplayOrder(@Param("memberId") Long memberId);

    Optional<WatchlistEntry> findByIdAndMemberId(Long id, Long memberId);

    int countByMemberId(Long memberId);
}
