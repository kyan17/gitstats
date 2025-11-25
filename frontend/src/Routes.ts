import type {Repo, Route} from './Types.ts';

export const splitFullName = (fullName?: string): { owner?: string; repo?: string } => {
  if (!fullName) return {}
  const parts = fullName.split('/')
  if (parts.length !== 2) return {}
  return {owner: parts[0], repo: parts[1]}
}

export const repoDetailsPath = (repo: Repo): string => {
  if (repo.ownerLogin && repo.name) {
    const q = repo.description ? `?description=${encodeURIComponent(repo.description)}` : ''
    return `/repository/${encodeURIComponent(repo.ownerLogin)}/${encodeURIComponent(repo.name)}${q}`
  }
  const {owner, repo: repoName} = splitFullName(repo.fullName)
  if (owner && repoName) {
    const q = repo.description ? `?description=${encodeURIComponent(repo.description)}` : ''
    return `/repository/${encodeURIComponent(owner)}/${encodeURIComponent(repoName)}${q}`
  }
  return '#'
}

export const parseLocation = (): Route => {
  const path = window.location.pathname
  const search = window.location.search
  const repoMatch = path.match(/^\/repository\/([^/]+)\/([^/]+)$/)
  if (repoMatch) {
    const owner = decodeURIComponent(repoMatch[1])
    const name = decodeURIComponent(repoMatch[2])
    const params = new URLSearchParams(search)
    const description = params.get('description') || undefined
    return {kind: 'repoDetails', owner, name, description}
  }
  if (path === '/list') {
    return {kind: 'list'}
  }
  return {kind: 'home'}
}
