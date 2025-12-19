package pt.iscte.se.gitstats.dto;

import java.util.List;

public record PullRequestsTimeline(
  String period,
  List<PullRequestsTimelinePoint> points,
  int totalOpen,
  int totalMerged
) {}

