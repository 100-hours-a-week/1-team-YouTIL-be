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

    // 커뮤니티 관련 성공 메시지
    COMMUNITY_RECENT_TILS_FETCHED("200", "최신 TIL 목록 조회 성공"),

    // AI 서버 관련 메시지
    TILS_AI_SERVER_HEALTH("200", "AI 서버는 정상입니다"),

    // 날짜 포맷 관련 메시지
    TIL_DATE_FORMAT_INVALID("400", "날짜 형식이 올바르지 않습니다. 'yyyy-MM-dd' 형식을 사용해주세요."),

    // 요청 검증 관련 메시지
    TIL_REPOSITORY_ID_REQUIRED("400", "레포지토리 ID가 필요합니다."),
    TIL_BRANCH_REQUIRED("400", "브랜치명이 필요합니다."),
    TIL_COMMITS_REQUIRED("400", "최소 하나 이상의 커밋 정보가 필요합니다."),
    TIL_TITLE_REQUIRED("400", "TIL 제목이 필요합니다."),
    TIL_CATEGORY_REQUIRED("400", "TIL 카테고리가 필요합니다."),
    TIL_SHARED_STATUS_REQUIRED("400", "커뮤니티 업로드 여부가 필요합니다."),
    TIL_FILES_NOT_FOUND("400", "조회된 파일 정보가 없습니다."),

    // TIL 오류 메시지
    TIL_NOT_FOUND("404", "TIL을 찾을 수 없습니다."),
    TIL_ALREADY_DELETED("410", "이미 삭제된 TIL입니다."),
    TIL_ACCESS_DENIED("403", "본인의 TIL만 접근할 수 있습니다."),
    TIL_EDIT_DENIED("403", "TIL 수정 권한이 없습니다."),
    TIL_DELETE_DENIED("403", "TIL 삭제 권한이 없습니다."),
    TIL_INVALID_REQUEST("400", "잘못된 TIL 요청입니다."),
    TIL_SERVER_ERROR("500", "서버 내부 오류입니다."),
    TIL_CREATION_ERROR("500", "TIL 생성 중 오류가 발생했습니다."),

    // AI 서버 오류 메시지
    TIL_AI_EMPTY_RESPONSE("503", "AI 서버에서 유효한 응답을 받지 못했습니다."),
    TIL_AI_CONNECTION_ERROR("503", "AI 서버와의 연결이 원활하지 않습니다."),
    TIL_AI_PROCESSING_ERROR("503", "AI 서비스 처리 중 오류가 발생했습니다."),

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
    GITHUB_API_ERROR("500", "GitHub API 호출 중 오류가 발생했습니다."),
    GITHUB_USER_REPOS_NOT_FOUND("404", "사용자의 레포지토리 정보를 찾을 수 없습니다."),
    GITHUB_ORG_REPOS_NOT_FOUND("404", "조직의 레포지토리 정보를 찾을 수 없습니다."),
    GITHUB_USER_ORGS_NOT_FOUND("404", "사용자의 조직 정보를 찾을 수 없습니다."),
    GITHUB_API_PERMISSION_DENIED("403", "GitHub API 호출 횟수 제한에 도달했거나 접근 권한이 없습니다."),
    GITHUB_RESOURCE_NOT_FOUND("404", "요청한 GitHub 리소스를 찾을 수 없습니다."),
    GITHUB_INVALID_REQUEST("422", "GitHub API 요청이 유효하지 않습니다."),
    GITHUB_SERVER_ERROR("500", "GitHub 서버 오류가 발생했습니다."),
    GITHUB_TOKEN_DECRYPT_ERROR("500", "GitHub 토큰 복호화에 실패했습니다."),
    GITHUB_COMMIT_NOT_FOUND("404", "해당 커밋을 찾을 수 없습니다."),
    GITHUB_INVALID_DATE_FORMAT("400", "날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식이어야 합니다.");

    private final String code;
    private final String message;
}