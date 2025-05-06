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
     * 수정: branch 정보를 별도 파라미터로 받도록 변경
     */
    public TilAiResponseDTO generateTilContent(CommitDetailResponseDTO.CommitDetailResponse commitDetail,
                                               Long repositoryId,
                                               String branch) {
        log.info("AI API로 TIL 내용 생성 요청 - 브랜치: {}, 파일 수: {}, AI 서버 URL: {}",
                branch,
                commitDetail.getFiles() != null ? commitDetail.getFiles().size() : 0,
                aiApiUrl);

        // AI API 요청 데이터 변환 (branch 정보 별도 전달)
        TilAiRequestDTO requestDTO = convertToAiRequest(commitDetail, repositoryId, branch);

        // 요청 데이터 로깅
        log.info("AI 요청 데이터: repository={}, branch={}, commits={}개",
                requestDTO.getRepository(),
                requestDTO.getBranch(), requestDTO.getCommits().size());

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
                    responseEntity.getBody().getTags());

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
     * 수정: branch 정보를 별도 파라미터로 받도록 변경
     */
    private TilAiRequestDTO convertToAiRequest(CommitDetailResponseDTO.CommitDetailResponse commitDetail,
                                               Long repositoryId,
                                               String branch) {
        List<TilAiRequestDTO.CommitInfo> commits = new ArrayList<>();

        // 각 파일에 대한 패치 정보를 커밋 별로 그룹화하여 변환
        for (CommitDetailResponseDTO.FileDetail file : commitDetail.getFiles()) {
            for (CommitDetailResponseDTO.PatchDetail patch : file.getPatches()) {
                // 커밋 정보 찾기 또는 생성
                TilAiRequestDTO.CommitInfo commitInfo = null;

                // 이미 같은 커밋 메시지를 가진 CommitInfo가 있는지 확인
                for (TilAiRequestDTO.CommitInfo existing : commits) {
                    if (existing.getMessage().equals(patch.getCommit_message())) {
                        commitInfo = existing;
                        break;
                    }
                }

                // 없으면 새로 생성
                if (commitInfo == null) {
                    commitInfo = TilAiRequestDTO.CommitInfo.builder()
                            .sha("SHA_PLACEHOLDER") // 실제 SHA는 없으므로 placeholder 사용
                            .message(patch.getCommit_message())
                            .author(commitDetail.getUsername())
                            .date(commitDetail.getDate())
                            .changes(new ArrayList<>())
                            .build();
                    commits.add(commitInfo);
                }

                // 파일 변경 정보 추가
                TilAiRequestDTO.FileChange fileChange = TilAiRequestDTO.FileChange.builder()
                        .filename(file.getFilepath())
                        .patch(patch.getPatch())
                        .content(file.getLatest_code())
                        .build();

                commitInfo.getChanges().add(fileChange);
            }
        }

        return TilAiRequestDTO.builder()
                .repository(String.valueOf(repositoryId)) // 레포지토리 ID 문자열로 변환
                .owner(commitDetail.getUsername()) // 사용자 이름을 owner로 사용
                .branch(branch) // 변경: branch 정보를 파라미터에서 받아옴
                .commits(commits)
                .build();
    }

    /**
     * AI 서버가 응답하지 않을 경우 사용할 대체 컨텐츠를 생성합니다.
     * 수정: branch 정보를 별도 파라미터로 받도록 변경
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
                .tags(List.of("개발", "자동생성", "커밋요약"))
                .build();
    }
}