package pt.iscte.se.gitstats.app;

import pt.iscte.se.gitstats.utils.NoAuthorizedClientException;

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

  @GetMapping("/me")
  public ResponseEntity<?> me(@AuthenticationPrincipal OAuth2User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("authenticated", false));
    }
    var login = principal.getAttribute("login");
    var name = principal.getAttribute("name");
    var avatar = principal.getAttribute("avatar_url");
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
    if (authentication == null || principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login first"));
    }
    try {
      var repositories = gitHubService.getUserRepositories(authentication);
      return ResponseEntity.ok(repositories);
    } catch (NoAuthorizedClientException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login again"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("message", "Error loading repositories", "detail", e.getMessage()));
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/contributors")
  public ResponseEntity<?> contributors(OAuth2AuthenticationToken authentication,
                                        @AuthenticationPrincipal OAuth2User principal,
                                        @PathVariable String owner,
                                        @PathVariable String repo) {
    if (authentication == null || principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login first"));
    }
    try {
      var contributors = gitHubService.getContributors(authentication, owner, repo);
      return ResponseEntity.ok(contributors);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("message", "Error loading contributors", "detail", e.getMessage()));
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/contributors/{login}/commit-stats/all-time")
  public ResponseEntity<?> commitStatsAllTime(OAuth2AuthenticationToken authentication,
                                              @AuthenticationPrincipal OAuth2User principal,
                                              @PathVariable String owner,
                                              @PathVariable String repo,
                                              @PathVariable String login) {
    if (authentication == null || principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login first"));
    }
    try {
      var stats = gitHubService.getAllTimeStats(authentication, owner, repo, login);
      return ResponseEntity.ok(stats);
    } catch (NoAuthorizedClientException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login again"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("message", "Error loading commit stats", "detail", e.getMessage()));
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/contributors/{login}/commit-stats/last-month")
  public ResponseEntity<?> commitStatsLastMonth(OAuth2AuthenticationToken authentication,
                                                @AuthenticationPrincipal OAuth2User principal,
                                                @PathVariable String owner,
                                                @PathVariable String repo,
                                                @PathVariable String login) {
    if (authentication == null || principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login first"));
    }
    try {
      var stats = gitHubService.getLastMonthStats(authentication, owner, repo, login);
      return ResponseEntity.ok(stats);
    } catch (NoAuthorizedClientException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login again"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("message", "Error loading commit stats", "detail", e.getMessage()));
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/contributors/{login}/commit-stats/last-week")
  public ResponseEntity<?> commitStatsLastWeek(OAuth2AuthenticationToken authentication,
                                               @AuthenticationPrincipal OAuth2User principal,
                                               @PathVariable String owner,
                                               @PathVariable String repo,
                                               @PathVariable String login) {
    if (authentication == null || principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login first"));
    }
    try {
      var stats = gitHubService.getLastWeekStats(authentication, owner, repo, login);
      return ResponseEntity.ok(stats);
    } catch (NoAuthorizedClientException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login again"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("message", "Error loading commit stats", "detail", e.getMessage()));
    }
  }

  @GetMapping("/repositories/{owner}/{repo}/network")
  public ResponseEntity<?> networkGraph(OAuth2AuthenticationToken authentication,
                                        @AuthenticationPrincipal OAuth2User principal,
                                        @PathVariable String owner,
                                        @PathVariable String repo,
                                        @RequestParam(defaultValue = "50") int maxCommits) {
    if (authentication == null || principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login first"));
    }
    try {
      var network = gitHubService.getNetworkGraph(authentication, owner, repo, maxCommits);
      return ResponseEntity.ok(network);
    } catch (NoAuthorizedClientException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of("message", "Please login again"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("message", "Error loading network graph", "detail", e.getMessage()));
    }
  }

}
