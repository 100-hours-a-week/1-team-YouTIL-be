package com.youtil.Repository;

import com.youtil.Model.TilRecommend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TilRecommendRepository extends JpaRepository<TilRecommend, Long> {
    Optional<TilRecommend> findByTilIdAndUserId(Long tilId, Long userId);
}
