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
  updatedAt: string       // <- use this in the list
}

export type Contributor = {
  login: string
  avatarUrl: string
  htmlUrl: string
  contributions: number
}

export type Route =
    | {kind: 'home'}
    | {kind: 'list'}
    | {kind: 'repoDetails'; owner: string; name: string; description?: string}
