import type {Contributor, Repo} from './Types.ts'

export const loginUrl = '/oauth2/authorization/github'

export const fetchJson = async <T, >(path: string): Promise<T> => {
  const res = await fetch(path, {credentials: 'include'})
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`Request failed ${res.status}: ${text || path}`)
  }
  return (await res.json()) as T
}

export const fetchMe = () => fetchJson<{
  authenticated: boolean;
  login?: string;
  name?: string;
  avatarUrl?: string
}>('/api/me')

export const fetchRepositories = () => fetchJson<Repo[]>('/api/repositories')

export const fetchContributors = (owner: string, name: string) =>
    fetchJson<Contributor[]>(
        `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(name)}/contributors`,
    )
