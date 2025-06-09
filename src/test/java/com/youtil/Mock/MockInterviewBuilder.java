package com.youtil.Mock;

import com.youtil.Common.Enums.Status;
import com.youtil.Model.Interview;
import com.youtil.Model.Til;

public class MockInterviewBuilder {

    public static Interview createMockInterview(Til til, String title) {
        Interview mockInterview = Interview.builder()
                .id(100L)
                .title(title)
                .til(til)
                .level(com.youtil.Common.Enums.Level.EASY)
                .status(Status.active)
                .build();
        return mockInterview;
    }

}
