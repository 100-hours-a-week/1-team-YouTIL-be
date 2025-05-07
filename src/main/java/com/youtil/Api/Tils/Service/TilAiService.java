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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilAiService {

    private final RestTemplate restTemplate;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    /**
     * 커밋 정보를 AI API로 전송하여 TIL 내용을 생성합니다.
     */
    public TilAiResponseDTO generateTilContent(CommitDetailResponseDTO.CommitDetailResponse commitDetail,
                                               Long repositoryId,
                                               String branch,
                                               String title) {
        log.info("AI API로 TIL 내용 생성 요청 - 제목: {}, 브랜치: {}, 파일 수: {}, AI 서버 URL: {}",
                title,
                branch,
                commitDetail.getFiles() != null ? commitDetail.getFiles().size() : 0,
                aiApiUrl);

        // AI API 요청 데이터 변환
        TilAiRequestDTO requestDTO = convertToAiRequest(commitDetail, repositoryId, branch);

        // 요청에서 받은 제목이 있으면 사용
        if (title != null && !title.isEmpty()) {
            requestDTO.setTitle(title);
        }

        // 요청 데이터 로깅 (제목 포함하도록 수정)
        log.info("AI 요청 데이터: 사용자={}, 레포지토리={}, 제목={}, 파일={}개",
                requestDTO.getUsername(),
                requestDTO.getRepo(),
                requestDTO.getTitle(),
                requestDTO.getFiles() != null ? requestDTO.getFiles().size() : 0);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // HTTP 요청 생성
        HttpEntity<TilAiRequestDTO> requestEntity = new HttpEntity<>(requestDTO, headers);

        String fullUrl = aiApiUrl + "/til";
        log.info("요청 전송 URL: {}", fullUrl);

        try {
            // AI API 호출
            ResponseEntity<TilAiResponseDTO> responseEntity = restTemplate.postForEntity(
                    fullUrl,
                    requestEntity,
                    TilAiResponseDTO.class);

            log.info("AI API 응답 수신 완료 - 상태 코드: {}", responseEntity.getStatusCode());

            if (responseEntity.getBody() == null) {
                log.error("AI 서버에서 빈 응답을 반환했습니다. Fallback 메커니즘 사용");
                return generateFallbackContent(commitDetail, branch);
            }

            log.info("AI 응답 내용: content 길이={}, tags={}",
                    responseEntity.getBody().getContent() != null ? responseEntity.getBody().getContent().length() : 0,
                    responseEntity.getBody().getKeywords());

            return responseEntity.getBody();

        } catch (RestClientException e) {
            log.error("AI API 호출 실패: {}", e.getMessage(), e);
            log.info("통신 오류로 인해 Fallback 메커니즘 사용");
            return generateFallbackContent(commitDetail, branch);
        } catch (Exception e) {
            log.error("AI 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            log.info("오류로 인해 Fallback 메커니즘 사용");
            return generateFallbackContent(commitDetail, branch);
        }
    }

    /**
     * CommitDetailResponse 구조를 AI API 요청 형식으로 변환합니다.
     */
    private TilAiRequestDTO convertToAiRequest(CommitDetailResponseDTO.CommitDetailResponse commitDetail,
                                               Long repositoryId,
                                               String branch) {

        List<TilAiRequestDTO.FileInfo> fileInfos = new ArrayList<>();

        for (CommitDetailResponseDTO.FileDetail file : commitDetail.getFiles()) {
            List<TilAiRequestDTO.PatchInfo> patchInfos = new ArrayList<>();

            for (CommitDetailResponseDTO.PatchDetail patch : file.getPatches()) {
                patchInfos.add(TilAiRequestDTO.PatchInfo.builder()
                        .commit_message(patch.getCommit_message())
                        .patch(patch.getPatch())
                        .build());
            }

            fileInfos.add(TilAiRequestDTO.FileInfo.builder()
                    .filepath(file.getFilepath())
                    .latest_code(file.getLatest_code())
                    .patches(patchInfos)
                    .build());
        }

        // 제목 생성 (저장소 이름 + 날짜 기반)
        String title = "TIL-" + commitDetail.getRepo() + "-" + commitDetail.getDate();

        return TilAiRequestDTO.builder()
                .username(commitDetail.getUsername())
                .date(commitDetail.getDate())
                .repo(String.valueOf(repositoryId))
                .title(title)  // 추가된 title 필드 설정
                .files(fileInfos)
                .build();
    }

    /**
     * AI 서버가 응답하지 않을 경우 사용할 대체 컨텐츠를 생성합니다.
     */
    private TilAiResponseDTO generateFallbackContent(CommitDetailResponseDTO.CommitDetailResponse commitDetail, String branch) {
        log.warn("AI 서버 연결 실패, 대체 내용 생성");

        // 파일별 패치의 커밋 메시지를 추출
        StringBuilder commitMessagesBuilder = new StringBuilder();

        for (CommitDetailResponseDTO.FileDetail file : commitDetail.getFiles()) {
            for (CommitDetailResponseDTO.PatchDetail patch : file.getPatches()) {
                commitMessagesBuilder.append("- ").append(patch.getCommit_message()).append("\n");
            }
        }

        String commitMessages = commitMessagesBuilder.toString();
        if (commitMessages.isEmpty()) {
            commitMessages = "- 커밋 메시지 정보가 없습니다.";
        }

        return TilAiResponseDTO.builder()
                .content("# 연결 실패 선택한 커밋에 대한 TIL\n\n" +
                        "## 커밋 메시지\n\n" + commitMessages + "\n\n" +
                        "## 참고사항\n\n" +
                        "AI 서버와의 연결이 원활하지 않아 기본 템플릿으로 생성되었습니다. " +
                        "필요에 따라 내용을 편집해주세요.")
                .keywords(List.of("개발", "자동생성", "커밋요약"))
                .build();
    }
}