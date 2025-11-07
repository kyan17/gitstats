package pt.iscte.se.gitstats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Repository(
  @JsonProperty("name")
  String name,

  @JsonProperty("full_name")
  String fullName,

  @JsonProperty("html_url")
  String url,

  @JsonProperty("description")
  String description,

  @JsonProperty("private")
  boolean isPrivate,

  @JsonProperty("updated_at")
  String updatedAt
) {}
