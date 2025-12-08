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

// Map raw backend contributor JSON -> frontend Contributor type
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

  // Commit stats
  commitCount: number
  avgCommitSizeLines: number
  totalLinesAdded: number
  totalLinesDeleted: number
  netLinesChanged: number

  // File stats
  distinctFilesTouched: number
  topFilesModifiedCount: number
  mainLanguagesCount: number

  // Issues
  issuesOpen: number
  issuesClosed: number

  // PRs
  prsOpen: number
  prsMerged: number
  prsClosed: number

  // Recent activity
  recentActivity: ActivityItem[]
}

export type ActivityItem = {
  type: 'commit' | 'issue' | 'pr'
  title: string
  url: string
  state: string
  createdAt: string
}
