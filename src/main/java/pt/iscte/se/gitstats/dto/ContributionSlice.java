package pt.iscte.se.gitstats.dto;

public record ContributionSlice(
  String login,
  long score
) {}
