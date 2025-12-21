package pt.iscte.se.gitstats.dto;

import java.util.List;

public record ContributionStats(
    String owner,
    String repo,
    CommitPeriod period,
    List<ContributionSlice> slices
) {}
