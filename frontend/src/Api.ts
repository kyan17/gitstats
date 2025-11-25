import type {Contributor, Repo, MeResponse} from './Types.ts'

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
    // Normal, not an error: user is not authenticated
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

export const fetchContributors = (owner: string, name: string) =>
    fetchJson<Contributor[]>(
        `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(name)}/contributors`,
    )
