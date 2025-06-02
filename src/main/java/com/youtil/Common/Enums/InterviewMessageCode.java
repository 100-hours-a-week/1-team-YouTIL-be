package com.youtil.Common.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterviewMessageCode {
    FIND_INTERVIEW_SUCCESS("200", "면접 상세 정보가 성공적으로 조회가 되었습니다."),
    FIND_INTERVIEWS_SUCCESS("200", "면접 리스트가 성공적으로 조회가 되었습니다."),
    INTERVIEW_INACTIVATE_SUCCESS("200", "면접 비활성화에 성공했습니다."),
    INTERVIEW_CREATED("201", "면접질문이 성공적으로 생성되었습니다.");
    private final String code;
    private final String message;
}
