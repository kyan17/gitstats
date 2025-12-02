package pt.iscte.se.gitstats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Contributor(
  @JsonProperty("login")
  String login,

  @JsonProperty("avatar_url")
  String avatarUrl,

  @JsonProperty("html_url")
  String htmlUrl,

  @JsonProperty("contributions")
  int contributions
) {}
