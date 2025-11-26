import type {RawContributor, Contributor, Repo, MeResponse} from './Types.ts'

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
