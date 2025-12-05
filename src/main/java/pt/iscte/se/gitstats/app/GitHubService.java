package pt.iscte.se.gitstats.app;

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
            .maxInMemorySize(16 * 1024 * 1024)) // 16 MB
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
    // Make sure url is never null / empty
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
            // Parse the GitHub date like "2025-10-29T14:53:14Z" and reformat
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

  public CommitStats getCommitStatsForContributor(OAuth2AuthenticationToken authentication,
                                                  String owner,
                                                  String repo,
                                                  String authorLogin) {
    var accessToken = getAccessToken(authentication);

    var now = OffsetDateTime.now();
    var thirtyDaysAgo = now.minusDays(30);
    var sevenDaysAgo = now.minusDays(7);
    var githubFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    int allTimeTotalCommits = 0;
    long allTimeTotalLinesChanged = 0L;

    int periodTotalCommits = 0;
    int periodCommitsLastWeek = 0;
    long periodTotalLinesChanged = 0L;

    int page = 1;
    int pageSize = 100;

    while (true) {
      // copy to avoid "effectively final" problem in the lambda
      int currentPage = page;

      var commitsPage = fetchCommitsPage(
              accessToken,
              owner,
              repo,
              authorLogin,
              currentPage,
              pageSize
      );

      if (commitsPage == null || commitsPage.isEmpty()) {
        break;
      }

      for (JsonNode commitNode : commitsPage) {
        var commitInfo = commitNode.get("commit");
        if (commitInfo == null || commitInfo.isNull()) {
          continue;
        }
        var authorNode = commitInfo.get("author");
        if (authorNode == null || authorNode.isNull()) {
          continue;
        }
        var dateNode = authorNode.get("date");
        if (dateNode == null || dateNode.isNull()) {
          continue;
        }

        var commitDate = OffsetDateTime.parse(dateNode.asText(), githubFormatter);

        var shaNode = commitNode.get("sha");
        if (shaNode == null || shaNode.isNull()) {
          continue;
        }
        var commitSha = shaNode.asText();

        // fetch full commit to get stats
        var fullCommit = webClient.get()
                .uri("/repos/{owner}/{repo}/commits/{sha}", owner, repo, commitSha)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (fullCommit == null) {
          continue;
        }
        var statsNode = fullCommit.get("stats");
        if (statsNode == null || statsNode.isNull()) {
          continue;
        }

        int additions = statsNode.path("additions").asInt(0);
        int deletions = statsNode.path("deletions").asInt(0);
        int totalLinesChanged = additions + deletions;

        // all-time
        allTimeTotalCommits++;
        allTimeTotalLinesChanged += totalLinesChanged;

        // period: last 30 days
        if (!commitDate.isBefore(thirtyDaysAgo)) {
          periodTotalCommits++;
          periodTotalLinesChanged += totalLinesChanged;

          // last 7 days inside that period
          if (!commitDate.isBefore(sevenDaysAgo)) {
            periodCommitsLastWeek++;
          }
        }
      }

      if (commitsPage.size() < pageSize) {
        break;
      }
      page++;
    }

    double allTimeAverageLinesChanged =
            allTimeTotalCommits == 0 ? 0.0 : (double) allTimeTotalLinesChanged / allTimeTotalCommits;

    double periodAverageLinesChanged =
            periodTotalCommits == 0 ? 0.0 : (double) periodTotalLinesChanged / periodTotalCommits;

    return new CommitStats(
            authorLogin,
            allTimeTotalCommits,
            allTimeAverageLinesChanged,
            periodTotalCommits,
            periodCommitsLastWeek,
            periodAverageLinesChanged
    );
  }

  // helper, reused in the loop above, avoids capturing mutable "page" in the lambda
  private List<JsonNode> fetchCommitsPage(String accessToken,
                                          String owner,
                                          String repo,
                                          String authorLogin,
                                          int page,
                                          int perPage) {
    return webClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/repos/{owner}/{repo}/commits")
                    .queryParam("author", authorLogin)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .build(owner, repo))
            .headers(headers -> headers.setBearerAuth(accessToken))
            .retrieve()
            .bodyToFlux(JsonNode.class)
            .collectList()
            .block();
  }

}
