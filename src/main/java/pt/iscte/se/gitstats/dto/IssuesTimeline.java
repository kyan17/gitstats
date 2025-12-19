package pt.iscte.se.gitstats.dto;

import java.util.List;

public record IssuesTimeline(
  String period,
  List<IssuesTimelinePoint> points,
  int totalOpen,
  int totalClosed
) {}

