package pt.iscte.se.gitstats;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/index", "/oauth2/**", "/login**").permitAll()
        .anyRequest().authenticated())
        .oauth2Login(Customizer.withDefaults())
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(logout -> logout.logoutSuccessUrl("/"));
    return http.build();
  }

}
