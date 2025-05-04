package com.youtil.Api.Tils.Service;

import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Tils.Dto.TilAiRequestDTO;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilAiService {

    private final RestTemplate restTemplate;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    /**
     * 커밋 정보를 AI API로 전송하여 TIL 내용을 생성합니다.
     * AI 서버가 배포되기 전까지는 임시 응답을 반환합니다.
     */
    public TilAiResponseDTO generateTilContent(CommitDetailResponseDTO.CommitDetailResponse commitDetail) {
        log.info("AI API로 TIL 내용 생성 요청 (임시 구현) - 레포: {}, 브랜치: {}, 커밋 수: {}",
                commitDetail.getRepo(), commitDetail.getBranch(),
                commitDetail.getCommits() != null ? commitDetail.getCommits().size() : 0);

        // 커밋 리스트가 비어있는지 확인
        String commitMessage = "TIL 작성";
        if (commitDetail.getCommits() != null && !commitDetail.getCommits().isEmpty()) {
            // 안전하게 첫 번째 커밋 메시지 가져오기
            commitMessage = commitDetail.getCommits().get(0).getMessage();
        }

        // TODO: AI 서버 배포 후 아래 주석 해제하고 실제 API 호출로 대체
        /*
        // AI API 요청 데이터 변환
        TilAiRequestDTO requestDTO = convertToAiRequest(commitDetail);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // HTTP 요청 생성
        HttpEntity<TilAiRequestDTO> requestEntity = new HttpEntity<>(requestDTO, headers);

        try {
            // AI API 호출
            TilAiResponseDTO response = restTemplate.postForObject(
                    aiApiUrl + "/users/til",
                    requestEntity,
                    TilAiResponseDTO.class);

            log.info("AI API 응답 수신 완료");
            return response;
        } catch (Exception e) {
            log.error("AI API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        */

        // 임시 응답 생성
        return TilAiResponseDTO.builder()
                .content("# " + commitMessage + "\n\n" +
                        "## 주요 변경 사항\n\n" +
                        "이 커밋에서는 주요 기능을 구현했습니다. 주요 변경 사항은 다음과 같습니다:\n\n" +
                        "1. 기능 A 구현\n" +
                        "2. 기능 B 추가\n" +
                        "3. 버그 C 수정\n\n" +
                        "## 코드 설명\n\n" +
                        "주요 클래스에서는 다음과 같은 로직을 처리합니다...")
                .tags(List.of("개발", "기능구현", "버그수정"))
                .build();
    }

    /**
     * CommitDetailResponse를 AI API 요청 형식으로 변환합니다.
     */
    private TilAiRequestDTO convertToAiRequest(CommitDetailResponseDTO.CommitDetailResponse commitDetail) {
        List<TilAiRequestDTO.CommitInfo> commits = commitDetail.getCommits().stream()
                .map(commit -> {
                    List<TilAiRequestDTO.FileChange> changes = commit.getFiles().stream()
                            .map(file -> TilAiRequestDTO.FileChange.builder()
                                    .filename(file.getFilepath())
                                    .patch(file.getPatch())
                                    .content(file.getLatestCode())
                                    .build())
                            .collect(Collectors.toList());

                    return TilAiRequestDTO.CommitInfo.builder()
                            .sha(commit.getSha())
                            .message(commit.getMessage())
                            .author(commit.getAuthorName())
                            .date(commit.getAuthorDate())
                            .changes(changes)
                            .build();
                })
                .collect(Collectors.toList());

        return TilAiRequestDTO.builder()
                .repository(commitDetail.getRepo())
                .owner(commitDetail.getRepoOwner())
                .branch(commitDetail.getBranch())
                .commits(commits)
                .build();
    }
}