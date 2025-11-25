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

  const title = useMemo(() => `${owner}/${name}`, [owner, name])

  useEffect(() => {
    const load = async () => {
      try {
        const data = await fetchContributors(owner, name)
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
        <h2 style={{marginTop: '1rem'}}>{title}</h2>
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
                                style={{width: 32, height: 32, borderRadius: '50%', marginRight: 8}}
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
