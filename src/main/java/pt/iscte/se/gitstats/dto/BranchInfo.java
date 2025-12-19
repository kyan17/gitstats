package pt.iscte.se.gitstats.dto;

public record BranchInfo(
  String name,
  String sha,
  boolean isDefault
) {}

