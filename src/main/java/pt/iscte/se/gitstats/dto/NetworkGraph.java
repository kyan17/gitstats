package pt.iscte.se.gitstats.dto;

import java.util.List;

public record NetworkGraph(
  List<BranchInfo> branches,
  List<CommitNode> commits,
  String defaultBranch
) {}

