package com.youtil.Repository;

import com.youtil.Api.Interview.dto.InterviewResponseDTO.InterviewsItem;
import com.youtil.Model.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface InterviewRepositoryCustom {

    List<InterviewsItem> findAllUserInterviewByDate(User user, Pageable pageable, LocalDate date);

    List<LocalDate> findInterviewedDatesByUserAndYear(Long userId, int year);

}
