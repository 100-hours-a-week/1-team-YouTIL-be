package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitResponseDTO;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubCommitService {

    private final WebClient webClient;
    private final TokenEncryptor tokenEncryptor;
    private final EntityValidator entityValidator;

    public CommitResponseDTO getCommits(Long userId, Long organizationId, Long repositoryId, String branch, String date) {
        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String token = decryptToken(user.getGithubToken());
        String sinceIso = LocalDate.parse(date)
                .atStartOfDay(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        String owner;
        String repoName;

        if (organizationId != null) {
            owner = getOrganizationLogin(organizationId, token);
            repoName = getRepositoryNameFromOrg(owner, repositoryId, token);
        } else {
            Map.Entry<String, String> result = getPersonalRepoInfo(repositoryId, token);
            owner = result.getKey();
            repoName = result.getValue();
        }

        return fetchCommits(owner, repoName, branch, sinceIso, token);
    }

    private CommitResponseDTO fetchCommits(String owner, String repoName, String branch, String sinceIso, String token) {
        String commitsUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/commits" +
                "?sha=" + branch + "&since=" + sinceIso;

        Map<String, Object>[] commits = webClient.get()
                .uri(commitsUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        List<CommitResponseDTO.CommitItem> commitItems = new ArrayList<>();

        if (commits != null) {
            for (Map<String, Object> commit : commits) {
                String sha = commit.get("sha").toString();

                Map<String, Object> detail = webClient.get()
                        .uri("https://api.github.com/repos/" + owner + "/" + repoName + "/commits/" + sha)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                String message = ((Map<String, Object>) detail.get("commit")).get("message").toString();
                List<Map<String, Object>> files = (List<Map<String, Object>>) detail.get("files");

                List<CommitResponseDTO.ChangeItem> changes = files.stream()
                        .map(f -> new CommitResponseDTO.ChangeItem(
                                f.get("filename").toString(),
                                f.getOrDefault("patch", "").toString()))
                        .toList();

                commitItems.add(new CommitResponseDTO.CommitItem(sha, message, changes));
            }
        }

        return new CommitResponseDTO(commitItems);
    }

    private String getOrganizationLogin(Long organizationId, String token) {
        Map<String, Object>[] orgs = webClient.get()
                .uri("https://api.github.com/user/orgs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        for (Map<String, Object> org : orgs) {
            if (Long.valueOf(org.get("id").toString()).equals(organizationId)) {
                return org.get("login").toString();
            }
        }

        throw new RuntimeException("해당 조직을 찾을 수 없습니다.");
    }

    private String getRepositoryNameFromOrg(String owner, Long repositoryId, String token) {
        Map<String, Object>[] repos = webClient.get()
                .uri("https://api.github.com/orgs/" + owner + "/repos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        for (Map<String, Object> repo : repos) {
            if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                return repo.get("name").toString();
            }
        }

        throw new RuntimeException("조직 내에서 레포지토리를 찾을 수 없습니다.");
    }

    private Map.Entry<String, String> getPersonalRepoInfo(Long repositoryId, String token) {
        Map<String, Object>[] repos = webClient.get()
                .uri("https://api.github.com/user/repos?affiliation=owner")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        for (Map<String, Object> repo : repos) {
            if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                String repoName = repo.get("name").toString();
                String owner = ((Map<String, Object>) repo.get("owner")).get("login").toString();
                return Map.entry(owner, repoName);
            }
        }

        throw new RuntimeException("개인 레포지토리를 찾을 수 없습니다.");
    }

    private void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            throw new RuntimeException("GitHub 토큰이 없습니다.");
        }
    }

    private String decryptToken(String token) {
        try {
            return tokenEncryptor.decrypt(token);
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰 복호화 실패");
        }
    }
}
