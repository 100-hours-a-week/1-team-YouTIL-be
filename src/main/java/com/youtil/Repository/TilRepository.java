package com.youtil.Repository;

import com.youtil.Model.Til;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TilRepository extends JpaRepository<Til, Long>, TilRepositoryCustom {


}
