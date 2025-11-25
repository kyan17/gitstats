package pt.iscte.se.gitstats;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

  @GetMapping("/logout-app")
  public void logoutApp(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Invalidate Spring Security session + authentication
    new SecurityContextLogoutHandler().logout(request, null, null);

    // Redirect to SPA with post-logout state
    response.sendRedirect("/?state=post-logout");
  }

  @GetMapping("/github-logout")
  public void githubLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Optional: also clear local session when hitting the GitHub logout shortcut
    var session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    // Redirect to GitHub account management/logout page
    response.sendRedirect("https://github.com/logout");
  }

}
