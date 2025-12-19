package pt.iscte.se.gitstats.dto;

public record PullRequestsTimelinePoint(
  String label,
  int opened,
  int merged
) {}

