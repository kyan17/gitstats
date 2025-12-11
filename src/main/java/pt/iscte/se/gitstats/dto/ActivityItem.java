package pt.iscte.se.gitstats.dto;

public record ActivityItem(
  String type,      // commit | issue | pr
  String title,
  String url,
  String state,
  String createdAt
) {}
