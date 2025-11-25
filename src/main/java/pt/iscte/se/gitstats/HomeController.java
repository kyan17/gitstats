package pt.iscte.se.gitstats;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

  // SPA entrypoint: homepage
  @GetMapping("/")
  public String root() {
    return "forward:/index.html";
  }

  // SPA entrypoint : repositories list page
  @GetMapping("/list")
  public String listPage() {
    return "forward:/index.html";
  }

  // SPA entrypoint: repo details page (if you have /repository/... in React)
  @GetMapping("/repository/{owner}/{name}")
  public String repoDetailsSpa(@PathVariable String owner, @PathVariable String name) {
    return "forward:/index.html";
  }

  // --- legacy Thymeleaf endpoints below, if you still need them ---

  @GetMapping("/legacy")
  public String home(Model model,
                     @RequestParam(value = "logout", required = false) String logout,
                     @RequestParam(value = "error", required = false) String error) {
    if (logout != null) {
      model.addAttribute("logoutMessage", "You have been logged out successfully.");
    }
    if (error != null) {
      model.addAttribute("error", "Login failed.");
    }
    return "index";
  }

  @GetMapping("/repositories")
  public String repositories(Model model,
                             OAuth2AuthenticationToken authentication,
                             @org.springframework.security.core.annotation.AuthenticationPrincipal OAuth2User principal) {
    // ... keep or delete this legacy path, it is not used by the SPA ...
    return "repositories";
  }

  @GetMapping("/post-logout")
  public String postLogout() {
    return "post-logout";
  }

}
