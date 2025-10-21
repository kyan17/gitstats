package pt.iscte.se.gitstats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ControllerExample {

  // http://localhost:8080/api/hello
  @GetMapping("/hello")
  public String test() {
    return "Hello Gitstats !";
  }

}
