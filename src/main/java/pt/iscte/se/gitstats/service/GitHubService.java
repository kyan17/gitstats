package pt.iscte.se.gitstats.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pt.iscte.se.gitstats.model.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GitHubService {

  @Autowired
  private OAuth2AuthorizedClientService authorizedClientService;
  
  private static final String GITHUB_API_BASE = "https://api.github.com";
  private final WebClient webClient;

  public GitHubService() {
    this.webClient = WebClient.builder()
        .baseUrl(GITHUB_API_BASE)
        .build();
  }

  public List<Repository> getUserRepositories(OAuth2AuthenticationToken authentication) {
    String accessToken = getAccessToken(authentication);
    
    List<GitHubRepo> repos = webClient.get()
        .uri("/user/repos?sort=updated&per_page=100")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .bodyToFlux(GitHubRepo.class)
        .collectList()
        .block();

    return repos.stream()
        .map(this::convertToRepository)
        .toList();
  }

  private String getAccessToken(OAuth2AuthenticationToken authentication) {
    if (authentication == null) {
      throw new IllegalStateException("User not authenticated");
    }

    String principalName = authentication.getName();
    String registrationId = authentication.getAuthorizedClientRegistrationId();

    OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
        registrationId,
        principalName
    );

    if (authorizedClient == null) {
      throw new IllegalStateException("No authorized client found for user: " + principalName);
    }

    return authorizedClient.getAccessToken().getTokenValue();
  }

  private Repository convertToRepository(GitHubRepo gitHubRepo) {
    LocalDateTime updatedAt = LocalDateTime.parse(
        gitHubRepo.updatedAt,
        DateTimeFormatter.ISO_DATE_TIME
    );

    return new Repository(
        gitHubRepo.name,
        gitHubRepo.fullName,
        gitHubRepo.htmlUrl,
        gitHubRepo.description,
        gitHubRepo.isPrivate,
        updatedAt
    );
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class GitHubRepo {
    public String name;
    @JsonProperty("full_name")
    public String fullName;
    @JsonProperty("html_url")
    public String htmlUrl;
    public String description;
    @JsonProperty("private")
    public boolean isPrivate;
    @JsonProperty("updated_at")
    public String updatedAt;
  }
}

