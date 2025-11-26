import {useEffect, useMemo, useState} from 'react'
import type {Contributor} from './Types.ts'
import {fetchContributors} from './Api.ts'

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
  const title = useMemo(() => `${owner}/${name}`, [owner, name])

  useEffect(() => {
    const load = async () => {
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
                            <div style={{display: 'flex', alignItems: 'center', gap: '0.5rem'}}>
                              {/* Avatar is rendered here */}
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

              {/* Right: per-contributor metrics placeholder */}
              <div className="column section">
                {selectedContributor ? (
                    <>
                      <div className="section-head">
                        <h3>Contributor metrics</h3>
                        <span className="pill info">{selectedContributor.login}</span>
                      </div>

                      <p className="muted" style={{marginTop: 0}}>
                        You are viewing metrics for this contributor in the selected repository.
                      </p>

                      <ul className="list">
                        <li>
                          <div>
                            <p className="list-title">Commits</p>
                            <p className="list-detail">
                              Total commits in this repository.
                            </p>
                          </div>
                          <strong>{selectedContributor.contributions}</strong>
                        </li>
                        <li>
                          <div>
                            <p className="list-title">Lines of code</p>
                            <p className="list-detail">
                              Added / removed lines for this contributor.
                            </p>
                          </div>
                          <span className="muted">coming soon</span>
                        </li>
                        <li>
                          <div>
                            <p className="list-title">Issues \& PRs</p>
                            <p className="list-detail">
                              Issues opened/closed, PRs submitted/approved.
                            </p>
                          </div>
                          <span className="muted">coming soon</span>
                        </li>
                        <li>
                          <div>
                            <p className="list-title">Activity profile</p>
                            <p className="list-detail">
                              Commit frequency, active days/hours, test coverage, etc.
                            </p>
                          </div>
                          <span className="muted">coming soon</span>
                        </li>
                      </ul>
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
