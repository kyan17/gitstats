import type {
  RawContributor,
  Contributor,
  Repo,
  MeResponse,
  CommitStats,
  NetworkGraph,
  LanguageStats,
  CommitTimeline,
  IssuesTimeline,
  PullRequestsTimeline,
  ContributionStats,
  WorkTypeStats,
} from "./Types.ts";
import { getCached, setCached } from "./Cache.ts";

export const loginUrl = "/oauth2/authorization/github";

export const fetchJson = async <T>(path: string): Promise<T> => {
  const res = await fetch(path, { credentials: "include" });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`Request failed ${res.status}: ${text || path}`);
  }
  return (await res.json()) as T;
};

export async function fetchMe(): Promise<MeResponse> {
  const res = await fetch("/api/me", { credentials: "include" });
  if (res.status === 401) {
    return { authenticated: false };
  }
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    console.error("[fetchMe] unexpected error", res.status, text);
    throw new Error(`Failed to load profile: ${res.status}`);
  }
  return res.json();
}

// Wrap fetchJson to use cache for GET requests
async function fetchJsonCached<T>(url: string, cacheKey?: string): Promise<T> {
  const cached = cacheKey ? getCached(cacheKey) : undefined;
  if (cached !== undefined) {
    return cached as T;
  }
  const data = await fetchJson<T>(url);
  if (cacheKey) {
    setCached(cacheKey, data);
  }
  return data;
}

export async function fetchRepositories() {
  return fetchJsonCached<Repo[]>("/api/repositories", "repositories");
}

export async function fetchContributors(
  owner: string,
  name: string,
): Promise<Contributor[]> {
  const raw = await fetchJsonCached<RawContributor[]>(
    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(name)}/contributors`,
    `contributors:${owner}/${name}`,
  );
  return raw.map((c) => ({
    login: c.login,
    avatarUrl: c.avatar_url,
    htmlUrl: c.html_url,
    contributions: c.contributions,
  }));
}

export async function fetchCommitStats(
  owner: string,
  repo: string,
  login: string,
  period: "ALL_TIME" | "LAST_MONTH" | "LAST_WEEK",
): Promise<CommitStats> {
  const path =
    period === "ALL_TIME"
      ? `/api/repositories/${owner}/${repo}/contributors/${login}/commit-stats/all-time`
      : period === "LAST_MONTH"
        ? `/api/repositories/${owner}/${repo}/contributors/${login}/commit-stats/last-month`
        : `/api/repositories/${owner}/${repo}/contributors/${login}/commit-stats/last-week`;
  const cacheKey = `commitStats:${owner}:${repo}:${login}:${period}`;
  return fetchJsonCached<CommitStats>(path, cacheKey);
}

export const fetchNetworkGraph = (
  owner: string,
  repo: string,
  maxCommits: number = 50,
) =>
  fetchJsonCached<NetworkGraph>(
    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/network?maxCommits=${maxCommits}`,
    `networkGraph:${owner}:${repo}:${maxCommits}`,
  );

export const fetchLanguages = (owner: string, repo: string) =>
  fetchJsonCached<LanguageStats[]>(
    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/languages`,
    `languages:${owner}:${repo}`,
  );

export const fetchCommitTimeline = (
  owner: string,
  repo: string,
  period: "day" | "week" | "month" = "day",
) =>
  fetchJsonCached<CommitTimeline>(
    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/commit-timeline?period=${period}`,
    `commitTimeline:${owner}:${repo}:${period}`,
  );

export const fetchIssuesTimeline = (
  owner: string,
  repo: string,
  period: "day" | "week" | "month" = "day",
) =>
  fetchJsonCached<IssuesTimeline>(
    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues-timeline?period=${period}`,
    `issuesTimeline:${owner}:${repo}:${period}`,
  );

export const fetchPullRequestsTimeline = (
  owner: string,
  repo: string,
  period: "day" | "week" | "month" = "day",
) =>
  fetchJsonCached<PullRequestsTimeline>(
    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/pull-requests-timeline?period=${period}`,
    `pullRequestsTimeline:${owner}:${repo}:${period}`,
  );

export const fetchContributionStats = (
  owner: string,
  repo: string,
  period: "ALL_TIME" | "LAST_MONTH" | "LAST_WEEK" = "ALL_TIME",
) =>
  fetchJsonCached<ContributionStats>(
    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/contribution-stats?period=${period}`,
    `contributionStats:${owner}:${repo}:${period}`,
  );

export const fetchWorkTypeStats = (
  owner: string,
  repo: string,
  period: "ALL_TIME" | "LAST_MONTH" | "LAST_WEEK" = "ALL_TIME",
) =>
  fetchJsonCached<WorkTypeStats>(
    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/worktype-stats?period=${period}`,
    `workTypeStats:${owner}:${repo}:${period}`,
  );
