import { useEffect, useState, useMemo } from "react";
import { Bar } from "react-chartjs-2";
import type { PullRequestsTimeline, Period } from "../common/Types.ts";
import { fetchPullRequestsTimeline } from "../common/Api.ts";

type Props = {
  owner: string;
  repo: string;
};

export function PullRequestsTimelineView({ owner, repo }: Props) {
  const [timeline, setTimeline] = useState<PullRequestsTimeline | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [period, setPeriod] = useState<Period>("week");

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchPullRequestsTimeline(owner, repo, period);
        setTimeline(data);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Failed to load PR timeline");
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [owner, repo, period]);

  const chartData = useMemo(() => {
    if (!timeline || timeline.points.length === 0) return null;

    return {
      labels: timeline.points.map((p) => p.label),
      datasets: [
        {
          label: "Opened",
          data: timeline.points.map((p) => p.opened),
          backgroundColor: "rgba(59, 130, 246, 0.7)",
          borderColor: "rgba(59, 130, 246, 1)",
          borderWidth: 1,
          borderRadius: 2,
        },
        {
          label: "Merged",
          data: timeline.points.map((p) => p.merged),
          backgroundColor: "rgba(22, 163, 74, 0.7)",
          borderColor: "rgba(22, 163, 74, 1)",
          borderWidth: 1,
          borderRadius: 2,
        },
      ],
    };
  }, [timeline]);

  const periodLabel =
    period === "day"
      ? "Last 30 days"
      : period === "week"
        ? "Last 12 weeks"
        : "Last 12 months";

  return (
    <div className="timeline-container">
      <div className="timeline-header">
        <div>
          <h4>Pull Requests</h4>
          <p className="muted timeline-subtitle">
            {periodLabel} •{" "}
            <span style={{ color: "#3b82f6" }}>{timeline?.totalOpen ?? 0} open</span> /{" "}
            <span style={{ color: "#16a34a" }}>{timeline?.totalMerged ?? 0} merged</span>
          </p>
        </div>
        <div className="timeline-period-selector">
          <button
            type="button"
            className={`timeline-btn ${period === "day" ? "active" : ""}`}
            onClick={() => setPeriod("day")}
          >
            Daily
          </button>
          <button
            type="button"
            className={`timeline-btn ${period === "week" ? "active" : ""}`}
            onClick={() => setPeriod("week")}
          >
            Weekly
          </button>
          <button
            type="button"
            className={`timeline-btn ${period === "month" ? "active" : ""}`}
            onClick={() => setPeriod("month")}
          >
            Monthly
          </button>
        </div>
      </div>

      {loading && <p className="muted">Loading...</p>}
      {error && !loading && <p className="error">Error: {error}</p>}

      {!loading && !error && chartData && (
        <div className="timeline-chart">
          <Bar
            data={chartData}
            options={{
              responsive: true,
              maintainAspectRatio: false,
              plugins: {
                legend: {
                  display: true,
                  position: "top",
                  align: "end",
                  labels: {
                    boxWidth: 12,
                    boxHeight: 12,
                    font: { size: 11 },
                    padding: 15,
                  },
                },
                tooltip: {
                  backgroundColor: "#24292f",
                  titleFont: { size: 12 },
                  bodyFont: { size: 11 },
                  padding: 10,
                  cornerRadius: 6,
                },
              },
              scales: {
                x: {
                  grid: { display: false },
                  ticks: {
                    font: { size: 10 },
                    color: "#656d76",
                    maxRotation: 45,
                    minRotation: 0,
                  },
                },
                y: {
                  beginAtZero: true,
                  grid: { color: "#eaeef2" },
                  ticks: {
                    font: { size: 10 },
                    color: "#656d76",
                    stepSize: 1,
                  },
                },
              },
            }}
          />
        </div>
      )}

      {!loading && !error && (!chartData || timeline?.points.length === 0) && (
        <p className="muted">No PR data available.</p>
      )}
    </div>
  );
}
