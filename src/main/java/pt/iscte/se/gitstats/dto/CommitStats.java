package pt.iscte.se.gitstats.dto;

import java.util.List;
import pt.iscte.se.gitstats.dto.ActivityItem;

public record CommitStats(
  String authorLogin,
  CommitPeriod period,

  // --- Commit stats ---
  int commitCount,
  double avgCommitSizeLines,
  long totalLinesAdded,
  long totalLinesDeleted,
  long netLinesChanged,

  // --- File stats ---
  int distinctFilesTouched,
  int topFilesModifiedCount,
  int mainLanguagesCount,

  // --- Issues activity ---
  int issuesOpen,
  int issuesClosed,

  // --- Pull requests activity ---
  int prsOpen,
  int prsMerged,
  int prsClosed,

  // --- Recent activity ---
  List<ActivityItem> recentActivity
) {}
