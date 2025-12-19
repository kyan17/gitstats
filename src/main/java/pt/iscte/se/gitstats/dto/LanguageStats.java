package pt.iscte.se.gitstats.dto;

public record LanguageStats(
  String name,
  long bytes,
  double percentage,
  String color
) {}

