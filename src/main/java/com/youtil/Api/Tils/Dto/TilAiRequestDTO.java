package com.youtil.Api.Tils.Dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TilAiRequestDTO {

    private String owner; // 조직명/개인닉네임
    private String date; //날짜
    private String repo; // 레포명
    private String branch; // 브랜치명
    private List<String> sha_list; // sha 리스트
    private String requestId;
    private String githubToken;
}
