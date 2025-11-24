import { useEffect, useMemo, useState } from 'react'
import './App.css'

type User = {
    authenticated: boolean
    login?: string
    name?: string
    avatarUrl?: string
}

type Repo = {
    name?: string
    fullName?: string
    url: string
    description: string
    private: boolean
    ownerLogin?: string | null
    updatedAt: string
}

type Contributor = {
    login: string
    avatarUrl: string
    htmlUrl: string
    contributions: number
}

const loginUrl = '/oauth2/authorization/github'

const fetchJson = async <T,>(path: string) => {
    const res = await fetch(path, { credentials: 'include' })
    if (!res.ok) {
        const text = await res.text().catch(() => '')
        throw new Error(`Request failed ${res.status}: ${text || path}`)
    }
    return await res.json() as Promise<T>
}

const splitFullName = (fullName?: string): { owner?: string; repo?: string } => {
    if (!fullName) return {}
    const parts = fullName.split('/')
    if (parts.length !== 2) return {}
    return { owner: parts[0], repo: parts[1] }
}

// Build details path from repo using ONLY data sent by backend
const repoDetailsPath = (repo: Repo): string => {
    // 1) Prefer explicit ownerLogin + name
    if (repo.ownerLogin && repo.name) {
        const q = repo.description ? `?description=${encodeURIComponent(repo.description)}` : ''
        return `/repository/${encodeURIComponent(repo.ownerLogin)}/${encodeURIComponent(repo.name)}${q}`
    }

    // 2) Try fullName
    const { owner, repo: repoName } = splitFullName(repo.fullName)
    if (owner && repoName) {
        const q = repo.description ? `?description=${encodeURIComponent(repo.description)}` : ''
        return `/repository/${encodeURIComponent(owner)}/${encodeURIComponent(repoName)}${q}`
    }

    // No valid owner/name \- avoid crashing
    return '#'
}

type Route =
    | { kind: 'home' }
    | { kind: 'repoDetails'; owner: string; name: string; description?: string }

const parseLocation = (): Route => {
    const path = window.location.pathname
    const search = window.location.search

    const repoMatch = path.match(/^\/repository\/([^/]+)\/([^/]+)$/)
    if (repoMatch) {
        const owner = decodeURIComponent(repoMatch[1])
        const name = decodeURIComponent(repoMatch[2])
        const params = new URLSearchParams(search)
        const description = params.get('description') || undefined
        return { kind: 'repoDetails', owner, name, description }
    }

    return { kind: 'home' }
}

function App() {
    const [user, setUser] = useState<User | null>(null)
    const [repos, setRepos] = useState<Repo[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [route, setRoute] = useState<Route>(() => parseLocation())

    useEffect(() => {
        const handler = () => setRoute(parseLocation())
        window.addEventListener('popstate', handler)
        return () => window.removeEventListener('popstate', handler)
    }, [])

    const isAuthed = !!user?.authenticated

    useEffect(() => {
        const load = async () => {
            try {
                const meRes = await fetch('/api/me', { credentials: 'include' })
                if (meRes.status === 401) {
                    setLoading(false)
                    return
                }
                const me = (await meRes.json()) as User
                setUser(me)

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

    const goto = (path: string) => {
        window.history.pushState({}, '', path)
        setRoute(parseLocation())
    }

    return (
        <div className="page">
            <header className="hero">
                <div>
                    <p className="eyebrow">GitStats</p>
                    <h1>GitHub stats</h1>
                    <p className="lede">Sign in with GitHub to view your repositories and contributors.</p>
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
                </div>
            </header>

            {loading && (
                <div className="section">
                    <p className="muted">Loading...</p>
                </div>
            )}
            {error && !loading && <div className="section error">Error: {error}</div>}

            {!loading && !error && isAuthed && (
                <>
                    {route.kind === 'home' && (
                        <RepoListView repos={repos} onOpenDetails={goto} />
                    )}
                    {route.kind === 'repoDetails' && (
                        <RepoDetailsView
                            owner={route.owner}
                            name={route.name}
                            description={route.description}
                            onBack={() => goto('/')}
                        />
                    )}
                </>
            )}
        </div>
    )
}

function RepoListView(props: {
    repos: Repo[]
    onOpenDetails: (path: string) => void
}) {
    const { repos, onOpenDetails } = props

    if (repos.length === 0) {
        return (
            <section className="section">
                <h2>Your repositories</h2>
                <p className="muted">No repositories found.</p>
            </section>
        )
    }

    return (
        <section className="section">
            <div className="section-head">
                <h2>Your repositories</h2>
            </div>
            <div className="grid cards">
                {repos.map((repo, idx) => {
                    const { owner: fullOwner, repo: fullRepoName } = splitFullName(repo.fullName)

                    // Owner label from backend fields only
                    const ownerLabel =
                        repo.ownerLogin ||
                        fullOwner ||
                        'Unknown'

                    const repoNameLabel =
                        repo.name ||
                        fullRepoName ||
                        `repo-${idx}`

                    const detailsPath = repoDetailsPath(repo)
                    const key = repo.fullName || `${ownerLabel}/${repoNameLabel}`

                    return (
                        <article key={key} className="card">
                            <div className="card-header">
                                <h3>
                                    {ownerLabel}/{repoNameLabel}
                                </h3>
                                <span className={`pill ${repo.private ? 'warning' : 'success'}`}>
                                  {repo.private ? 'Private' : 'Public'}
                                </span>
                            </div>
                            <p className="muted">{repo.description || 'No description'}</p>
                            <small className="muted">Last updated: {repo.updatedAt}</small>
                            <div className="card-actions">
                                <button
                                    type="button"
                                    className="secondary"
                                    onClick={() => onOpenDetails(detailsPath)}
                                    disabled={detailsPath === '#'}
                                >
                                    View details
                                </button>
                                <a className="secondary" href={repo.url} target="_blank" rel="noreferrer">
                                    Open on GitHub
                                </a>
                            </div>
                        </article>
                    )
                })}
            </div>
        </section>
    )
}

function RepoDetailsView(props: {
    owner: string
    name: string
    description?: string
    onBack: () => void
}) {
    const { owner, name, description, onBack } = props
    const [contributors, setContributors] = useState<Contributor[] | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const title = useMemo(() => `${owner}/${name}`, [owner, name])

    useEffect(() => {
        const load = async () => {
            try {
                const data = await fetchJson<Contributor[]>(
                    `/api/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(name)}/contributors`,
                )
                setContributors(data)
            } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to load contributors')
            } finally {
                setLoading(false)
            }
        }
        void load()
    }, [owner, name])

    return (
        <section className="section">
            <button type="button" className="secondary" onClick={onBack}>
                ← Back to repositories
            </button>

            <h2 style={{ marginTop: '1rem' }}>{title}</h2>
            {description && <p className="muted">{description}</p>}


            {loading && <p className="muted">Loading contributors...</p>}
            {error && !loading && <p className="error">Error: {error}</p>}

            {!loading && !error && (
                <>
                    {contributors && contributors.length > 0 ? (
                        <ul className="contributors">
                            {contributors.map((c) => (
                                <li key={c.login} className="contributor">
                                    <a href={c.htmlUrl} target="_blank" rel="noreferrer">
                                        <img
                                            src={c.avatarUrl}
                                            alt={c.login}
                                            style={{ width: 32, height: 32, borderRadius: '50%', marginRight: 8 }}
                                        />
                                        <span>{c.login}</span>
                                    </a>
                                    <span className="muted">Contributions: {c.contributions}</span>
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <p className="muted">No contributors found.</p>
                    )}
                </>
            )}
        </section>
    )
}

export default App
