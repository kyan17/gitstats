package pt.iscte.se.gitstats;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

  private final GitHubService gitHubService;

  @Autowired
  public HomeController(GitHubService gitHubService) {
    this.gitHubService = Objects.requireNonNull(gitHubService);
  }

  @GetMapping("/")
  public String root() {
    // Serve the React SPA entrypoint
    return "forward:/index.html";
  }

  @GetMapping("/legacy")
  public String home(Model model,
                     @AuthenticationPrincipal OAuth2User principal,
                     @RequestParam(value = "logout", required = false) String logout
  ) {
    if (principal != null) {
      model.addAttribute("name", principal.getAttribute("login"));
      model.addAttribute("avatar", principal.getAttribute("avatar_url"));
      return "welcome";
    }
    if (logout != null) {
      model.addAttribute("logoutMessage", "You have been logged out successfully.");
    }
    return "index";
  }

  @GetMapping("/repositories")
  public String repositories(Model model,
                             OAuth2AuthenticationToken authentication,
                             @AuthenticationPrincipal OAuth2User principal) {
    try {
      if (authentication == null || principal == null) {
        model.addAttribute("error", "Please login first");
        model.addAttribute("name", "");
        model.addAttribute("avatar", "");
        return "welcome";
      }

      // Add user info to model in case of error
      model.addAttribute("name", principal.getAttribute("login"));
      model.addAttribute("avatar", principal.getAttribute("avatar_url"));

      List<Repository> repos = gitHubService.getUserRepositories(authentication);
      model.addAttribute("repositories", repos);
      return "repositories";
    } catch (Exception e) {
      e.printStackTrace(); // Debug
      // Preserve user info even on error
      if (principal != null) {
        model.addAttribute("name", principal.getAttribute("login"));
        model.addAttribute("avatar", principal.getAttribute("avatar_url"));
      }
      model.addAttribute("error", "Error loading repositories: " + e.getMessage());
      return "welcome";
    }
  }

  @GetMapping("/repository/{owner}/{name}")
  public String repositoryDetails(
          @PathVariable String owner,
          @PathVariable String name,
          @RequestParam(value = "description", required = false) String description,
          Model model
  ) {
    model.addAttribute("owner", owner);
    model.addAttribute("name", name);
    model.addAttribute("description", description);
    return "repository-details";
  }

  @GetMapping("/post-logout")
  public String postLogout() {
    return "post-logout";
  }

}
