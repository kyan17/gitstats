import {useEffect, useMemo, useState} from 'react'
import type {Contributor, CommitStats} from './Types.ts'
import {fetchContributors, fetchCommitStats} from './Api.ts'

type Props = {
  owner: string
  name: string
  description?: string
  onBack: () => void
}

export function RepoDetailsView({owner, name, description, onBack}: Props) {
  const [contributors, setContributors] = useState<Contributor[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedLogin, setSelectedLogin] = useState<string | null>(null)

  const [commitStats, setCommitStats] = useState<CommitStats | null>(null)
  const [commitStatsLoading, setCommitStatsLoading] = useState(false)
  const [commitStatsError, setCommitStatsError] = useState<string | null>(null)

  const title = useMemo(() => `${owner}/${name}`, [owner, name])

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await fetchContributors(owner, name)
        setContributors(data)
        if (data && data.length > 0) {
          setSelectedLogin(data[0].login)
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Failed to load contributors')
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [owner, name])

  // Load commit stats whenever selected contributor changes
  useEffect(() => {
    const login = selectedLogin
    if (!login) {
      setCommitStats(null)
      return
    }
    const loadStats = async () => {
      setCommitStatsLoading(true)
      setCommitStatsError(null)
      try {
        const stats = await fetchCommitStats(owner, name, login)
        setCommitStats(stats)
      } catch (e) {
        setCommitStats(null)
        setCommitStatsError(
            e instanceof Error ? e.message : 'Failed to load commit stats',
        )
      } finally {
        setCommitStatsLoading(false)
      }
    }
    void loadStats()
  }, [owner, name, selectedLogin])

  const selectedContributor =
      contributors?.find((c) => c.login === selectedLogin) ?? null

  return (
      <section className="section">
        <button type="button" className="secondary" onClick={onBack}>
          ← Back to repositories
        </button>

        <h2 style={{marginTop: '1rem'}}>{title}</h2>
        {description && <p className="muted">{description}</p>}

        {loading && <p className="muted">Loading contributors...</p>}
        {error && !loading && <p className="error">Error: {error}</p>}

        {!loading && !error && contributors && contributors.length === 0 && (
            <p className="muted">No contributors found.</p>
        )}

        {!loading && !error && contributors && contributors.length > 0 && (
            <div className="two-column">
              {/* Left: contributor list as clickable boxes */}
              <div className="column section">
                <div className="section-head">
                  <h3>Contributors</h3>
                </div>
                <div className="grid cards">
                  {contributors.map((c) => {
                    const isSelected = c.login === selectedLogin
                    return (
                        <article
                            key={c.login}
                            className={`card contributor-card ${
                                isSelected ? 'contributor-card-selected' : ''
                            }`}
                            onClick={() => setSelectedLogin(c.login)}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter' || e.key === ' ') {
                                e.preventDefault()
                                setSelectedLogin(c.login)
                              }
                            }}
                        >
                          <div className="card-header">
                            <div
                                style={{
                                  display: 'flex',
                                  alignItems: 'center',
                                  gap: '0.5rem',
                                }}
                            >
                              <img
                                  src={c.avatarUrl}
                                  alt=""
                                  style={{
                                    width: 32,
                                    height: 32,
                                    borderRadius: '50%',
                                    border: '1px solid #cbd5e1',
                                    objectFit: 'cover',
                                    backgroundColor: '#e5e7eb',
                                  }}
                                  onError={(e) => {
                                    console.warn(
                                        '[RepoDetailsView] avatar failed to load for',
                                        c.login,
                                        'url=',
                                        c.avatarUrl,
                                    )
                                    e.currentTarget.style.visibility = 'hidden'
                                  }}
                              />
                              <h3>{c.login}</h3>
                            </div>
                            <span className="pill info">
                        {c.contributions} commits
                      </span>
                          </div>
                          <div className="card-actions">
                            <a
                                className="secondary"
                                href={c.htmlUrl}
                                target="_blank"
                                rel="noreferrer"
                            >
                              View on GitHub
                            </a>
                          </div>
                        </article>
                    )
                  })}
                </div>
              </div>

              {/* Right: per-contributor metrics */}
              <div className="column section">
                {selectedContributor ? (
                    <>
                      <div className="section-head">
                        <h3>Contributor metrics</h3>
                        <span className="pill info">{selectedContributor.login}</span>
                      </div>

                      <p className="muted" style={{marginTop: 0}}>
                        You are viewing metrics for this contributor in the selected
                        repository.
                      </p>

                      {commitStatsLoading && (
                          <p className="muted">Loading commit stats...</p>
                      )}
                      {commitStatsError && !commitStatsLoading && (
                          <p className="error">Error: {commitStatsError}</p>
                      )}
                      
                      {!commitStatsLoading && !commitStatsError && commitStats && (
                          <ul className="list">
                            <li>
                              <div>
                                <p className="list-title">All-time total commits</p>
                                <p className="list-detail">
                                  Number of commits authored in this repository since its creation.
                                </p>
                              </div>
                              <strong>{commitStats.allTimeTotalCommits}</strong>
                            </li>
                            <li>
                              <div>
                                <p className="list-title">All-time average commit size (lines)</p>
                                <p className="list-detail">
                                  Average number of lines added/removed per commit across all history.
                                </p>
                              </div>
                              <strong>{commitStats.allTimeAverageLinesChanged.toFixed(1)}</strong>
                            </li>
                            <li>
                              <div>
                                <p className="list-title">Commits (last 30 days)</p>
                                <p className="list-detail">
                                  Number of commits made in the last 30 days.
                                </p>
                              </div>
                              <strong>{commitStats.periodTotalCommits}</strong>
                            </li>
                            <li>
                              <div>
                                <p className="list-title">Commits last 7 days</p>
                                <p className="list-detail">
                                  Commits authored during the last 7 days.
                                </p>
                              </div>
                              <strong>{commitStats.periodCommitsLastWeek}</strong>
                            </li>
                            <li>
                              <div>
                                <p className="list-title">Average commit size (last 30 days)</p>
                                <p className="list-detail">
                                  Average number of lines added/removed per commit in the last 30 days.
                                </p>
                              </div>
                              <strong>{commitStats.periodAverageLinesChanged.toFixed(1)}</strong>
                            </li>
                          </ul>
                      )}

                      {!commitStatsLoading &&
                          !commitStatsError &&
                          !commitStats && (
                              <p className="muted">
                                No commit statistics available for this contributor.
                              </p>
                          )}
                    </>
                ) : (
                    <p className="muted">Select a contributor to view metrics.</p>
                )}
              </div>
            </div>
        )}

        <div className="section" style={{marginTop: '1rem'}}>
          <div className="section-head">
            <h3>Repository metrics (coming soon)</h3>
          </div>
          <p className="muted">
            Overall statistics for this repository (issues, pull requests, stars,
            and more) will be displayed here in a future version.
          </p>
        </div>
        {loading && <p className="muted">Loading contributors...</p>}
      </section>
  )
}
