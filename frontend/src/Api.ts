import type {RawContributor, Contributor, Repo, MeResponse, CommitStats, NetworkGraph, LanguageStats, CommitTimeline, IssuesTimeline, PullRequestsTimeline, ContributionStats, WorkTypeStats} from './Types.ts'

export const loginUrl = '/oauth2/authorization/github'

export const fetchJson = async <T, >(path: string): Promise<T> => {
  const res = await fetch(path, {credentials: 'include'})
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`Request failed ${res.status}: ${text || path}`)
  }
  return (await res.json()) as T
}

export async function fetchMe(): Promise<MeResponse> {
  const res = await fetch('/api/me', {credentials: 'include'})
  if (res.status === 401) {
    return {authenticated: false}
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    console.error('[fetchMe] unexpected error', res.status, text)
    throw new Error(`Failed to load profile: ${res.status}`)
  }
  return res.json()
}

export const fetchRepositories = () => fetchJson<Repo[]>('/api/repositories')

export const fetchContributors = async (owner: string, name: string): Promise<Contributor[]> => {
  const raw = await fetchJson<RawContributor[]>(
      `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(name)}/contributors`,
  )
  return raw.map((c) => ({
    login: c.login,
    avatarUrl: c.avatar_url,
    htmlUrl: c.html_url,
    contributions: c.contributions,
  }))
}

export async function fetchCommitStats(owner: string, repo: string, login: string, period: 'ALL_TIME' | 'LAST_MONTH' | 'LAST_WEEK'): Promise<CommitStats> {
  const path =
    period === 'ALL_TIME'
      ? `/api/repositories/${owner}/${repo}/contributors/${login}/commit-stats/all-time`
      : period === 'LAST_MONTH'
        ? `/api/repositories/${owner}/${repo}/contributors/${login}/commit-stats/last-month`
        : `/api/repositories/${owner}/${repo}/contributors/${login}/commit-stats/last-week`

  console.debug('[Api.fetchCommitStats] request', {owner, repo, login, period, path})
  const res = await fetch(path, {credentials: 'include'})
  if (!res.ok) {
    const text = await res.text()
    console.error('[Api.fetchCommitStats] failed', res.status, text)
    throw new Error(`Request failed ${res.status}: ${text}`)
  }
  const json = (await res.json()) as CommitStats
  console.debug('[Api.fetchCommitStats] response', json)
  return json
}

export const fetchNetworkGraph = (owner: string, repo: string, maxCommits: number = 50) =>
    fetchJson<NetworkGraph>(
        `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(
            repo,
        )}/network?maxCommits=${maxCommits}`,
    )

export const fetchLanguages = (owner: string, repo: string) =>
    fetchJson<LanguageStats[]>(
        `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/languages`,
    )

export const fetchCommitTimeline = (owner: string, repo: string, period: 'day' | 'week' | 'month' = 'day') =>
    fetchJson<CommitTimeline>(
        `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/commit-timeline?period=${period}`,
    )

export const fetchIssuesTimeline = (owner: string, repo: string, period: 'day' | 'week' | 'month' = 'day') =>
    fetchJson<IssuesTimeline>(
        `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues-timeline?period=${period}`,
    )

export const fetchPullRequestsTimeline = (owner: string, repo: string, period: 'day' | 'week' | 'month' = 'day') =>
    fetchJson<PullRequestsTimeline>(
        `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/pull-requests-timeline?period=${period}`,
    )

export const fetchContributionStats = (owner: string, repo: string, period: 'ALL_TIME' | 'LAST_MONTH' | 'LAST_WEEK' = 'ALL_TIME') =>
  fetchJson<ContributionStats>(`/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/contribution-stats?period=${period}`)

export const fetchWorkTypeStats = (
  owner: string,
  repo: string,
  period: 'ALL_TIME' | 'LAST_MONTH' | 'LAST_WEEK' = 'ALL_TIME',
) =>
  fetchJson<WorkTypeStats>(
    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/worktype-stats?period=${period}`,
  )
