package pt.iscte.se.gitstats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GitstatsApplication {

  static void main(String[] args) {
    SpringApplication.run(GitstatsApplication.class, args);
    System.out.println("Hello Spring Boot !");
  }

}
