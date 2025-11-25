import {useEffect, useState} from 'react'
import './App.css'
import type {Repo, Route, User} from './Types.ts'
import {fetchMe, fetchRepositories, loginUrl} from './Api.ts'
import {parseLocation} from './Routes.ts'
import {RepoListView} from './RepoListView.tsx'
import {RepoDetailsView} from './RepoDetailsView.tsx'

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
        const me = await fetchMe()
        if (!me.authenticated) {
          setLoading(false)
          return
        }
        setUser(me)
        const data = await fetchRepositories()
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
                  {user?.avatarUrl ? <img src={user.avatarUrl} alt={user?.login ?? ''}/> : null}
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
              {route.kind === 'home' && <RepoListView repos={repos} onOpenDetails={goto}/>}
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

export default App
