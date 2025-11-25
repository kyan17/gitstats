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

export function parseLocation(): Route {
  const url = new URL(window.location.href);

  const pathname = url.pathname;
  const state = url.searchParams.get('state');

  if (state === 'post-logout') {
    return { kind: 'postLogout' };
  }

  if (pathname === '/' || pathname === '/index.html') {
    return { kind: 'home' };
  }

  if (pathname === '/list') {
    return { kind: 'list' };
  }

  const repoMatch = pathname.match(/^\/repository\/([^/]+)\/([^/]+)/);
  if (repoMatch) {
    const [, owner, name] = repoMatch;
    const description = url.searchParams.get('description') ?? '';
    return { kind: 'repoDetails', owner, name, description };
  }

  return { kind: 'home' };
}
