package pt.iscte.se.gitstats.app;

import pt.iscte.se.gitstats.dto.CommitPeriod;
import pt.iscte.se.gitstats.dto.CommitStats;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    List<JsonNode> commits = fetchCommitsForContributor(
            accessToken, webClient, owner, repo, login, since
    );

    AtomicInteger commitCount = new AtomicInteger();
    AtomicLong totalAdded = new AtomicLong();
    AtomicLong totalDeleted = new AtomicLong();
    Set<String> distinctFiles = new HashSet<>();

    for (JsonNode commit : commits) {
      String sha = commit.get("sha").asText();
      JsonNode details = fetchCommitDetails(accessToken, webClient, owner, repo, sha);

      if (details == null) {
        continue;
      }

      JsonNode statsNode = details.get("stats");
      if (statsNode != null && !statsNode.isNull()) {
        int added = statsNode.path("additions").asInt(0);
        int deleted = statsNode.path("deletions").asInt(0);
        totalAdded.addAndGet(added);
        totalDeleted.addAndGet(deleted);
        commitCount.incrementAndGet();
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

    int issuesOpened = searchIssuesOrPrs(
            accessToken, webClient, owner, repo, login, since, "issue", "author"
    );
    int issuesClosed = searchIssuesOrPrs(
            accessToken, webClient, owner, repo, login, since, "issue", "assignee+state:closed"
    );

    int prsOpened = searchIssuesOrPrs(
            accessToken, webClient, owner, repo, login, since, "pr", "author"
    );
    int prsMerged = searchMergedPullRequests(
            accessToken, webClient, owner, repo, login, since
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
            issuesOpened,
            issuesClosed,
            prsOpened,
            prsMerged
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
                                                           OffsetDateTime since) {

    StringBuilder uriBuilder = new StringBuilder(
            "/repos/{owner}/{repo}/commits?author=" + urlEncode(login) + "&per_page=100"
    );
    if (since != null) {
      String sinceParam = since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      uriBuilder.append("&since=").append(urlEncode(sinceParam));
    }

    return webClient.get()
            .uri(uriBuilder.toString(), owner, repo)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToFlux(JsonNode.class)
            .collectList()
            .block();
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
                                       String extraQualifier) {

    StringBuilder q = new StringBuilder();
    q.append("repo:").append(owner).append("/").append(repo).append("+");
    q.append("type:").append(type).append("+");
    q.append(extraQualifier).append(":").append(login);
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
