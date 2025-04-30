package com.youtil.Repository;

import com.youtil.Model.News;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {

    List<News> findAllByOrderByCreatedAtDesc();

    boolean existsByOriginUrl(String originUrl);

}
