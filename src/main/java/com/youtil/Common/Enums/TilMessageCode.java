package com.youtil.Common.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TilMessageCode {
    // TIL 성공 메시지
    TIL_CREATED("201", "TIL이 성공적으로 생성되었습니다."),
    TIL_UPDATED("200", "TIL이 성공적으로 수정되었습니다."),
    TIL_DELETED("200", "TIL이 성공적으로 삭제되었습니다."),
    TIL_LIST_FETCHED("200", "내 TIL 목록 조회 성공"),
    TIL_DETAIL_FETCHED("200", "내 TIL 상세 조회 성공"),
    TIL_AI_GENERATED("200", "AI가 TIL 내용을 성공적으로 생성했습니다."),

    // TIL 오류 메시지
    TIL_NOT_FOUND("404", "TIL을 찾을 수 없습니다."),
    TIL_ALREADY_DELETED("410", "이미 삭제된 TIL입니다."),
    TIL_ACCESS_DENIED("403", "본인의 TIL만 접근할 수 있습니다."),
    TIL_EDIT_DENIED("403", "TIL 수정 권한이 없습니다."),
    TIL_DELETE_DENIED("403", "TIL 삭제 권한이 없습니다."),
    TIL_INVALID_REQUEST("400", "잘못된 TIL 요청입니다."),
    TIL_SERVER_ERROR("500", "서버 내부 오류입니다."),

    // GitHub 성공 메시지
    GITHUB_ORG_FETCHED("200", "깃허브 조직 목록 조회에 성공했습니다."),
    GITHUB_ORG_REPOS_FETCHED("200", "조직 레포지토리 목록 조회 성공"),
    GITHUB_USER_REPOS_FETCHED("200", "개인 레포지토리 목록 조회 성공"),
    GITHUB_ORG_BRANCHES_FETCHED("200", "조직 레포지토리 브랜치 목록 조회 성공"),
    GITHUB_USER_BRANCHES_FETCHED("200", "개인 레포지토리 브랜치 목록 조회 성공"),
    GITHUB_COMMITS_FETCHED("200", "GitHub 커밋 정보 조회 성공"),
    GITHUB_COMMIT_DETAIL_FETCHED("200", "GitHub 커밋 상세 정보 조회 성공"),

    // GitHub 오류 메시지
    GITHUB_TOKEN_INVALID("401", "GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요."),
    GITHUB_TOKEN_MISSING("401", "GitHub 토큰이 없습니다."),
    GITHUB_ORG_NOT_FOUND("404", "해당 ID의 조직을 찾을 수 없습니다."),
    GITHUB_REPO_NOT_FOUND("404", "해당 ID의 레포지토리를 찾을 수 없습니다."),
    GITHUB_API_ERROR("500", "GitHub API 호출 중 오류가 발생했습니다.");

    private final String code;
    private final String message;
}