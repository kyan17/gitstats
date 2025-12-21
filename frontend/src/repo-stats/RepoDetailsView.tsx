import { useEffect, useMemo, useRef, useState } from "react";
import { Pie } from "react-chartjs-2";
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
} from "chart.js";
import type {
  Contributor,
  CommitStats,
  CommitPeriod,
  ContributionStats,
  WorkTypeStats,
} from "../common/Types.ts";
import {
  fetchContributors,
  fetchCommitStats,
  fetchContributionStats,
  fetchWorkTypeStats,
} from "../common/Api.ts";
import { NetworkView } from "./NetworkView.tsx";
import { LanguagesView } from "./LanguagesView.tsx";
import { CommitTimelineView } from "./CommitTimelineView.tsx";
import { IssuesTimelineView } from "./IssuesTimelineView.tsx";
import { PullRequestsTimelineView } from "./PullRequestsTimelineView.tsx";
import "../css/App.css";

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Tooltip, Legend);

type Props = {
  owner: string;
  name: string;
  description?: string;
  onBack: () => void;
};

export function RepoDetailsView({ owner, name, description, onBack }: Props) {
  const [contributors, setContributors] = useState<Contributor[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedLogin, setSelectedLogin] = useState<string | null>(null);
  const [selectedPeriod, setSelectedPeriod] = useState<CommitPeriod>("ALL_TIME");

  const [commitStats, setCommitStats] = useState<CommitStats | null>(null);
  const [commitStatsLoading, setCommitStatsLoading] = useState(false);
  const [commitStatsError, setCommitStatsError] = useState<string | null>(null);
  const commitStatsRequestRef = useRef(0);

  const [contributionStats, setContributionStats] = useState<ContributionStats | null>(
    null,
  );
  const [contributionStatsLoading, setContributionStatsLoading] = useState(false);
  const [workTypeStats, setWorkTypeStats] = useState<WorkTypeStats | null>(null);
  const [workTypeStatsLoading, setWorkTypeStatsLoading] = useState(false);
  const title = useMemo(() => `${owner}/${name}`, [owner, name]);

  const commitShareData = useMemo(() => {
    if (!contributors || contributors.length === 0) return null;
    const top5 = contributors.slice(0, 5);
    const labels = top5.map((c) => c.login);
    const commits = top5.map((c) => c.contributions);
    const colors = ["#60a5fa", "#a78bfa", "#34d399", "#fbbf24", "#f87171"];
    return {
      labels,
      datasets: [
        {
          data: commits,
          backgroundColor: colors,
          borderWidth: 0,
        },
      ],
    };
  }, [contributors]);

  const contributionPieData = useMemo(() => {
    if (!contributionStats || contributionStats.slices.length === 0) return null;
    const labels = contributionStats.slices.map((s) => s.login);
    const data = contributionStats.slices.map((s) => s.score);
    const colors = ["#60a5fa", "#a78bfa", "#34d399", "#fbbf24", "#f97316", "#f87171"];
    return {
      labels,
      datasets: [
        {
          data,
          backgroundColor: colors,
          borderWidth: 0,
        },
      ],
    };
  }, [contributionStats]);

  const workTypePieData = useMemo(() => {
    if (!workTypeStats) return null;
    const {
      featureCommits,
      bugfixCommits,
      refactorCommits,
      testCommits,
      documentationCommits,
    } = workTypeStats;
    const data = [
      featureCommits,
      bugfixCommits,
      refactorCommits,
      testCommits,
      documentationCommits,
    ];
    const total = data.reduce((sum, v) => sum + v, 0);
    if (total === 0) return null;
    const labels = ["Features", "Bugfixes", "Refactors", "Tests", "Docs"];
    const colors = ["#60a5fa", "#f97316", "#a78bfa", "#22c55e", "#facc15"];
    return {
      labels,
      datasets: [
        {
          data,
          backgroundColor: colors,
          borderWidth: 0,
        },
      ],
    };
  }, [workTypeStats]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchContributors(owner, name);
        setContributors(data);
        if (data && data.length > 0) {
          setSelectedLogin(data[0].login);
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : "Failed to load contributors");
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [owner, name]);

  useEffect(() => {
    const login = selectedLogin;
    if (!login) {
      setCommitStats(null);
      setCommitStatsError(null);
      return;
    }
    const loadStats = async () => {
      setCommitStats(null);
      setCommitStatsLoading(true);
      setCommitStatsError(null);
      const requestId = ++commitStatsRequestRef.current;
      try {
        const stats = await fetchCommitStats(owner, name, login, selectedPeriod);
        if (requestId === commitStatsRequestRef.current) {
          setCommitStats(stats);
        }
      } catch (e) {
        if (requestId === commitStatsRequestRef.current) {
          setCommitStats(null);
          setCommitStatsError(
            e instanceof Error ? e.message : "Failed to load commit stats",
          );
        }
      } finally {
        if (requestId === commitStatsRequestRef.current) {
          setCommitStatsLoading(false);
        }
      }
    };
    void loadStats();
  }, [owner, name, selectedLogin, selectedPeriod]);

  useEffect(() => {
    const loadContribution = async () => {
      setContributionStatsLoading(true);
      try {
        const stats = await fetchContributionStats(owner, name, "ALL_TIME");
        setContributionStats(stats);
      } catch {
        setContributionStats(null);
      } finally {
        setContributionStatsLoading(false);
      }
    };
    void loadContribution();
  }, [owner, name]);

  useEffect(() => {
    const loadWorkTypes = async () => {
      setWorkTypeStatsLoading(true);
      try {
        const stats = await fetchWorkTypeStats(owner, name, "ALL_TIME");
        setWorkTypeStats(stats);
      } catch {
        setWorkTypeStats(null);
      } finally {
        setWorkTypeStatsLoading(false);
      }
    };
    void loadWorkTypes();
  }, [owner, name]);

  const selectedContributor =
    contributors?.find((c) => c.login === selectedLogin) ?? null;
  const totalCommits = contributors?.reduce((sum, c) => sum + c.contributions, 0) ?? 0;

  return (
    <section className="section repo-details">
      <div className="repo-header">
        <button type="button" className="secondary btn-sm" onClick={onBack}>
          ← Back
        </button>
        <div className="repo-title-area">
          <h2>{title}</h2>
          {description && <p className="muted">{description}</p>}
        </div>
      </div>

      {loading && <p className="muted">Loading...</p>}
      {error && !loading && <p className="error">Error: {error}</p>}

      {!loading && !error && contributors && contributors.length > 0 && (
        <>
          <div className="stats-row">
            <div className="stat-card">
              <span className="stat-value">{contributors.length}</span>
              <span className="stat-label">Contributors</span>
            </div>
            <div className="stat-card">
              <span className="stat-value">{totalCommits}</span>
              <span className="stat-label">Total Commits</span>
            </div>
            <div className="stat-card-wide">
              <LanguagesView owner={owner} repo={name} />
            </div>
          </div>

          <div className="three-column" style={{ marginTop: "1rem" }}>
            <div className="column section">
              <CommitTimelineView owner={owner} repo={name} />
            </div>
            <div className="column section">
              <IssuesTimelineView owner={owner} repo={name} />
            </div>
            <div className="column section">
              <PullRequestsTimelineView owner={owner} repo={name} />
            </div>
          </div>

          <div className="two-column pie-row" style={{ marginTop: "1rem", gap: "1rem" }}>
            <div className="column section" style={{ flex: 2 }}>
              <div className="section-head">
                <h3>Repository Graph</h3>
              </div>
              <NetworkView owner={owner} repo={name} />
            </div>
            <div className="column section pie-charts-section" style={{ flex: 1 }}>
              <div className="pie-chart-container">
                <div style={{ width: "100%", height: "100%" }}>
                  <h4 style={{ margin: "0 0 0.5rem" }}>Global Contribution Score</h4>
                  {contributionStatsLoading ? (
                    <div style={{ textAlign: "center" }}>
                      <img
                        src="/loading.gif"
                        alt="Loading..."
                        style={{ width: 96, height: 96 }}
                      />
                    </div>
                  ) : contributionPieData ? (
                    <div style={{ width: "100%", height: "100%", margin: "0 auto" }}>
                      <Pie
                        data={contributionPieData}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          plugins: {
                            legend: {
                              position: "bottom",
                              labels: { boxWidth: 10, font: { size: 10 } },
                            },
                          },
                        }}
                      />
                    </div>
                  ) : null}
                </div>
              </div>
              <div className="pie-chart-container">
                <div style={{ width: "100%" }}>
                  <h4 style={{ margin: "0 0 0.5rem" }}>Commit Share</h4>
                  {loading ? (
                    <div style={{ textAlign: "center" }}>
                      <img
                        src="/loading.gif"
                        alt="Loading..."
                        style={{ width: 96, height: 96 }}
                      />
                    </div>
                  ) : commitShareData ? (
                    <div style={{ width: "100%", height: "100%", margin: "0 auto" }}>
                      <Pie
                        data={commitShareData}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          plugins: {
                            legend: {
                              position: "bottom",
                              labels: { boxWidth: 10, font: { size: 10 } },
                            },
                          },
                        }}
                      />
                    </div>
                  ) : null}
                </div>
              </div>
              <div className="pie-chart-container">
                <div style={{ width: "100%" }}>
                  <h4 style={{ margin: "0 0 0.5rem" }}>Work Types</h4>
                  {workTypeStatsLoading ? (
                    <div style={{ textAlign: "center" }}>
                      <img
                        src="/loading.gif"
                        alt="Loading..."
                        style={{ width: 96, height: 96 }}
                      />
                    </div>
                  ) : workTypePieData ? (
                    <div style={{ width: "100%", height: "100%", margin: "0 auto" }}>
                      <Pie
                        data={workTypePieData}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          plugins: {
                            legend: {
                              position: "bottom",
                              labels: { boxWidth: 10, font: { size: 10 } },
                            },
                          },
                        }}
                      />
                    </div>
                  ) : null}
                </div>
              </div>
            </div>
          </div>

          <div className="two-column" style={{ marginTop: "1rem", gap: "1rem" }}>
            <div className="column section">
              <div className="section-head">
                <h3>Contributors</h3>
              </div>
              <div className="contributors-compact">
                {contributors.map((c) => {
                  const isSelected = c.login === selectedLogin;
                  return (
                    <div
                      key={c.login}
                      className={`contributor-row ${isSelected ? "selected" : ""}`}
                      onClick={() => setSelectedLogin(c.login)}
                    >
                      <img src={c.avatarUrl} alt="" className="contributor-avatar" />
                      <span className="contributor-name">{c.login}</span>
                      <span className="contributor-commits">{c.contributions}</span>
                    </div>
                  );
                })}
              </div>
            </div>
            <div className="column section">
              {selectedContributor ? (
                <>
                  <div className="section-head">
                    <h3>Metrics</h3>
                    <span className="pill info">{selectedContributor.login}</span>
                  </div>

                  <div className="period-selector">
                    {(["ALL_TIME", "LAST_MONTH", "LAST_WEEK"] as CommitPeriod[]).map(
                      (p) => (
                        <button
                          key={p}
                          type="button"
                          className={`timeline-btn ${selectedPeriod === p ? "active" : ""}`}
                          onClick={() => setSelectedPeriod(p)}
                        >
                          {p === "ALL_TIME"
                            ? "All"
                            : p === "LAST_MONTH"
                              ? "Month"
                              : "Week"}
                        </button>
                      ),
                    )}
                  </div>

                  {commitStatsLoading && <p className="muted">Loading...</p>}
                  {commitStatsError && !commitStatsLoading && (
                    <p className="error">{commitStatsError}</p>
                  )}

                  {!commitStatsLoading && !commitStatsError && commitStats && (
                    <div className="metrics-grid">
                      <div className="metric-item">
                        <span className="metric-value">{commitStats.commitCount}</span>
                        <span className="metric-label">Commits</span>
                      </div>
                      <div className="metric-item">
                        <span className="metric-value">
                          +{commitStats.totalLinesAdded}
                        </span>
                        <span className="metric-label">Lines Added</span>
                      </div>
                      <div className="metric-item">
                        <span className="metric-value">
                          -{commitStats.totalLinesDeleted}
                        </span>
                        <span className="metric-label">Lines Removed</span>
                      </div>
                      <div className="metric-item">
                        <span className="metric-value">
                          {commitStats.distinctFilesTouched}
                        </span>
                        <span className="metric-label">Files Touched</span>
                      </div>
                      <div className="metric-item">
                        <span className="metric-value">
                          {commitStats.issuesOpen}/{commitStats.issuesClosed}
                        </span>
                        <span className="metric-label">Issues Open/Closed</span>
                      </div>
                      <div className="metric-item">
                        <span className="metric-value">
                          {commitStats.prsOpen}/{commitStats.prsMerged}
                        </span>
                        <span className="metric-label">PRs Open/Merged</span>
                      </div>
                    </div>
                  )}
                </>
              ) : (
                <p className="muted">Select a contributor</p>
              )}
            </div>
          </div>
        </>
      )}
      {!loading && !error && contributors && contributors.length === 0 && (
        <p className="muted">No contributors found.</p>
      )}
    </section>
  );
}
