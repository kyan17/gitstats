package pt.iscte.se.gitstats.dto;

public record CommitStats(
  String authorLogin,

  // All-time stats (from repo creation)
  int allTimeTotalCommits,
  double allTimeAverageLinesChanged,

  // Period stats (currently last 30 days by default)
  int periodTotalCommits,
  int periodCommitsLastWeek,
  double periodAverageLinesChanged
) {}
