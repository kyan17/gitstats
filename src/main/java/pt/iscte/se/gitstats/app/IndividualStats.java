package pt.iscte.se.gitstats.app;

import pt.iscte.se.gitstats.dto.CommitPeriod;
import pt.iscte.se.gitstats.dto.CommitStats;
import pt.iscte.se.gitstats.dto.ActivityItem;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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

    OffsetDateTime since = periodToSince(period);

    String defaultBranch = fetchDefaultBranch(accessToken, webClient, owner, repo);

    List<JsonNode> commits = fetchCommitsForContributor(
            accessToken, webClient, owner, repo, login, since, defaultBranch
    );

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

    int issuesOpen = searchIssuesOrPrs(
            accessToken, webClient, owner, repo, login, since, "issue",
            "author:" + login,
            "state:open"
    );
    int issuesClosed = searchIssuesOrPrs(
            accessToken, webClient, owner, repo, login, since, "issue",
            "author:" + login,
            "state:closed"
    );

    int prsOpen = searchIssuesOrPrs(
            accessToken, webClient, owner, repo, login, since, "pr",
            "author:" + login,
            "state:open"
    );
    int prsMerged = searchMergedPullRequests(
            accessToken, webClient, owner, repo, login, since
    );
    int prsClosedTotal = searchIssuesOrPrs(
            accessToken, webClient, owner, repo, login, since, "pr",
            "author:" + login,
            "state:closed"
    );
    int prsClosed = Math.max(0, prsClosedTotal - prsMerged);

    List<ActivityItem> recentActivity = buildRecentActivity(
            accessToken, webClient, owner, repo, login, since, commits
    );

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
            issuesOpen,
            issuesClosed,
            prsOpen,
            prsMerged,
            prsClosed,
            recentActivity
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

  private static int searchIssuesOrPrs(String accessToken,
                                       WebClient webClient,
                                       String owner,
                                       String repo,
                                       String login,
                                       OffsetDateTime since,
                                       String type, // "issue" or "pr"
                                       String... extraQualifiers) {

    StringBuilder q = new StringBuilder();
    q.append("repo:").append(owner).append("/").append(repo);
    q.append("+type:").append(type);
    for (String qualifier : extraQualifiers) {
      q.append("+").append(qualifier);
    }
    if (since != null) {
      String sinceDate = since.toLocalDate().toString();
      q.append("+created:>=").append(sinceDate);
    }

    String uri = "/search/issues?q=" + urlEncode(q.toString());

    JsonNode response = webClient.get()
            .uri(uri)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

    return response == null ? 0 : response.path("total_count").asInt(0);
  }

  private static int searchMergedPullRequests(String accessToken,
                                              WebClient webClient,
                                              String owner,
                                              String repo,
                                              String login,
                                              OffsetDateTime since) {

    StringBuilder q = new StringBuilder();
    q.append("repo:").append(owner).append("/").append(repo).append("+");
    q.append("type:pr+is:merged+");
    q.append("author:").append(login);
    if (since != null) {
      String sinceDate = since.toLocalDate().toString();
      q.append("+merged:>=").append(sinceDate);
    }

    String uri = "/search/issues?q=" + urlEncode(q.toString());

    JsonNode response = webClient.get()
            .uri(uri)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

    return response == null ? 0 : response.path("total_count").asInt(0);
  }

  private static List<ActivityItem> buildRecentActivity(String accessToken,
                                                        WebClient webClient,
                                                        String owner,
                                                        String repo,
                                                        String login,
                                                        OffsetDateTime since,
                                                        List<JsonNode> commits) {
    List<ActivityItem> items = new ArrayList<>();

    // Latest commits (API already returns sorted by date desc)
    int commitLimit = Math.min(5, commits.size());
    for (int i = 0; i < commitLimit; i++) {
      JsonNode c = commits.get(i);
      String title = c.path("commit").path("message").asText("(no message)");
      String url = c.path("html_url").asText("");
      String createdAt = c.path("commit").path("author").path("date").asText("");
      items.add(new ActivityItem("commit", title, url, "committed", createdAt));
    }

    // Latest issues and PRs by author
    items.addAll(fetchRecentIssuesOrPrsList(accessToken, webClient, owner, repo, login, since, "issue", 5));
    items.addAll(fetchRecentIssuesOrPrsList(accessToken, webClient, owner, repo, login, since, "pr", 5));

    items.sort((a, b) -> compareDateDesc(a.createdAt(), b.createdAt()));
    if (items.size() > 10) {
      return new ArrayList<>(items.subList(0, 10));
    }
    return items;
  }

  private static List<ActivityItem> fetchRecentIssuesOrPrsList(String accessToken,
                                                               WebClient webClient,
                                                               String owner,
                                                               String repo,
                                                               String login,
                                                               OffsetDateTime since,
                                                               String type,
                                                               int limit) {
    StringBuilder q = new StringBuilder();
    q.append("repo:").append(owner).append("/").append(repo);
    q.append("+type:").append(type);
    q.append("+author:").append(login);
    if (since != null) {
      String sinceDate = since.toLocalDate().toString();
      q.append("+updated:>=").append(sinceDate);
    }

    String uri = "/search/issues?q=" + urlEncode(q.toString()) +
            "&sort=updated&order=desc&per_page=" + Math.max(1, limit);

    JsonNode response = webClient.get()
            .uri(uri)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

    if (response == null) {
      return Collections.emptyList();
    }
    JsonNode items = response.path("items");
    if (items == null || !items.isArray()) {
      return Collections.emptyList();
    }

    List<ActivityItem> result = new ArrayList<>();
    int count = 0;
    for (JsonNode item : items) {
      if (count >= limit) break;
      String title = item.path("title").asText("(no title)");
      String url = item.path("html_url").asText("");
      String state = item.path("state").asText("");
      String createdAt = item.path("created_at").asText("");
      result.add(new ActivityItem(type, title, url, state, createdAt));
      count++;
    }
    return result;
  }

  private static int compareDateDesc(String a, String b) {
    try {
      OffsetDateTime da = OffsetDateTime.parse(a);
      OffsetDateTime db = OffsetDateTime.parse(b);
      return db.compareTo(da);
    } catch (Exception e) {
      return 0;
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

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

}
