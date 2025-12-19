package pt.iscte.se.gitstats.app;

import pt.iscte.se.gitstats.dto.BranchInfo;
import pt.iscte.se.gitstats.dto.CommitNode;
import pt.iscte.se.gitstats.dto.CommitPeriod;
import pt.iscte.se.gitstats.dto.CommitStats;
import pt.iscte.se.gitstats.dto.Contributor;
import pt.iscte.se.gitstats.dto.NetworkGraph;
import pt.iscte.se.gitstats.dto.Repository;
import pt.iscte.se.gitstats.utils.NoAuthorizedClientException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  public NetworkGraph getNetworkGraph(OAuth2AuthenticationToken authentication,
                                      String owner,
                                      String repo,
                                      int maxCommits) {
    var accessToken = getAccessToken(authentication);

    // 1. Fetch repository info to get default branch
    JsonNode repoNode = webClient.get()
            .uri("/repos/{owner}/{repo}", owner, repo)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

    String defaultBranch = repoNode != null
            ? repoNode.path("default_branch").asText("main")
            : "main";

    // 2. Fetch all branches
    List<JsonNode> branchNodes = webClient.get()
            .uri("/repos/{owner}/{repo}/branches?per_page=100", owner, repo)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToFlux(JsonNode.class)
            .collectList()
            .block();

    List<BranchInfo> branches = new ArrayList<>();
    Map<String, List<String>> commitToBranches = new HashMap<>();

    if (branchNodes != null) {
      for (JsonNode branchNode : branchNodes) {
        String branchName = branchNode.path("name").asText();
        String sha = branchNode.path("commit").path("sha").asText();
        boolean isDefault = branchName.equals(defaultBranch);
        branches.add(new BranchInfo(branchName, sha, isDefault));

        // Map commit SHA to branch names
        commitToBranches.computeIfAbsent(sha, k -> new ArrayList<>()).add(branchName);
      }
    }

    // 3. Fetch commits from default branch
    List<JsonNode> commitNodes = webClient.get()
            .uri("/repos/{owner}/{repo}/commits?sha={branch}&per_page={perPage}",
                    owner, repo, defaultBranch, Math.min(maxCommits, 100))
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToFlux(JsonNode.class)
            .collectList()
            .block();

    List<CommitNode> commits = new ArrayList<>();
    DateTimeFormatter githubFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    if (commitNodes != null) {
      for (JsonNode commitNode : commitNodes) {
        String sha = commitNode.path("sha").asText();
        String shortSha = sha.length() >= 7 ? sha.substring(0, 7) : sha;

        JsonNode commitData = commitNode.path("commit");
        String message = commitData.path("message").asText("");
        // Truncate message to first line
        int newlineIdx = message.indexOf('\n');
        if (newlineIdx > 0) {
          message = message.substring(0, newlineIdx);
        }
        if (message.length() > 72) {
          message = message.substring(0, 69) + "...";
        }

        JsonNode authorNode = commitNode.path("author");
        String authorLogin = authorNode.isNull() || authorNode.isMissingNode()
                ? commitData.path("author").path("name").asText("Unknown")
                : authorNode.path("login").asText("Unknown");
        String authorAvatarUrl = authorNode.isNull() || authorNode.isMissingNode()
                ? ""
                : authorNode.path("avatar_url").asText("");

        String dateStr = commitData.path("author").path("date").asText("");
        String formattedDate = dateStr;
        try {
          OffsetDateTime dateTime = OffsetDateTime.parse(dateStr, githubFormatter);
          formattedDate = dateTime.format(outputFormatter);
        } catch (Exception ignored) {
        }

        // Get parent SHAs
        List<String> parentShas = new ArrayList<>();
        JsonNode parentsNode = commitNode.path("parents");
        if (parentsNode.isArray()) {
          for (JsonNode parentNode : parentsNode) {
            parentShas.add(parentNode.path("sha").asText());
          }
        }

        // Get branches that point to this commit
        List<String> branchNames = commitToBranches.getOrDefault(sha, List.of());

        commits.add(new CommitNode(
                sha,
                shortSha,
                message,
                authorLogin,
                authorAvatarUrl,
                formattedDate,
                parentShas,
                branchNames
        ));
      }
    }

    return new NetworkGraph(branches, commits, defaultBranch);
  }

}
