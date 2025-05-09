package com.youtil.Api.Tils.Converter;

import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Tils.Dto.TilAiRequestDTO;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.Til;
import com.youtil.Model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * TIL 관련 DTO 변환 클래스
 */
public class TilDtoConverter {

    /**
     * CommitDetailResponse를 TilAiRequestDTO로 변환
     */
    public static TilAiRequestDTO toTilAiRequest(
            CommitDetailResponseDTO.CommitDetailResponse commitDetail,
            Long repositoryId,
            String title) {

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

        return TilAiRequestDTO.builder()
                .username(commitDetail.getUsername())
                .date(commitDetail.getDate())
                .repo(String.valueOf(repositoryId))
                .title(title)  // title 필드 설정
                .files(fileInfos)
                .build();
    }

    /**
     * CreateWithAiRequest와 AiResponse로부터 CreateAiTilRequest 생성
     */
    public static TilRequestDTO.CreateAiTilRequest toCreateAiTilRequest(
            TilRequestDTO.CreateWithAiRequest request,
            TilAiResponseDTO aiResponse) {

        List<String> tags = new ArrayList<>(aiResponse.getKeywords());
        if (!tags.contains(request.getCategory())) {
            tags.add(request.getCategory());
        }

        return TilRequestDTO.CreateAiTilRequest.builder()
                .repo(String.valueOf(request.getRepositoryId()))
                .title(request.getTitle())
                .category(request.getCategory())
                .content(aiResponse.getContent())
                .tags(tags)
                .isShared(request.getIsShared())
                .build();
    }

    /**
     * Til 엔티티를 TilDetailResponse로 변환
     */
    public static TilResponseDTO.TilDetailResponse toTilDetailResponse(Til til) {
        return TilResponseDTO.TilDetailResponse.builder()
                .id(til.getId())
                .userId(til.getUser().getId())
                .nickname(til.getUser().getNickname())
                .profileImageUrl(til.getUser().getProfileImageUrl())
                .title(til.getTitle())
                .content(til.getContent())
                .category(til.getCategory())
                .tag(til.getTag())
                .isDisplay(til.getIsDisplay())
                .commitRepository(til.getCommitRepository())
                .isUploaded(til.getIsUploaded())
                .recommendCount(til.getRecommendCount())
                .visitedCount(til.getVisitedCount())
                .commentsCount(til.getCommentsCount())
                .createdAt(til.getCreatedAt())
                .updatedAt(til.getUpdatedAt())
                .build();
    }

    /**
     * 태그 리스트 처리 (카테고리가 태그에 없는 경우 추가)
     */
    public static List<String> processTagList(List<String> tags, String category) {
        List<String> result = new ArrayList<>();
        if (tags != null && !tags.isEmpty()) {
            result.addAll(tags);
        }
        // 기본적으로 카테고리도 태그로 추가
        if (category != null && !category.isEmpty() && !result.contains(category)) {
            result.add(category);
        }
        return result;
    }

    /**
     * TIL 엔티티 생성
     * isShared 값이 true면 isDisplay에 1을, false면 0을 저장
     */
    public static Til createTilEntity(TilRequestDTO.CreateAiTilRequest request, User user, List<String> tags) {
        // isShared 값에 따라 display 값 설정 (true -> 1, false -> 0)
        Boolean isDisplay = request.getIsShared() != null && request.getIsShared();

        return Til.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .tag(tags)
                .isDisplay(isDisplay) // isShared 값에 따라 true/false 설정
                .commitRepository(request.getRepo())
                .isUploaded(true) // GitHub 업로드 여부는 별도로 관리됨, 기본값은 true
                .recommendCount(0)
                .visitedCount(0)
                .commentsCount(0)
                .status(Status.active)
                .build();
    }

    /**
     * AI 서버 연결 실패 시 대체 TIL 내용 생성
     */
    public static TilAiResponseDTO createFallbackResponse(CommitDetailResponseDTO.CommitDetailResponse commitDetail) {
        // 파일별 패치의 커밋 메시지를 추출
        StringBuilder commitMessagesBuilder = new StringBuilder();

        if (commitDetail.getFiles() != null) {
            for (CommitDetailResponseDTO.FileDetail file : commitDetail.getFiles()) {
                for (CommitDetailResponseDTO.PatchDetail patch : file.getPatches()) {
                    commitMessagesBuilder.append("- ").append(patch.getCommit_message()).append("\n");
                }
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