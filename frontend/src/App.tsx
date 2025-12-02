import {useEffect, useState} from 'react'
import './App.css'
import type {Repo, Route, User} from './Types'
import {fetchMe, fetchRepositories, loginUrl} from './Api'
import {parseLocation} from './Routes'
import {RepoListView} from './RepoListView'
import {RepoDetailsView} from './RepoDetailsView'
import {PostLogoutView} from './PostLogoutView'

function App() {
  const [user, setUser] = useState<User | null>(null)
  const [repos, setRepos] = useState<Repo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [route, setRoute] = useState<Route>(() => parseLocation())
  const isAuthed = !!user?.authenticated
  const isHome = route.kind === 'home'
  const isList = route.kind === 'list'

  const goto = (path: string) => {
    window.history.pushState({}, '', path)
    setRoute(parseLocation())
  }

  useEffect(() => {
    const handler = () => setRoute(parseLocation())
    window.addEventListener('popstate', handler)
    return () => window.removeEventListener('popstate', handler)
  }, [])

  useEffect(() => {
    if (route.kind === 'postLogout') {
      const checkStatus = async () => {
        setLoading(true)
        setError(null)
        try {
          const me = await fetchMe()
          setUser(me)
        } catch (e) {
          console.info(e)
          setUser({authenticated: false})
        } finally {
          setLoading(false)
        }
      }
      void checkStatus()
      return
    }
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const me = await fetchMe()
        if (!me.authenticated) {
          setUser({authenticated: false})
          setRepos([])
          setLoading(false)
          return
        }
        setUser(me)
        if (route.kind === 'list') {
          const data = await fetchRepositories()
          setRepos(data)
        }
      } catch (e) {
        console.info('[App] unexpected error while loading /me', e)
        setUser({authenticated: false})
        setRepos([])
        setError(e instanceof Error ? e.message : 'Failed to load data')
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [route.kind])

  useEffect(() => {
    const loadReposIfNeeded = async () => {
      if (!isAuthed || route.kind !== 'list') return
      setLoading(true)
      setError(null)
      try {
        const data = await fetchRepositories()
        setRepos(data)
      } catch (e) {
        setUser({authenticated: false})
        setRepos([])
        goto('/')
        setError(e instanceof Error ? e.message : 'Failed to load repositories')
      } finally {
        setLoading(false)
      }
    }
    void loadReposIfNeeded()
  }, [route.kind, isAuthed])

  const showUserHeader = isAuthed && isList
  const showHeader = isHome || isList

  const handleBackFromPostLogout = () => {
    if (user?.authenticated) {
      goto('/list')
    } else {
      goto('/')
    }
  }

  // Different header content for home vs. repo list
  const headerTitle = 'GitHub stats'
  const headerLede = isHome
      ? 'Sign in with GitHub to view your repositories details'
      : 'Browse your repositories and inspect contributor activity and general statistics';

  return (
      <div className="page">
        {showHeader && (
            <header className="hero">
              <div>
                <p className="eyebrow">GitStats</p>
                <h1>{headerTitle}</h1>
                <p className="lede">{headerLede}</p>
                <div className="actions">
                  {showUserHeader && user && user.login && user.avatarUrl ? (
                      <>
                  <span className="user-chip">
                    <img alt={user.login} src={user.avatarUrl} />
                    <span>{user.login}</span>
                  </span>
                        <a className="danger" href="/logout-app">
                          Logout
                        </a>
                      </>
                  ) : (
                      isHome && (
                          <a className="primary" href={loginUrl}>
                            Login with GitHub
                          </a>
                      )
                  )}
                </div>
              </div>
            </header>
        )}

        {loading && (
            <div className="section">
              <p className="muted">Loading...</p>
            </div>
        )}
        {error && !loading && <div className="section error">Error: {error}</div>}

        {!loading && !error && route.kind === 'postLogout' && (
            <PostLogoutView
                isStillLoggedIn={!!user?.authenticated}
                onBack={handleBackFromPostLogout}
            />
        )}

        {!loading && !error && isAuthed && route.kind === 'list' && (
            <RepoListView repos={repos} onOpenDetails={goto} />
        )}

        {!loading && !error && isAuthed && route.kind === 'repoDetails' && (
            <RepoDetailsView
                owner={route.owner}
                name={route.name}
                description={route.description}
                onBack={() => goto('/list')}
            />
        )}
      </div>
  )
}

export default App
