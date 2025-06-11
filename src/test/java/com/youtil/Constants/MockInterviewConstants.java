package com.youtil.Constants;

import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class MockInterviewConstants {

    public static final Pageable DEFAULT_PAGE_REQUEST = PageRequest.of(0, 20);
    public static final int DEFAULT_INTERVIEW_COUNT = 4;
    public static final int DEFAULT_QUESTION_COUNT = 2;
    public static final LocalDate TODAY = LocalDate.now();
}
