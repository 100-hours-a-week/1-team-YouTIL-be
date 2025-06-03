package com.youtil.Repository;

import com.youtil.Model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long>,
        InterviewRepositoryCustom {

}
