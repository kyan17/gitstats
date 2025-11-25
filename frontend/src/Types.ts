export type User = {
  authenticated: boolean
  login?: string
  name?: string
  avatarUrl?: string
}

export type Repo = {
  name?: string
  fullName?: string
  url: string
  description: string
  private: boolean
  ownerLogin?: string | null
  updatedAt: string
}

export type Contributor = {
  login: string
  avatarUrl: string
  htmlUrl: string
  contributions: number
}

export type Route =
    | { kind: 'home' }
    | { kind: 'repoDetails'; owner: string; name: string; description?: string }
