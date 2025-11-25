// typescript
// frontend/src/PostLogoutView.tsx
import type {FC} from 'react'

type Props = {
  isStillLoggedIn: boolean
  onBack: () => void
}

export const PostLogoutView: FC<Props> = ({isStillLoggedIn, onBack}) => {
  return (
      <main className="section">
        <h2>Manage your GitHub session</h2>

        <p className="lede">
          You have left the current GitStats session.
        </p>

        <p className="muted">
          You might still be logged in on GitHub in this browser. You can either go
          back to GitStats or open GitHub&apos;s logout page to disconnect your GitHub account.
        </p>

        <div className="actions" style={{marginTop: '1.5rem'}}>
          <button type="button" onClick={onBack}>
            Back
          </button>

          <a
              className="danger"
              href="/github-logout"
              target="_blank"
              rel="noreferrer"
              style={{marginLeft: '0.75rem'}}
          >
            Logout GitHub account
          </a>
        </div>

        {!isStillLoggedIn && (
            <p className="muted" style={{marginTop: '1rem'}}>
              You are no longer authenticated in GitStats. If you log out from GitHub as well,
              you will need to log in again when you return.
            </p>
        )}
      </main>
  )
}
