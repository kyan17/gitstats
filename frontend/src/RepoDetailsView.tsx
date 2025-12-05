import {useEffect, useMemo, useState} from 'react'
import type {Contributor, CommitStats, CommitPeriod} from './Types.ts'
import {
  fetchContributors,
  fetchCommitStatsAllTime,
  fetchCommitStatsLastMonth,
  fetchCommitStatsLastWeek,
} from './Api.ts'

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
  const [selectedPeriod, setSelectedPeriod] = useState<CommitPeriod>('ALL_TIME')

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

  // Load commit stats whenever selected contributor OR period changes
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
        let stats: CommitStats
        if (selectedPeriod === 'ALL_TIME') {
          stats = await fetchCommitStatsAllTime(owner, name, login)
        } else if (selectedPeriod === 'LAST_MONTH') {
          stats = await fetchCommitStatsLastMonth(owner, name, login)
        } else {
          stats = await fetchCommitStatsLastWeek(owner, name, login)
        }
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
  }, [owner, name, selectedLogin, selectedPeriod])

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

                      {/* Period selector */}
                      <div className="actions" style={{marginBottom: '1rem'}}>
                        <button
                            type="button"
                            className={`secondary${
                                selectedPeriod === 'ALL_TIME' ? ' disabled' : ''
                            }`}
                            onClick={() => setSelectedPeriod('ALL_TIME')}
                            disabled={selectedPeriod === 'ALL_TIME'}
                        >
                          All time
                        </button>
                        <button
                            type="button"
                            className={`secondary${
                                selectedPeriod === 'LAST_MONTH' ? ' disabled' : ''
                            }`}
                            onClick={() => setSelectedPeriod('LAST_MONTH')}
                            disabled={selectedPeriod === 'LAST_MONTH'}
                        >
                          Last month
                        </button>
                        <button
                            type="button"
                            className={`secondary${
                                selectedPeriod === 'LAST_WEEK' ? ' disabled' : ''
                            }`}
                            onClick={() => setSelectedPeriod('LAST_WEEK')}
                            disabled={selectedPeriod === 'LAST_WEEK'}
                        >
                          Last week
                        </button>
                      </div>

                      {commitStatsLoading && (
                          <p className="muted">Loading metrics...</p>
                      )}
                      {commitStatsError && !commitStatsLoading && (
                          <p className="error">Error: {commitStatsError}</p>
                      )}

                      {!commitStatsLoading && !commitStatsError && commitStats && (
                          <>
                            {/* 1) Commit stats */}
                            <h4>Commit stats</h4>
                            <ul className="list">
                              <li>
                                <div>
                                  <p className="list-title">Number of commits</p>
                                </div>
                                <strong>{commitStats.commitCount}</strong>
                              </li>
                              <li>
                                <div>
                                  <p className="list-title">Average commit size (lines)</p>
                                </div>
                                <strong>{commitStats.avgCommitSizeLines.toFixed(1)}</strong>
                              </li>
                              <li>
                                <div>
                                  <p className="list-title">Lines added</p>
                                </div>
                                <strong>{commitStats.totalLinesAdded}</strong>
                              </li>
                              <li>
                                <div>
                                  <p className="list-title">Lines removed</p>
                                </div>
                                <strong>{commitStats.totalLinesDeleted}</strong>
                              </li>
                              <li>
                                <div>
                                  <p className="list-title">Net lines changed</p>
                                </div>
                                <strong>{commitStats.netLinesChanged}</strong>
                              </li>
                            </ul>

                            {/* 2) Files stats */}
                            <h4>Files stats</h4>
                            <ul className="list">
                              <li>
                                <div>
                                  <p className="list-title">Distinct files touched</p>
                                </div>
                                <strong>{commitStats.distinctFilesTouched}</strong>
                              </li>
                              <li>
                                <div>
                                  <p className="list-title">Top files modified (count)</p>
                                </div>
                                <strong>{commitStats.topFilesModifiedCount}</strong>
                              </li>
                              <li>
                                <div>
                                  <p className="list-title">Main languages (count)</p>
                                </div>
                                <strong>{commitStats.mainLanguagesCount}</strong>
                              </li>
                            </ul>

                            {/* 3) Issues activity */}
                            <h4>Issues activity</h4>
                            <ul className="list">
                              <li>
                                <div>
                                  <p className="list-title">Issues opened</p>
                                </div>
                                <strong>{commitStats.issuesOpened}</strong>
                              </li>
                              <li>
                                <div>
                                  <p className="list-title">Issues closed</p>
                                </div>
                                <strong>{commitStats.issuesClosed}</strong>
                              </li>
                            </ul>

                            {/* 4) Pull requests activity */}
                            <h4>Pull requests activity</h4>
                            <ul className="list">
                              <li>
                                <div>
                                  <p className="list-title">PRs opened</p>
                                </div>
                                <strong>{commitStats.prsOpened}</strong>
                              </li>
                              <li>
                                <div>
                                  <p className="list-title">PRs merged</p>
                                </div>
                                <strong>{commitStats.prsMerged}</strong>
                              </li>
                            </ul>
                          </>
                      )}
                    </>
                ) : (
                    <p className="muted">Select a contributor to view metrics.</p>
                )}
              </div>
            </div>
        )}
      </section>
  )
}
