export type User = {
  authenticated: boolean
  login?: string
  name?: string
  avatarUrl?: string
}

export type Repo = {
  name: string
  fullName: string
  url: string
  description: string
  isPrivate: boolean
  ownerLogin: string | null
  updatedAt: string
}

export type RawContributor = {
  login: string
  avatar_url: string
  html_url: string
  contributions: number
}

export type Contributor = {
  login: string
  avatarUrl: string
  htmlUrl: string
  contributions: number
}

export type Route =
    | { kind: 'home' }
    | { kind: 'list' }
    | { kind: 'repoDetails'; owner: string; name: string; description: string }
    | { kind: 'postLogout' };

export type MeResponse = {
  authenticated: boolean
  login?: string
  name?: string
  avatarUrl?: string
}

export type CommitPeriod = 'ALL_TIME' | 'LAST_MONTH' | 'LAST_WEEK'

export type CommitStats = {
  authorLogin: string
  period: CommitPeriod
  commitCount: number
  avgCommitSizeLines: number
  totalLinesAdded: number
  totalLinesDeleted: number
  netLinesChanged: number
  distinctFilesTouched: number
  topFilesModifiedCount: number
  mainLanguagesCount: number
  issuesOpen: number
  issuesClosed: number
  prsOpen: number
  prsMerged: number
  prsClosed: number
}

export type BranchInfo = {
  name: string
  sha: string
  isDefault: boolean
}

export type CommitNode = {
  sha: string
  shortSha: string
  message: string
  authorLogin: string
  authorAvatarUrl: string
  date: string
  parentShas: string[]
  branches: string[]
}

export type NetworkGraph = {
  branches: BranchInfo[]
  commits: CommitNode[]
  defaultBranch: string
}

export type LanguageStats = {
  name: string
  bytes: number
  percentage: number
  color: string
}

export type TimelinePoint = {
  label: string
  count: number
}

export type CommitTimeline = {
  period: 'day' | 'week' | 'month'
  points: TimelinePoint[]
}

export type IssuesTimelinePoint = {
  label: string
  opened: number
  closed: number
}

export type IssuesTimeline = {
  period: 'day' | 'week' | 'month'
  points: IssuesTimelinePoint[]
  totalOpen: number
  totalClosed: number
}

export type PullRequestsTimelinePoint = {
  label: string
  opened: number
  merged: number
}

export type PullRequestsTimeline = {
  period: 'day' | 'week' | 'month'
  points: PullRequestsTimelinePoint[]
  totalOpen: number
  totalMerged: number
}

export type Period = 'day' | 'week' | 'month'

export type ContributionSlice = {
  login: string
  score: number
}

export type ContributionStats = {
  owner: string
  repo: string
  period: CommitPeriod
  slices: ContributionSlice[]
}

export type WorkTypeStats = {
  owner: string
  repo: string
  period: CommitPeriod
  featureCommits: number
  bugfixCommits: number
  refactorCommits: number
  testCommits: number
  documentationCommits: number
}
