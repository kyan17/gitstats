package pt.iscte.se.gitstats;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
public class SecurityConfig {

  @Bean
  public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository repository) {
    return new InMemoryOAuth2AuthorizedClientService(repository);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
                                         @Qualifier("mvcHandlerMappingIntrospector") HandlerMappingIntrospector introspector) throws Exception {
    MvcRequestMatcher apiMatcher = new MvcRequestMatcher(introspector, "/api/**");
    MvcRequestMatcher logoutGetMatcher = new MvcRequestMatcher(introspector, "/logout");
    logoutGetMatcher.setMethod(HttpMethod.GET);

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
            "/post-logout"
        ).permitAll()
        .requestMatchers("/api/**", "/repositories", "/repository/**").authenticated()
        .anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                apiMatcher
            )
        )
        .oauth2Login(oauth -> oauth
            .defaultSuccessUrl("/", true)
            .failureUrl("/legacy?error")
        )
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(logout -> logout
            .logoutRequestMatcher(logoutGetMatcher)
            .logoutSuccessUrl("/post-logout")
            .clearAuthentication(true)
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID"));
    return http.build();
  }

}
