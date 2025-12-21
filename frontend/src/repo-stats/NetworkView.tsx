import { useEffect, useMemo, useState, useRef } from "react";
import type { NetworkGraph } from "../common/Types.ts";
import { fetchNetworkGraph } from "../common/Api.ts";
import "../css/App.css";

type Props = {
  owner: string;
  repo: string;
};

// Colors for different branches (matching GitHub style)
const BRANCH_COLORS = [
  "#1a7f37", // green (main/master)
  "#0969da", // blue
  "#8250df", // purple
  "#bf3989", // pink
  "#cf222e", // red
  "#bc4c00", // orange
];

type CommitPosition = {
  sha: string;
  shortSha: string;
  message: string;
  authorLogin: string;
  date: string;
  dateLabel: string;
  x: number;
  y: number;
  color: string;
  branchName: string;
  parentPositions: { x: number; y: number; color: string }[];
  branches: string[];
};

type TooltipData = {
  x: number;
  y: number;
  sha: string;
  message: string;
  author: string;
  date: string;
  branches: string[];
} | null;

export function NetworkView({ owner, repo }: Props) {
  const [network, setNetwork] = useState<NetworkGraph | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tooltip, setTooltip] = useState<TooltipData>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchNetworkGraph(owner, repo, 50);
        setNetwork(data);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Failed to load network graph");
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [owner, repo]);

  const { positions, graphWidth, graphHeight, branchLabels, timelineLabels } =
    useMemo(() => {
      if (!network || network.commits.length === 0) {
        return {
          positions: [],
          graphWidth: 200,
          graphHeight: 200,
          branchLabels: [],
          timelineLabels: [],
        };
      }

      const commits = network.commits;
      const commitMap = new Map<string, number>();
      commits.forEach((c, i) => commitMap.set(c.sha, i));

      const branchLanes = new Map<string, number>();
      let laneIdx = 0;

      network.branches.forEach((branch) => {
        if (branch.isDefault) {
          branchLanes.set(branch.name, laneIdx++);
        }
      });
      network.branches.forEach((branch) => {
        if (!branchLanes.has(branch.name)) {
          branchLanes.set(branch.name, laneIdx++);
        }
      });

      const commitLanes = new Map<string, number>();
      const commitBranchName = new Map<string, string>();

      network.branches.forEach((branch) => {
        const lane = branchLanes.get(branch.name) ?? 0;
        let currentSha = branch.sha;
        const visited = new Set<string>();
        while (currentSha && !visited.has(currentSha)) {
          visited.add(currentSha);
          if (!commitLanes.has(currentSha)) {
            commitLanes.set(currentSha, lane);
            commitBranchName.set(currentSha, branch.name);
          }
          const commit = commits.find((c) => c.sha === currentSha);
          if (commit && commit.parentShas.length > 0) {
            currentSha = commit.parentShas[0];
          } else {
            break;
          }
        }
      });

      commits.forEach((c) => {
        if (!commitLanes.has(c.sha)) {
          commitLanes.set(c.sha, 0);
          commitBranchName.set(c.sha, network.defaultBranch);
        }
      });

      const nodeSpacingX = 28;
      const nodeSpacingY = 28;
      const leftPadding = 20;
      const topPadding = 25;

      const positionedCommits: CommitPosition[] = commits.map((commit, idx) => {
        const lane = commitLanes.get(commit.sha) ?? 0;
        const branchName = commitBranchName.get(commit.sha) ?? network.defaultBranch;
        const branchIdx = network.branches.findIndex((b) => b.name === branchName);
        const color =
          BRANCH_COLORS[branchIdx >= 0 ? branchIdx % BRANCH_COLORS.length : 0];

        const x = leftPadding + lane * nodeSpacingX;
        const y = topPadding + idx * nodeSpacingY;

        const d = new Date(commit.date);
        const dateLabel = d.toLocaleDateString("en-US", {
          month: "short",
          day: "numeric",
        });

        const parentPositions = commit.parentShas
          .map((parentSha) => {
            const parentIdx = commitMap.get(parentSha);
            if (parentIdx === undefined) return null;
            const parentLane = commitLanes.get(parentSha) ?? 0;
            const parentBranchName =
              commitBranchName.get(parentSha) ?? network.defaultBranch;
            const parentBranchIdx = network.branches.findIndex(
              (b) => b.name === parentBranchName,
            );
            const parentColor =
              BRANCH_COLORS[
                parentBranchIdx >= 0 ? parentBranchIdx % BRANCH_COLORS.length : 0
              ];
            return {
              x: leftPadding + parentLane * nodeSpacingX,
              y: topPadding + parentIdx * nodeSpacingY,
              color: parentColor,
            };
          })
          .filter((p): p is { x: number; y: number; color: string } => p !== null);

        return {
          sha: commit.sha,
          shortSha: commit.shortSha,
          message: commit.message,
          authorLogin: commit.authorLogin,
          date: commit.date,
          dateLabel,
          x,
          y,
          color,
          branchName,
          parentPositions,
          branches: commit.branches,
        };
      });

      const timeline: { label: string; y: number }[] = [];
      let lastLabel = "";
      positionedCommits.forEach((pos) => {
        if (pos.dateLabel !== lastLabel) {
          timeline.push({ label: pos.dateLabel, y: pos.y });
          lastLabel = pos.dateLabel;
        }
      });

      const branchEndLabels = network.branches.map((branch, bIdx) => ({
        name: branch.name,
        color: BRANCH_COLORS[bIdx % BRANCH_COLORS.length],
        isDefault: branch.isDefault,
      }));

      const maxLane = Math.max(...[...commitLanes.values()], 0);
      const graphAreaWidth = leftPadding + (maxLane + 1) * nodeSpacingX + 20;
      const height = topPadding + commits.length * nodeSpacingY + 25;

      return {
        positions: positionedCommits,
        graphWidth: graphAreaWidth,
        graphHeight: height,
        branchLabels: branchEndLabels,
        timelineLabels: timeline,
      };
    }, [network]);

  const handleMouseEnter = (pos: CommitPosition) => {
    const margin = 8;

    const x = graphWidth + 40;
    let y = pos.y - 8;

    if (y < margin) y = margin;
    if (y > graphHeight - margin) y = graphHeight - margin;

    setTooltip({
      x,
      y,
      sha: pos.shortSha,
      message: pos.message,
      author: pos.authorLogin,
      date: pos.date,
      branches: pos.branches,
    });
  };

  const handleMouseLeave = () => {
    setTooltip(null);
  };

  if (loading) {
    return <p className="muted">Loading network graph...</p>;
  }

  if (error) {
    return <p className="error">Error: {error}</p>;
  }

  if (!network || network.commits.length === 0) {
    return <p className="muted">No commits found.</p>;
  }

  return (
    <div className="network-container" ref={containerRef}>
      <div className="network-header-bar">
        <h4>Network</h4>
        <p className="network-subtitle">Commit history and branch structure</p>
      </div>

      {/* Branch legend */}
      <div className="network-branch-legend">
        {branchLabels.map((branch) => (
          <span key={branch.name} className="branch-legend-item">
            <span
              className="branch-legend-dot"
              style={{ backgroundColor: branch.color }}
            />
            {branch.name}
            {branch.isDefault && <span className="branch-default-tag">default</span>}
          </span>
        ))}
      </div>

      <div className="network-with-timeline">
        {/* Timeline column */}
        <div className="network-timeline" style={{ height: graphHeight }}>
          {timelineLabels.map((item, idx) => (
            <div key={idx} className="timeline-label" style={{ top: item.y - 8 }}>
              {item.label}
            </div>
          ))}
        </div>

        {/* Graph area */}
        <div className="network-graph-only">
          <svg width={graphWidth} height={graphHeight} className="network-svg-clean">
            {/* Connection lines */}
            <g className="graph-connections">
              {positions.map((pos) =>
                pos.parentPositions.map((parent, pIdx) => {
                  const isSameLane = pos.x === parent.x;

                  let path: string;
                  if (isSameLane) {
                    // Straight vertical line
                    path = `M ${pos.x} ${pos.y} L ${parent.x} ${parent.y}`;
                  } else if (pos.x > parent.x) {
                    // Branch from left to right
                    path = `M ${pos.x} ${pos.y} 
                                L ${pos.x} ${pos.y + 8}
                                Q ${pos.x} ${parent.y - 4}, ${parent.x + 8} ${parent.y}
                                L ${parent.x} ${parent.y}`;
                  } else {
                    // Merge from right to left
                    path = `M ${pos.x} ${pos.y}
                                L ${pos.x} ${pos.y + 8}
                                Q ${pos.x} ${parent.y - 4}, ${parent.x - 8} ${parent.y}
                                L ${parent.x} ${parent.y}`;
                  }

                  return (
                    <path
                      key={`${pos.sha}-${pIdx}`}
                      d={path}
                      stroke={parent.color}
                      strokeWidth={2}
                      fill="none"
                    />
                  );
                }),
              )}
            </g>

            {/* Commit nodes */}
            <g className="graph-nodes">
              {positions.map((pos) => (
                <g key={pos.sha}>
                  <circle
                    cx={pos.x}
                    cy={pos.y}
                    r={5}
                    fill={pos.color}
                    className="commit-dot-clean"
                    onMouseEnter={() => handleMouseEnter(pos)}
                    onMouseLeave={handleMouseLeave}
                  />
                  {/* Small branch indicator if commit has branches */}
                  {pos.branches.length > 0 && (
                    <circle
                      cx={pos.x}
                      cy={pos.y}
                      r={8}
                      fill="none"
                      stroke={pos.color}
                      strokeWidth={1.5}
                      opacity={0.5}
                    />
                  )}
                </g>
              ))}
            </g>
          </svg>
        </div>

        {/* Tooltip */}
        {tooltip && (
          <div className="network-tooltip" style={{ left: tooltip.x, top: tooltip.y }}>
            <div className="tooltip-sha">{tooltip.sha}</div>
            <div className="tooltip-message">{tooltip.message}</div>
            <div className="tooltip-meta">
              <span>{tooltip.author}</span>
              <span>•</span>
              <span>{tooltip.date}</span>
            </div>
            {tooltip.branches.length > 0 && (
              <div className="tooltip-branches">🏷️ {tooltip.branches.join(", ")}</div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
