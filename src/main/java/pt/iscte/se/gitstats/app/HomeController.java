package pt.iscte.se.gitstats.app;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  // SPA entrypoint : homepage
  @GetMapping("/")
  public String root() {
    return "forward:/index.html";
  }

  // SPA entrypoint : repositories list page
  @GetMapping("/list")
  public String listPage() {
    return "forward:/index.html";
  }

  // SPA entrypoint : repo details page
  @GetMapping("/repository/{owner}/{name}")
  public String repoDetailsSpa() {
    return "forward:/index.html";
  }

  // SPA entrypoint : Gitstats logout page
  @GetMapping("/logout-app")
  public void logoutApp(HttpServletRequest request, HttpServletResponse response) throws IOException {
    new SecurityContextLogoutHandler().logout(request, null, null);
    response.sendRedirect("/?state=post-logout");
  }

  // SPA entrypoint : GitHub logout page
  @GetMapping("/github-logout")
  public void githubLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
    var session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    response.sendRedirect("https://github.com/logout");
  }

}
