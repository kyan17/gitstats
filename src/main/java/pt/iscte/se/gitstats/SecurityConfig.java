package pt.iscte.se.gitstats;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository repository) {
    return new InMemoryOAuth2AuthorizedClientService(repository);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/index", "/oauth2/**", "/login**", "/post-logout").permitAll()
        .requestMatchers("/repositories", "/repository/**").authenticated()
        .anyRequest().authenticated())
        .oauth2Login(Customizer.withDefaults())
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(logout -> logout
            .logoutSuccessUrl("/post-logout")
            .clearAuthentication(true)
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID"));
    return http.build();
  }

}
