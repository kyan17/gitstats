import {useEffect, useState, useMemo} from 'react'
import {Bar} from 'react-chartjs-2'
import type {CommitTimeline} from './Types.ts'
import {fetchCommitTimeline} from './Api.ts'

type Props = {
  owner: string
  repo: string
}

type Period = 'day' | 'week' | 'month'

export function CommitTimelineView({owner, repo}: Props) {
  const [timeline, setTimeline] = useState<CommitTimeline | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [period, setPeriod] = useState<Period>('day')

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await fetchCommitTimeline(owner, repo, period)
        setTimeline(data)
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Failed to load commit timeline')
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [owner, repo, period])

  const chartData = useMemo(() => {
    if (!timeline || timeline.points.length === 0) return null

    return {
      labels: timeline.points.map(p => p.label),
      datasets: [
        {
          label: 'Commits',
          data: timeline.points.map(p => p.count),
          backgroundColor: 'rgba(22, 163, 74, 0.7)',
          borderColor: 'rgba(22, 163, 74, 1)',
          borderWidth: 1,
          borderRadius: 3,
        },
      ],
    }
  }, [timeline])

  const totalCommits = useMemo(() => {
    if (!timeline) return 0
    return timeline.points.reduce((sum, p) => sum + p.count, 0)
  }, [timeline])

  const periodLabel = period === 'day' ? 'Last 30 days' : period === 'week' ? 'Last 12 weeks' : 'Last 12 months'

  return (
      <div className="timeline-container">
        <div className="timeline-header">
          <div>
            <h4>Commit Activity</h4>
            <p className="muted timeline-subtitle">{periodLabel} • {totalCommits} commits</p>
          </div>
          <div className="timeline-period-selector">
            <button
                type="button"
                className={`timeline-btn ${period === 'day' ? 'active' : ''}`}
                onClick={() => setPeriod('day')}
            >
              Daily
            </button>
            <button
                type="button"
                className={`timeline-btn ${period === 'week' ? 'active' : ''}`}
                onClick={() => setPeriod('week')}
            >
              Weekly
            </button>
            <button
                type="button"
                className={`timeline-btn ${period === 'month' ? 'active' : ''}`}
                onClick={() => setPeriod('month')}
            >
              Monthly
            </button>
          </div>
        </div>

        {loading && <p className="muted">Loading timeline...</p>}
        {error && !loading && <p className="error">Error: {error}</p>}

        {!loading && !error && chartData && (
            <div className="timeline-chart">
              <Bar
                  data={chartData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                      legend: {display: false},
                      tooltip: {
                        backgroundColor: '#24292f',
                        titleFont: {size: 12},
                        bodyFont: {size: 11},
                        padding: 10,
                        cornerRadius: 6,
                      },
                    },
                    scales: {
                      x: {
                        grid: {display: false},
                        ticks: {
                          font: {size: 10},
                          color: '#656d76',
                          maxRotation: 45,
                          minRotation: 0,
                        },
                      },
                      y: {
                        beginAtZero: true,
                        grid: {color: '#eaeef2'},
                        ticks: {
                          font: {size: 10},
                          color: '#656d76',
                          stepSize: 1,
                        },
                      },
                    },
                  }}
              />
            </div>
        )}

        {!loading && !error && (!chartData || timeline?.points.length === 0) && (
            <p className="muted">No commit data available for this period.</p>
        )}
      </div>
  )
}

