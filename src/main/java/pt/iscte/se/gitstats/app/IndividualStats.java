package pt.iscte.se.gitstats.app;

import pt.iscte.se.gitstats.dto.CommitPeriod;
import pt.iscte.se.gitstats.dto.CommitStats;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.function.client.WebClient;

public enum IndividualStats {;

  public static CommitStats getCommitStats(String accessToken,
                                           WebClient webClient,
                                           String owner,
                                           String repo,
                                           String login,
                                           CommitPeriod period) {
    Objects.requireNonNull(accessToken, "accessToken must not be null");
    Objects.requireNonNull(webClient, "webClient must not be null");
    var since = periodToSince(period);
    var defaultBranch = fetchDefaultBranch(accessToken, webClient, owner, repo);
    var commits = fetchCommitsForContributor(
            accessToken, webClient, owner, repo, login, since, defaultBranch
    );
    var commitCount = new AtomicInteger();
    var totalAdded = new AtomicLong();
    var totalDeleted = new AtomicLong();
    var distinctFiles = new HashSet<String>();

    for (JsonNode commit : commits) {
      var sha = commit.path("sha").asText(null);
      if (sha == null || sha.isBlank()) {
        continue;
      }
      var details = fetchCommitDetails(accessToken, webClient, owner, repo, sha);
      if (details == null) {
        continue;
      }
      commitCount.incrementAndGet();
      var statsNode = details.get("stats");
      if (statsNode != null && !statsNode.isNull()) {
        int added = statsNode.path("additions").asInt(0);
        int deleted = statsNode.path("deletions").asInt(0);
        totalAdded.addAndGet(added);
        totalDeleted.addAndGet(deleted);
      }
      var filesNode = details.get("files");
      if (filesNode != null && filesNode.isArray()) {
        filesNode.forEach(fileNode -> {
          var fileName = fileNode.path("filename").asText(null);
          if (fileName != null && !fileName.isBlank()) {
            distinctFiles.add(fileName);
          }
        });
      }
    }
    long totalLinesAdded = totalAdded.get();
    long totalLinesDeleted = totalDeleted.get();
    long netLinesChanged = totalLinesAdded - totalLinesDeleted;

    double avgCommitSizeLines =
            commitCount.get() == 0 ? 0.0 : (double) (totalLinesAdded + totalLinesDeleted) / commitCount.get();
    int distinctFilesTouched = distinctFiles.size();
    int topFilesModifiedCount = Math.min(5, distinctFilesTouched);
    int mainLanguagesCount = estimateLanguagesCount(distinctFiles);
    var issuePrStats = collectIssueAndPrStats(accessToken, webClient, owner, repo, login, since);
    return new CommitStats(
            login,
            period,
            commitCount.get(),
            avgCommitSizeLines,
            totalLinesAdded,
            totalLinesDeleted,
            netLinesChanged,
            distinctFilesTouched,
            topFilesModifiedCount,
            mainLanguagesCount,
            issuePrStats.issuesOpened,
            issuePrStats.issuesClosed,
            issuePrStats.prsOpened,
            issuePrStats.prsMerged,
            issuePrStats.prsClosed
    );
  }

  private static OffsetDateTime periodToSince(CommitPeriod period) {
    if (period == CommitPeriod.ALL_TIME) {
      return null;
    }
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    return switch (period) {
      case LAST_MONTH -> now.minusMonths(1);
      case LAST_WEEK -> now.minusWeeks(1);
      default -> null;
    };
  }

  private static List<JsonNode> fetchCommitsForContributor(String accessToken,
                                                           WebClient webClient,
                                                           String owner,
                                                           String repo,
                                                           String login,
                                                           OffsetDateTime since,
                                                           String branch) {

    List<JsonNode> withBranch = fetchCommitsPaged(accessToken, webClient, owner, repo, login, since, branch);
    if (withBranch.isEmpty() && branch != null && !branch.isBlank()) {
      return fetchCommitsPaged(accessToken, webClient, owner, repo, login, since, null);
    }
    return withBranch;
  }

  private static List<JsonNode> fetchCommitsPaged(String accessToken,
                                                  WebClient webClient,
                                                  String owner,
                                                  String repo,
                                                  String login,
                                                  OffsetDateTime since,
                                                  String branch) {
    final int pageSize = 100;
    List<JsonNode> allCommits = new ArrayList<>();
    int page = 1;

    while (true) {
      StringBuilder uriBuilder = new StringBuilder(
              "/repos/{owner}/{repo}/commits?author=" + urlEncode(login)
      );
      uriBuilder.append("&per_page=").append(pageSize);
      uriBuilder.append("&page=").append(page);
      if (branch != null && !branch.isBlank()) {
        uriBuilder.append("&sha=").append(urlEncode(branch));
      }
      if (since != null) {
        String sinceParam = since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        uriBuilder.append("&since=").append(urlEncode(sinceParam));
      }

      List<JsonNode> commitsPage = webClient.get()
              .uri(uriBuilder.toString(), owner, repo)
              .header("Authorization", "Bearer " + accessToken)
              .retrieve()
              .bodyToFlux(JsonNode.class)
              .collectList()
              .block();
      if (commitsPage == null || commitsPage.isEmpty()) {
        break;
      }
      allCommits.addAll(commitsPage);
      if (commitsPage.size() < pageSize) {
        break;
      }
      page++;
    }

    return allCommits;
  }

  private static String fetchDefaultBranch(String accessToken,
                                           WebClient webClient,
                                           String owner,
                                           String repo) {
    JsonNode repoNode = webClient.get()
            .uri("/repos/{owner}/{repo}", owner, repo)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

    String defaultBranch = repoNode == null ? null : repoNode.path("default_branch").asText(null);
    return (defaultBranch == null || defaultBranch.isBlank()) ? null : defaultBranch;
  }

  private static JsonNode fetchCommitDetails(String accessToken,
                                             WebClient webClient,
                                             String owner,
                                             String repo,
                                             String sha) {
    return webClient.get()
            .uri("/repos/{owner}/{repo}/commits/{sha}", owner, repo, sha)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
  }

  private static IssuePrStats collectIssueAndPrStats(String accessToken,
                                                     WebClient webClient,
                                                     String owner,
                                                     String repo,
                                                     String login,
                                                     OffsetDateTime since) {
    IssuePrStats stats = new IssuePrStats();
    int page = 1;
    final int pageSize = 100;
    DateTimeFormatter githubFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    while (true) {
      var issuePage = webClient.get()
              .uri("/repos/{owner}/{repo}/issues?state=all&per_page=" + pageSize + "&page=" + page, owner, repo)
              .header("Authorization", "Bearer " + accessToken)
              .retrieve()
              .bodyToFlux(JsonNode.class)
              .collectList()
              .block();

      if (issuePage == null || issuePage.isEmpty()) {
        break;
      }

      for (JsonNode issue : issuePage) {
        if (issue.hasNonNull("pull_request")) {
          continue;
        }

        JsonNode userNode = issue.path("user");
        String authorLogin = userNode.path("login").asText("");
        if (!login.equals(authorLogin)) {
          continue;
        }

        String createdAtStr = issue.path("created_at").asText(null);
        String closedAtStr = issue.path("closed_at").asText(null);
        OffsetDateTime createdAt = parseGithubDate(createdAtStr, githubFormatter);
        OffsetDateTime closedAt = parseGithubDate(closedAtStr, githubFormatter);

        boolean inCreatedWindow = since == null || (createdAt != null && !createdAt.isBefore(since));
        boolean inClosedWindow = since == null || (closedAt != null && !closedAt.isBefore(since));

        if (inCreatedWindow) {
          stats.issuesOpened++;
        }
        if (inClosedWindow && closedAt != null) {
          stats.issuesClosed++;
        }
      }

      if (issuePage.size() < pageSize) {
        break;
      }
      page++;
    }
    page = 1;
    while (true) {
      var prPage = webClient.get()
              .uri("/repos/{owner}/{repo}/pulls?state=all&per_page=" + pageSize + "&page=" + page, owner, repo)
              .header("Authorization", "Bearer " + accessToken)
              .retrieve()
              .bodyToFlux(JsonNode.class)
              .collectList()
              .block();
      if (prPage == null || prPage.isEmpty()) {
        break;
      }
      for (JsonNode pr : prPage) {
        var userNode = pr.path("user");
        var authorLogin = userNode.path("login").asText("");
        if (!login.equals(authorLogin)) {
          continue;
        }
        var createdAtStr = pr.path("created_at").asText(null);
        var closedAtStr = pr.path("closed_at").asText(null);
        var mergedAtStr = pr.path("merged_at").asText(null);
        var createdAt = parseGithubDate(createdAtStr, githubFormatter);
        var closedAt = parseGithubDate(closedAtStr, githubFormatter);
        var mergedAt = parseGithubDate(mergedAtStr, githubFormatter);
        boolean inCreatedWindow = since == null || (createdAt != null && !createdAt.isBefore(since));
        boolean inClosedWindow = since == null || (closedAt != null && !closedAt.isBefore(since));
        boolean inMergedWindow = since == null || (mergedAt != null && !mergedAt.isBefore(since));
        if (inCreatedWindow) {
          stats.prsOpened++;
        }
        if (inClosedWindow && closedAt != null) {
          stats.prsClosed++;
        }
        if (inMergedWindow && mergedAt != null) {
          stats.prsMerged++;
        }
      }
      if (prPage.size() < pageSize) {
        break;
      }
      page++;
    }
    return stats;
  }

  private static OffsetDateTime parseGithubDate(String value, DateTimeFormatter formatter) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value, formatter);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static int estimateLanguagesCount(Set<String> fileNames) {
    Set<String> extensions = new HashSet<>();
    for (String name : fileNames) {
      int idx = name.lastIndexOf('.');
      if (idx > 0 && idx < name.length() - 1) {
        String ext = name.substring(idx + 1).toLowerCase(Locale.ROOT);
        extensions.add(ext);
      }
    }
    return extensions.size();
  }

  public static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static final class IssuePrStats {
    private int issuesOpened;
    private int issuesClosed;
    private int prsOpened;
    private int prsMerged;
    private int prsClosed;
  }

}
