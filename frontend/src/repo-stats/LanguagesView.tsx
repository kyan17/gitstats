import {useEffect, useState} from 'react'
import type {LanguageStats} from '../common/Types.ts'
import {fetchLanguages} from '../common/Api.ts'

type Props = {
  owner: string
  repo: string
}

export function LanguagesView({owner, repo}: Props) {
  const [languages, setLanguages] = useState<LanguageStats[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await fetchLanguages(owner, repo)
        setLanguages(data)
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Failed to load languages')
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [owner, repo])

  if (loading) {
    return <p className="muted">Loading languages...</p>
  }

  if (error) {
    return <p className="error">Error: {error}</p>
  }

  if (languages.length === 0) {
    return <p className="muted">No language data available.</p>
  }

  return (
      <div className="languages-container">
        <h4>Languages</h4>

        {/* Progress bar */}
        <div className="languages-bar">
          {languages.map((lang) => (
              <div
                  key={lang.name}
                  className="language-segment"
                  style={{
                    width: `${lang.percentage}%`,
                    backgroundColor: lang.color,
                  }}
                  title={`${lang.name}: ${lang.percentage}%`}
              />
          ))}
        </div>

        {/* Language list */}
        <div className="languages-list">
          {languages.map((lang) => (
              <div key={lang.name} className="language-item">
                <span
                    className="language-dot"
                    style={{backgroundColor: lang.color}}
                />
                <span className="language-name">{lang.name}</span>
                <span className="language-percent">{lang.percentage}%</span>
              </div>
          ))}
        </div>
      </div>
  )
}
