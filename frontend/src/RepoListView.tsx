import type {Repo} from './Types.ts'
import {repoDetailsPath, splitFullName} from './Routes.ts'

type Props = {
  repos: Repo[]
  onOpenDetails: (path: string) => void
}

export function RepoListView({repos, onOpenDetails}: Props) {
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
            const {owner: fullOwner, repo: fullRepoName} = splitFullName(repo.fullName)
            const ownerLabel = repo.ownerLogin || fullOwner || 'Unknown'
            const repoNameLabel = repo.name || fullRepoName || `repo-${idx}`
            const detailsPath = repoDetailsPath(repo)
            const key = repo.fullName || `${ownerLabel}/${repoNameLabel}`

            return (
                <article key={key} className="card">
                  <div className="card-header">
                    <h3>
                      {ownerLabel}/{repoNameLabel}
                    </h3>
                    <span className={`pill ${repo.isPrivate ? 'warning' : 'success'}`}>
                  {repo.isPrivate ? 'Private' : 'Public'}
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
                    <a
                        className="secondary"
                        href={repo.url}
                        target="_blank"
                        rel="noreferrer"
                    >
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
