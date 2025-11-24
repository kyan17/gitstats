package pt.iscte.se.gitstats;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GitHubService {

  private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
  private static final String GITHUB_API_BASE = "https://api.github.com";

  private final OAuth2AuthorizedClientService authorizedClientService;
  private final WebClient webClient;

  @Autowired
  public GitHubService(OAuth2AuthorizedClientService authorizedClientService) {
    this.authorizedClientService = Objects.requireNonNull(authorizedClientService);
    this.webClient = WebClient.builder()
        .clientConnector(new JettyClientHttpConnector())
        .baseUrl(GITHUB_API_BASE)
        .build();
  }

  private String getAccessToken(OAuth2AuthenticationToken authentication) {
    if (authentication == null) {
      throw new IllegalStateException("User not authenticated");
    }
    var principalName = authentication.getName();
    var registrationId = authentication.getAuthorizedClientRegistrationId();
    var authorizedClient = authorizedClientService.loadAuthorizedClient(
            registrationId,
            principalName
    );
    if (authorizedClient == null) {
      throw new IllegalStateException("No authorized client found for user: " + principalName);
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
    // Debug: log what we receive and what we compute
    Logger log = LoggerFactory.getLogger(GitHubService.class);
    log.info("Repo JSON: full_name={}, owner.login={}",
            node.get("full_name") != null ? node.get("full_name").asText() : null,
            ownerLogin
    );
    return new Repository(
            node.get("name").asText(),
            node.get("full_name").asText(),
            node.get("html_url").asText(),
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
    var contributors = webClient.get()
            .uri("/repos/{owner}/{repo}/contributors", owner, repo)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToFlux(Contributor.class)
            .collectList()
            .block();
    // Log on the server side
    log.info("Contributors for {}/{}:", owner, repo);
    if (contributors != null) {
      for (var cont : contributors) {
        log.info("  login=`{}`, contributions={}", cont.login(), cont.contributions());
      }
    } else {
      log.info("  no contributors returned");
    }
    return contributors;
  }

}
