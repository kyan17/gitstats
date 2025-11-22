import { useEffect, useState } from 'react'
import './App.css'

type User = {
  authenticated: boolean
  login?: string
  name?: string
  avatarUrl?: string
}

type Repo = {
  fullName?: string
  name?: string
  url: string
  description: string
  private: boolean
  updatedAt: string
}

const loginUrl = '/oauth2/authorization/github'

const repoDetailsPath = (repo: Repo, fallbackOwner?: string) => {
  const fullName =
    repo.fullName ||
    (fallbackOwner && repo.name ? `${fallbackOwner}/${repo.name}` : repo.name)
  if (!fullName || !fullName.includes('/')) return null
  const [owner, name] = fullName.split('/')
  if (!owner || !name) return null
  const desc = repo.description ? `?description=${encodeURIComponent(repo.description)}` : ''
  return `/repository/${encodeURIComponent(owner)}/${encodeURIComponent(name)}${desc}`
}

const fetchJson = async <T,>(path: string) => {
  const res = await fetch(path, { credentials: 'include' })
  if (!res.ok) throw new Error(`Request failed ${res.status}`)
  return res.json() as Promise<T>
}

function App() {
  const [user, setUser] = useState<User | null>(null)
  const [repos, setRepos] = useState<Repo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        const me = await fetch('/api/me', { credentials: 'include' })
        if (me.status === 401) {
          setLoading(false)
          return
        }
        const u = (await me.json()) as User
        setUser(u)
        const data = await fetchJson<Repo[]>('/api/repositories')
        setRepos(data)
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Failed to load data')
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [])

  const isAuthed = !!user?.authenticated

  return (
    <div className="page">
      <header className="hero">
        <div>
          <p className="eyebrow">GitStats</p>
          <h1>Welcome to your GitHub stats</h1>
          <p className="lede">Sign in with GitHub to view your repositories and navigate to details.</p>
          <div className="actions">
            {!isAuthed ? (
              <a className="primary" href={loginUrl}>
                Sign in with GitHub
              </a>
            ) : (
              <>
                <span className="user-chip">
                  {user?.avatarUrl ? <img src={user.avatarUrl} alt={user?.login ?? ''} /> : null}
                  <span>{user?.login}</span>
                </span>
                <a className="danger" href="/logout">
                  Logout
                </a>
              </>
            )}
          </div>
          <p className="muted">If you just logged out, refresh to clear the session.</p>
        </div>
      </header>

      {loading && <div className="section"><p className="muted">Loading...</p></div>}
      {error && <div className="section error">Error: {error}</div>}

      {isAuthed && !loading && !error && (
        <section className="section">
          <div className="section-head">
            <h2>Your repositories</h2>
          </div>
          {repos.length === 0 ? (
            <p className="muted">No repositories found.</p>
          ) : (
            <div className="grid cards">
              {repos.map((repo) => {
                const fullName = repo.fullName || repo.name || 'Unknown repo'
                const detailsPath = repoDetailsPath(repo, user?.login)
                return (
                  <article key={fullName} className="card">
                    <div className="card-header">
                      <h3>{fullName}</h3>
                      <span className={`pill ${repo.private ? 'warning' : 'success'}`}>
                        {repo.private ? 'Private' : 'Public'}
                      </span>
                    </div>
                    <p className="muted">{repo.description || 'No description'}</p>
                    <div className="card-actions">
                      <a className="secondary" href={detailsPath ?? '#'}>
                        View details
                      </a>
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </section>
      )}
    </div>
  )
}

export default App
