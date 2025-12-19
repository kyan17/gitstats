package pt.iscte.se.gitstats.dto;

public record IssuesTimelinePoint(
  String label,
  int opened,
  int closed
) {}

