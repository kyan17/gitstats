package pt.iscte.se.gitstats.dto;

import java.util.List;

public record CommitNode(
  String sha,
  String shortSha,
  String message,
  String authorLogin,
  String authorAvatarUrl,
  String date,
  List<String> parentShas,
  List<String> branches
) {}

