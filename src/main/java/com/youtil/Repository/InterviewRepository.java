package com.youtil.Repository;

import com.youtil.Model.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<InterviewQuestion, Long> {

}
