package pt.iscte.se.gitstats.dto;

import java.util.List;

public record CommitTimeline(
  String period,  // "day", "week", or "month"
  List<TimelinePoint> points
) {}

