package pt.iscte.se.gitstats;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

  @Value("${app.frontend-base-url:}")
  private String frontendBaseUrl;

  @Bean
  public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository repository) {
    return new InMemoryOAuth2AuthorizedClientService(repository);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    String successUrl;
    if (frontendBaseUrl != null && !frontendBaseUrl.isBlank()) {
      successUrl = frontendBaseUrl + "/list";
    } else {
      successUrl = "/list";
    }
    http.authorizeHttpRequests(auth -> auth
          .requestMatchers(
                  "/",
                  "/index",
                  "/index.html",
                  "/assets/**",
                  "/favicon.ico",
                  "/manifest.json",
                  "/oauth2/**",
                  "/login**",
                  "/post-logout",
                  "/logout-app",
                  "/github-logout"
          ).permitAll()
          .requestMatchers("/api/**", "/repositories", "/repository/**", "/list").authenticated()
          .anyRequest().authenticated())
          .exceptionHandling(ex -> ex
            .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    (request) -> request.getRequestURI().startsWith("/api/")
            )
          )
          .oauth2Login(oauth -> oauth
                  .defaultSuccessUrl(successUrl, true)
                  .failureUrl("/legacy?error")
          )
          .formLogin(AbstractHttpConfigurer::disable)
          .logout(logout -> logout
                  .logoutUrl("/logout")
                  .logoutSuccessUrl("/")
                  .clearAuthentication(true)
                  .invalidateHttpSession(true)
                  .deleteCookies("JSESSIONID"));
    return http.build();
  }

}
