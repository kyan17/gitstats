package pt.iscte.se.gitstats.app;

import pt.iscte.se.gitstats.dto.CommitPeriod;
import pt.iscte.se.gitstats.dto.WorkTypeStats;
import pt.iscte.se.gitstats.NoAuthorizedClientException;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

  private final GitHubService gitHubService;

  @Autowired
  public ApiController(GitHubService gitHubService) {
    this.gitHubService = Objects.requireNonNull(gitHubService);
  }

  private static ResponseEntity<?> unauthorizedLoginFirst() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("message", "Please login first"));
  }

  private static ResponseEntity<?> unauthorizedLoginAgain() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("message", "Please login again"));
  }

  private static ResponseEntity<?> internalServerError(String message, Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("message", message, "detail", e.getMessage()));
  }

  private static boolean isAuthenticated(OAuth2AuthenticationToken authentication, OAuth2User principal) {
    return authentication != null && principal != null;
  }

  @GetMapping("/me")
  public ResponseEntity<?> me(@AuthenticationPrincipal OAuth2User principal) {
    if (principal == null) {
      return unauthorizedLoginFirst();
    }
    var login = principal.getAttribute("login");
    var name = principal.getAttribute("name");
    var avatar = principal.getAttribute("avatar_url");
    if (login == null || avatar == null) {
      return internalServerError("Error retrieving user info", new RuntimeException("Missing login or avatar"));
    }
    return ResponseEntity.ok(Map.of(
        "authenticated", true,
        "login", login,
        "name", name == null ? login : name,
        "avatarUrl", avatar
    ));
  }

  @GetMapping("/repositories")
  public ResponseEntity<?> repositories(OAuth2AuthenticationToken authentication,
                                        @AuthenticationPrincipal OAuth2User principal) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var repositories = gitHubService.getUserRepositories(authentication);
      return ResponseEntity.ok(repositories);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading repositories", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/contributors")
  public ResponseEntity<?> contributors(OAuth2AuthenticationToken authentication,
                                        @AuthenticationPrincipal OAuth2User principal,
                                        @PathVariable String owner,
                                        @PathVariable String repo) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var contributors = gitHubService.getContributors(authentication, owner, repo);
      return ResponseEntity.ok(contributors);
    } catch (RuntimeException e) {
      return internalServerError("Error loading contributors", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/contributors/{login}/commit-stats/all-time")
  public ResponseEntity<?> commitStatsAllTime(OAuth2AuthenticationToken authentication,
                                              @AuthenticationPrincipal OAuth2User principal,
                                              @PathVariable String owner,
                                              @PathVariable String repo,
                                              @PathVariable String login) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var stats = gitHubService.getAllTimeStats(authentication, owner, repo, login);
      return ResponseEntity.ok(stats);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading commit stats", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/contributors/{login}/commit-stats/last-month")
  public ResponseEntity<?> commitStatsLastMonth(OAuth2AuthenticationToken authentication,
                                                @AuthenticationPrincipal OAuth2User principal,
                                                @PathVariable String owner,
                                                @PathVariable String repo,
                                                @PathVariable String login) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var stats = gitHubService.getLastMonthStats(authentication, owner, repo, login);
      return ResponseEntity.ok(stats);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading commit stats", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/contributors/{login}/commit-stats/last-week")
  public ResponseEntity<?> commitStatsLastWeek(OAuth2AuthenticationToken authentication,
                                               @AuthenticationPrincipal OAuth2User principal,
                                               @PathVariable String owner,
                                               @PathVariable String repo,
                                               @PathVariable String login) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var stats = gitHubService.getLastWeekStats(authentication, owner, repo, login);
      return ResponseEntity.ok(stats);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading commit stats", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/network")
  public ResponseEntity<?> networkGraph(OAuth2AuthenticationToken authentication,
                                        @AuthenticationPrincipal OAuth2User principal,
                                        @PathVariable String owner,
                                        @PathVariable String repo,
                                        @RequestParam(defaultValue = "50") int maxCommits) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var network = gitHubService.getNetworkGraph(authentication, owner, repo, maxCommits);
      return ResponseEntity.ok(network);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading network graph", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/languages")
  public ResponseEntity<?> languages(OAuth2AuthenticationToken authentication,
                                     @AuthenticationPrincipal OAuth2User principal,
                                     @PathVariable String owner,
                                     @PathVariable String repo) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var languages = gitHubService.getLanguages(authentication, owner, repo);
      return ResponseEntity.ok(languages);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading languages", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/commit-timeline")
  public ResponseEntity<?> commitTimeline(OAuth2AuthenticationToken authentication,
                                          @AuthenticationPrincipal OAuth2User principal,
                                          @PathVariable String owner,
                                          @PathVariable String repo,
                                          @RequestParam(defaultValue = "day") String period) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var timeline = gitHubService.getCommitTimeline(authentication, owner, repo, period);
      return ResponseEntity.ok(timeline);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading commit timeline", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/issues-timeline")
  public ResponseEntity<?> issuesTimeline(OAuth2AuthenticationToken authentication,
                                          @AuthenticationPrincipal OAuth2User principal,
                                          @PathVariable String owner,
                                          @PathVariable String repo,
                                          @RequestParam(defaultValue = "day") String period) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var timeline = gitHubService.getIssuesTimeline(authentication, owner, repo, period);
      return ResponseEntity.ok(timeline);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading issues timeline", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/pull-requests-timeline")
  public ResponseEntity<?> pullRequestsTimeline(OAuth2AuthenticationToken authentication,
                                                @AuthenticationPrincipal OAuth2User principal,
                                                @PathVariable String owner,
                                                @PathVariable String repo,
                                                @RequestParam(defaultValue = "day") String period) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      var timeline = gitHubService.getPullRequestsTimeline(authentication, owner, repo, period);
      return ResponseEntity.ok(timeline);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading pull requests timeline", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/contribution-stats")
  public ResponseEntity<?> contributionStats(OAuth2AuthenticationToken authentication,
                                             @AuthenticationPrincipal OAuth2User principal,
                                             @PathVariable String owner,
                                             @PathVariable String repo,
                                             @RequestParam(defaultValue = "ALL_TIME") String period) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      CommitPeriod p = switch (period) {
        case "LAST_MONTH" -> CommitPeriod.LAST_MONTH;
        case "LAST_WEEK" -> CommitPeriod.LAST_WEEK;
        default -> CommitPeriod.ALL_TIME;
      };
      var stats = gitHubService.getContributionStats(authentication, owner, repo, p);
      return ResponseEntity.ok(stats);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading contribution stats", e);
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/worktype-stats")
  public ResponseEntity<?> workTypeStats(OAuth2AuthenticationToken authentication,
                                         @AuthenticationPrincipal OAuth2User principal,
                                         @PathVariable String owner,
                                         @PathVariable String repo,
                                         @RequestParam(defaultValue = "ALL_TIME") String period) {
    if (!isAuthenticated(authentication, principal)) {
      return unauthorizedLoginFirst();
    }
    try {
      CommitPeriod p = switch (period) {
        case "LAST_MONTH" -> CommitPeriod.LAST_MONTH;
        case "LAST_WEEK" -> CommitPeriod.LAST_WEEK;
        default -> CommitPeriod.ALL_TIME;
      };
      WorkTypeStats stats = gitHubService.getWorkTypeStats(authentication, owner, repo, p);
      return ResponseEntity.ok(stats);
    } catch (NoAuthorizedClientException e) {
      return unauthorizedLoginAgain();
    } catch (RuntimeException e) {
      return internalServerError("Error loading work type stats", e);
    }
  }

}
