package com.youtil.Repository;

import com.youtil.Model.Til;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface TilRepository extends JpaRepository<Til, Long>, TilRepositoryCustom {
    List<Til> findRecentPublicTilsByCategory(String category, Pageable pageable);

}
