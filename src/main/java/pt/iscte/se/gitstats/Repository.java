package pt.iscte.se.gitstats;

import java.time.LocalDateTime;

public class Repository {
  private String name;
  private String fullName;
  private String url;
  private String description;
  private boolean isPrivate;
  private LocalDateTime updatedAt;

  public Repository() {}

  public Repository(String name, String fullName, String url, String description, boolean isPrivate, LocalDateTime updatedAt) {
    this.name = name;
    this.fullName = fullName;
    this.url = url;
    this.description = description;
    this.isPrivate = isPrivate;
    this.updatedAt = updatedAt;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public void setPrivate(boolean aPrivate) {
    isPrivate = aPrivate;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
