package pt.iscte.se.gitstats.dto;

public record WorkTypeStats(
  String owner,
  String repo,
  CommitPeriod period,
  long featureCommits,
  long bugfixCommits,
  long refactorCommits,
  long testCommits,
  long documentationCommits
) {}
