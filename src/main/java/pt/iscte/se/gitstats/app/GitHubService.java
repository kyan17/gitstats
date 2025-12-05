package pt.iscte.se.gitstats.app;

import pt.iscte.se.gitstats.dto.CommitPeriod;
import pt.iscte.se.gitstats.dto.CommitStats;
import pt.iscte.se.gitstats.dto.Contributor;
import pt.iscte.se.gitstats.dto.Repository;
import pt.iscte.se.gitstats.utils.NoAuthorizedClientException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.jetty.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GitHubService {

  private static final String GITHUB_API_BASE = "https://api.github.com";

  private final OAuth2AuthorizedClientService authorizedClientService;
  private final WebClient webClient;

  @Autowired
  public GitHubService(OAuth2AuthorizedClientService authorizedClientService) {
    this.authorizedClientService = Objects.requireNonNull(authorizedClientService);
    var httpClient = new HttpClient();
    this.webClient = WebClient.builder()
      .baseUrl(GITHUB_API_BASE)
      .clientConnector(new JettyClientHttpConnector(httpClient))
      .codecs(configurer -> configurer
              .defaultCodecs()
              .maxInMemorySize(16 * 1024 * 1024))
      .build();
  }

  private String getAccessToken(OAuth2AuthenticationToken authentication) {
    if (authentication == null) {
      throw new NoAuthorizedClientException("User not authenticated");
    }
    var principalName = authentication.getName();
    var registrationId = authentication.getAuthorizedClientRegistrationId();
    var authorizedClient = authorizedClientService.loadAuthorizedClient(
            registrationId,
            principalName
    );
    if (authorizedClient == null) {
      throw new NoAuthorizedClientException("No authorized client found for user: " + principalName);
    }
    return authorizedClient.getAccessToken().getTokenValue();
  }

  private static Repository convertToRepository(JsonNode node) {
    var githubFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    var simpleFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    var ownerNode = node.get("owner");
    var ownerLogin = (ownerNode != null && !ownerNode.isNull() && ownerNode.get("login") != null)
            ? ownerNode.get("login").asText()
            : null;
    var htmlUrlNode = node.get("html_url");
    var htmlUrl = (htmlUrlNode != null && !htmlUrlNode.isNull())
            ? htmlUrlNode.asText()
            : "";
    return new Repository(
            node.get("name").asText(),
            node.get("full_name").asText(),
            htmlUrl,
            node.get("description").isNull() ? "" : node.get("description").asText(),
            node.get("private").asBoolean(),
            ownerLogin,
            OffsetDateTime.parse(node.get("updated_at").asText(), githubFormatter).format(simpleFormatter)
    );
  }

  public List<Repository> getUserRepositories(OAuth2AuthenticationToken authentication) {
    var accessToken = getAccessToken(authentication);
    return webClient.get()
        .uri("/user/repos?sort=updated&per_page=100")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .bodyToFlux(JsonNode.class)
        .map(GitHubService::convertToRepository)
        .collectList()
        .block();
  }

  public List<Contributor> getContributors(OAuth2AuthenticationToken authentication,
                                           String owner,
                                           String repo) {
    var accessToken = getAccessToken(authentication);
    return webClient.get()
            .uri("/repos/{owner}/{repo}/contributors", owner, repo)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToFlux(Contributor.class)
            .collectList()
            .block();
  }

  public CommitStats getAllTimeStats(OAuth2AuthenticationToken authentication,
                                     String owner,
                                     String repo,
                                     String login) {
    var accessToken = getAccessToken(authentication);
    return IndividualStats.getCommitStats(
            accessToken,
            webClient,
            owner,
            repo,
            login,
            CommitPeriod.ALL_TIME
    );
  }

  public CommitStats getLastMonthStats(OAuth2AuthenticationToken authentication,
                                       String owner,
                                       String repo,
                                       String login) {
    var accessToken = getAccessToken(authentication);
    return IndividualStats.getCommitStats(
            accessToken,
            webClient,
            owner,
            repo,
            login,
            CommitPeriod.LAST_MONTH
    );
  }

  public CommitStats getLastWeekStats(OAuth2AuthenticationToken authentication,
                                      String owner,
                                      String repo,
                                      String login) {
    var accessToken = getAccessToken(authentication);
    return IndividualStats.getCommitStats(
            accessToken,
            webClient,
            owner,
            repo,
            login,
            CommitPeriod.LAST_WEEK
    );
  }

}
