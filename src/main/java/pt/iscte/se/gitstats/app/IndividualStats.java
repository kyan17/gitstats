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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

public enum IndividualStats {
  ;

  private static final Logger log = LoggerFactory.getLogger(IndividualStats.class);

  public static CommitStats getCommitStats(String accessToken,
                                           WebClient webClient,
                                           String owner,
                                           String repo,
                                           String login,
                                           CommitPeriod period) {

    Objects.requireNonNull(accessToken, "accessToken must not be null");
    Objects.requireNonNull(webClient, "webClient must not be null");

    OffsetDateTime since = periodToSince(period);

    String defaultBranch = fetchDefaultBranch(accessToken, webClient, owner, repo);

    log.info("[IndividualStats] Computing stats for {}/{} contributor={} period={} since={}",
            owner, repo, login, period, since);

    List<JsonNode> commits = fetchCommitsForContributor(
            accessToken, webClient, owner, repo, login, since, defaultBranch
    );

    log.info("[IndividualStats] Found {} commits for contributor {} in period {}", commits.size(), login, period);

    AtomicInteger commitCount = new AtomicInteger();
    AtomicLong totalAdded = new AtomicLong();
    AtomicLong totalDeleted = new AtomicLong();
    Set<String> distinctFiles = new HashSet<>();

    for (JsonNode commit : commits) {
      String sha = commit.path("sha").asText(null);
      if (sha == null || sha.isBlank()) {
        continue;
      }
      JsonNode details = fetchCommitDetails(accessToken, webClient, owner, repo, sha);
      if (details == null) {
        continue;
      }

      // Count every commit once; stats may be missing but count should reflect commit total
      commitCount.incrementAndGet();

      JsonNode statsNode = details.get("stats");
      if (statsNode != null && !statsNode.isNull()) {
        int added = statsNode.path("additions").asInt(0);
        int deleted = statsNode.path("deletions").asInt(0);
        totalAdded.addAndGet(added);
        totalDeleted.addAndGet(deleted);
      }

      JsonNode filesNode = details.get("files");
      if (filesNode != null && filesNode.isArray()) {
        filesNode.forEach(fileNode -> {
          String fileName = fileNode.path("filename").asText(null);
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
    int topFilesModifiedCount = Math.min(5, distinctFilesTouched); // simple heuristic
    int mainLanguagesCount = estimateLanguagesCount(distinctFiles);

    // Issues & PRs: fetch directly from /issues and /pulls endpoints and filter by author + timestamps
    IssuePrStats issuePrStats = collectIssueAndPrStats(accessToken, webClient, owner, repo, login, since);

    log.info("[IndividualStats] Issue/PR counts for {}/{} contributor={} period={}: issuesOpened={} issuesClosed={} prsOpened={} prsMerged={} prsClosed={}",
            owner, repo, login, period,
            issuePrStats.issuesOpened,
            issuePrStats.issuesClosed,
            issuePrStats.prsOpened,
            issuePrStats.prsMerged,
            issuePrStats.prsClosed);

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
      // Fallback: if default branch guess was wrong, retry without branch filter
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
        break; // last page reached
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
    // If missing or blank, don't force a branch filter; we'll fall back to all branches
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

  /**
   * Aggregate issues and pull requests authored by this user in the repo, using direct list endpoints.
   * We avoid /search/issues here because it can have subtle behavior differences vs the web UI.
   */
  private static IssuePrStats collectIssueAndPrStats(String accessToken,
                                                     WebClient webClient,
                                                     String owner,
                                                     String repo,
                                                     String login,
                                                     OffsetDateTime since) {
    IssuePrStats stats = new IssuePrStats();

    // 1) Issues (non-PR) from /repos/{owner}/{repo}/issues
    // GitHub REST: this returns both issues and PRs; filter out PRs by checking pull_request field.
    int page = 1;
    final int pageSize = 100;
    DateTimeFormatter githubFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    while (true) {
      StringBuilder uriBuilder = new StringBuilder("/repos/{owner}/{repo}/issues?state=all&per_page=");
      uriBuilder.append(pageSize).append("&page=").append(page);

      List<JsonNode> issuePage = webClient.get()
              .uri(uriBuilder.toString(), owner, repo)
              .header("Authorization", "Bearer " + accessToken)
              .retrieve()
              .bodyToFlux(JsonNode.class)
              .collectList()
              .block();

      if (issuePage == null || issuePage.isEmpty()) {
        break;
      }

      for (JsonNode issue : issuePage) {
        // Skip items that are actually PRs (they have a pull_request field)
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

    // 2) Pull requests from /repos/{owner}/{repo}/pulls?state=all
    page = 1;
    while (true) {
      StringBuilder uriBuilder = new StringBuilder("/repos/{owner}/{repo}/pulls?state=all&per_page=");
      uriBuilder.append(pageSize).append("&page=").append(page);

      List<JsonNode> prPage = webClient.get()
              .uri(uriBuilder.toString(), owner, repo)
              .header("Authorization", "Bearer " + accessToken)
              .retrieve()
              .bodyToFlux(JsonNode.class)
              .collectList()
              .block();

      if (prPage == null || prPage.isEmpty()) {
        break;
      }

      for (JsonNode pr : prPage) {
        JsonNode userNode = pr.path("user");
        String authorLogin = userNode.path("login").asText("");
        if (!login.equals(authorLogin)) {
          continue;
        }

        String createdAtStr = pr.path("created_at").asText(null);
        String closedAtStr = pr.path("closed_at").asText(null);
        String mergedAtStr = pr.path("merged_at").asText(null);

        OffsetDateTime createdAt = parseGithubDate(createdAtStr, githubFormatter);
        OffsetDateTime closedAt = parseGithubDate(closedAtStr, githubFormatter);
        OffsetDateTime mergedAt = parseGithubDate(mergedAtStr, githubFormatter);

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
    } catch (Exception e) {
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
    int issuesOpened;
    int issuesClosed;
    int prsOpened;
    int prsMerged;
    int prsClosed;
  }

}


