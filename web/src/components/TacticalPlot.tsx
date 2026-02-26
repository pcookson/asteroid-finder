import { useEffect, useRef, useState, type MouseEvent } from "react";
import type { NeoSummary } from "../types";

type TacticalPlotProps = {
  items: NeoSummary[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  displayMaxLD?: number;
};

type PlotPoint = {
  id: string;
  name: string;
  missDistanceLunar: number;
  x: number;
  y: number;
  hitRadius: number;
};

type CanvasLayout = {
  size: number;
  offsetX: number;
  offsetY: number;
};

const HIT_PADDING_PX = 10;
const DEFAULT_DISPLAY_MAX_LD = 60;
const RING_FRACTIONS = [0.25, 0.5, 0.75, 1];
const JITTER_MAX_RADIANS = 0.06;
const JITTER_MAX_PX = 6;
const COLLISION_MAX_TRIES = 5;
const PLOT_PADDING_PX = 40;
const RING_LABEL_ANGLE_RADIANS = -0.35;
const INNER_RADIUS_PX = 22;
const POWER_SCALE_GAMMA = 0.45;

function stableHash(input: string): number {
  let hash = 2166136261;
  for (let i = 0; i < input.length; i += 1) {
    hash ^= input.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function angleForId(id: string): number {
  const normalized = stableHash(id) / 0xffffffff;
  return normalized * Math.PI * 2;
}

function normalizedHash(input: string): number {
  return stableHash(input) / 0xffffffff;
}

function scaleRadiusFromLD(
  distanceLD: number,
  minLD: number,
  maxLD: number,
  innerRadiusPx: number,
  outerRadiusPx: number
): number {
  if (
    !Number.isFinite(distanceLD) ||
    distanceLD <= 0 ||
    !Number.isFinite(minLD) ||
    !Number.isFinite(maxLD) ||
    outerRadiusPx <= innerRadiusPx
  ) {
    return innerRadiusPx;
  }

  const safeMin = Math.max(minLD, 1);
  const safeMax = Math.max(maxLD, safeMin);
  const clampedLD = Math.max(safeMin, Math.min(distanceLD, safeMax));

  if (safeMax === safeMin) {
    return (innerRadiusPx + outerRadiusPx) / 2;
  }

  const tLinearRaw = (clampedLD - safeMin) / (safeMax - safeMin);
  const tLinear = Math.max(
    0,
    Math.min(1, Number.isFinite(tLinearRaw) ? tLinearRaw : 0)
  );
  const t = Math.pow(tLinear, POWER_SCALE_GAMMA);

  return innerRadiusPx + t * (outerRadiusPx - innerRadiusPx);
}

function scaleDotSize(diameterMaxMeters: number): number {
  if (!Number.isFinite(diameterMaxMeters) || diameterMaxMeters <= 0) {
    return 4;
  }
  return Math.max(4, Math.min(14, Math.sqrt(diameterMaxMeters) * 0.35));
}

export function TacticalPlot({
  items,
  selectedId,
  onSelect,
  displayMaxLD = DEFAULT_DISPLAY_MAX_LD
}: TacticalPlotProps) {
  const hostRef = useRef<HTMLDivElement | null>(null);
  const frameRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const pointsRef = useRef<PlotPoint[]>([]);
  const layoutRef = useRef<CanvasLayout>({ size: 420, offsetX: 0, offsetY: 0 });
  const [size, setSize] = useState(420);
  const [hovered, setHovered] = useState<{
    id: string;
    name: string;
    missDistanceLunar: number;
    x: number;
    y: number;
  } | null>(null);

  useEffect(() => {
    const host = hostRef.current;
    if (!host) {
      return;
    }

    const resize = () => {
      const next = Math.max(220, Math.floor(Math.min(host.clientWidth, host.clientHeight)));
      setSize(next);
    };

    resize();

    const observer = new ResizeObserver(() => resize());
    observer.observe(host);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    const host = hostRef.current;
    const frame = frameRef.current;
    if (!canvas || !host || !frame) {
      return;
    }

    const cssWidth = size;
    const cssHeight = size;
    const offsetX = Math.max(0, (host.clientWidth - cssWidth) / 2);
    const offsetY = Math.max(0, (host.clientHeight - cssHeight) / 2);
    layoutRef.current = { size, offsetX, offsetY };

    frame.style.width = `${cssWidth}px`;
    frame.style.height = `${cssHeight}px`;
    frame.style.left = `${offsetX}px`;
    frame.style.top = `${offsetY}px`;

    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.floor(cssWidth * dpr);
    canvas.height = Math.floor(cssHeight * dpr);
    canvas.style.width = `${cssWidth}px`;
    canvas.style.height = `${cssHeight}px`;

    const ctx = canvas.getContext("2d");
    if (!ctx) {
      return;
    }

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, cssWidth, cssHeight);

    const plotWidth = Math.max(0, cssWidth - PLOT_PADDING_PX * 2);
    const plotHeight = Math.max(0, cssHeight - PLOT_PADDING_PX * 2);
    const centerX = PLOT_PADDING_PX + plotWidth / 2;
    const centerY = PLOT_PADDING_PX + plotHeight / 2;
    const outerRadius = Math.max(10, Math.min(plotWidth, plotHeight) / 2);
    const innerRadius = INNER_RADIUS_PX;
    const clampedDistances = items
      .map((item) => item.missDistanceLunar)
      .filter((value) => Number.isFinite(value) && value > 0)
      .map((value) => Math.min(value, displayMaxLD));

    const minObservedLD =
      clampedDistances.length > 0 ? Math.min(...clampedDistances) : 1;
    const minLD = Math.max(minObservedLD, 1);
    const maxLD = Math.max(displayMaxLD, minLD);

    // Background grid glow
    const glow = ctx.createRadialGradient(centerX, centerY, 0, centerX, centerY, outerRadius);
    glow.addColorStop(0, "rgba(134, 227, 255, 0.08)");
    glow.addColorStop(1, "rgba(7, 12, 22, 0)");
    ctx.fillStyle = glow;
    ctx.fillRect(0, 0, cssWidth, cssHeight);

    // Concentric rings
    for (const fraction of RING_FRACTIONS) {
      const r = outerRadius * fraction;
      ctx.beginPath();
      ctx.arc(centerX, centerY, r, 0, Math.PI * 2);
      ctx.strokeStyle =
        fraction === 1 ? "rgba(122, 162, 255, 0.35)" : "rgba(63, 87, 127, 0.28)";
      ctx.lineWidth = fraction === 1 ? 2 : 1;
      ctx.stroke();
    }

    // Crosshair guides
    ctx.strokeStyle = "rgba(63, 87, 127, 0.22)";
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(centerX, PLOT_PADDING_PX);
    ctx.lineTo(centerX, PLOT_PADDING_PX + plotHeight);
    ctx.moveTo(PLOT_PADDING_PX, centerY);
    ctx.lineTo(PLOT_PADDING_PX + plotWidth, centerY);
    ctx.stroke();

    // Earth
    ctx.beginPath();
    ctx.arc(centerX, centerY, 7, 0, Math.PI * 2);
    ctx.fillStyle = "#86e3ff";
    ctx.shadowColor = "rgba(134, 227, 255, 0.5)";
    ctx.shadowBlur = 10;
    ctx.fill();
    ctx.shadowBlur = 0;

    const points: PlotPoint[] = [];

    for (const item of items) {
      const angle = angleForId(item.id);
      const baseRadius = scaleRadiusFromLD(
        item.missDistanceLunar,
        minLD,
        maxLD,
        innerRadius,
        outerRadius
      );
      const jitterAngle =
        (normalizedHash(`${item.id}:angle`) - 0.5) * JITTER_MAX_RADIANS;
      const jitterRadius =
        (normalizedHash(`${item.id}:radius`) - 0.5) * JITTER_MAX_PX;
      const finalAngle = angle + jitterAngle;
      let radius = Math.max(
        innerRadius,
        Math.min(outerRadius, baseRadius + jitterRadius)
      );
      const dotRadius = scaleDotSize(item.diameterMaxMeters) + (item.id === selectedId ? 1.5 : 0);
      const minSeparation = dotRadius * 2 + 2;

      let x = centerX + radius * Math.cos(finalAngle);
      let y = centerY + radius * Math.sin(finalAngle);

      for (let attempt = 0; attempt < COLLISION_MAX_TRIES; attempt += 1) {
        const hasCollision = points.some((point) => {
          const dx = x - point.x;
          const dy = y - point.y;
          return Math.hypot(dx, dy) < minSeparation;
        });

        if (!hasCollision) {
          break;
        }

        const outwardNudge =
          3 + normalizedHash(`${item.id}:nudge:${attempt}`) * 3;
        radius = Math.min(outerRadius, radius + outwardNudge);
        x = centerX + radius * Math.cos(finalAngle);
        y = centerY + radius * Math.sin(finalAngle);
      }

      if (item.id === selectedId) {
        ctx.beginPath();
        ctx.arc(x, y, dotRadius + 6, 0, Math.PI * 2);
        ctx.strokeStyle = "rgba(134, 227, 255, 0.9)";
        ctx.lineWidth = 2;
        ctx.shadowColor = "rgba(134, 227, 255, 0.45)";
        ctx.shadowBlur = 14;
        ctx.stroke();
        ctx.shadowBlur = 0;
      }

      ctx.beginPath();
      ctx.arc(x, y, dotRadius, 0, Math.PI * 2);
      ctx.fillStyle = item.isHazardous ? "#ffb46a" : "#7aa2ff";
      ctx.fill();

      ctx.beginPath();
      ctx.arc(x, y, Math.max(2, dotRadius * 0.38), 0, Math.PI * 2);
      ctx.fillStyle = item.isHazardous ? "#3a190a" : "#d7e5ff";
      ctx.globalAlpha = 0.9;
      ctx.fill();
      ctx.globalAlpha = 1;

      points.push({
        id: item.id,
        name: item.name,
        missDistanceLunar: item.missDistanceLunar,
        x,
        y,
        hitRadius: dotRadius + HIT_PADDING_PX
      });
    }

    pointsRef.current = points;

    // Ring labels
    ctx.fillStyle = "rgba(159, 177, 209, 0.8)";
    ctx.font = '11px "IBM Plex Mono", monospace';
    ctx.textAlign = "left";
    ctx.textBaseline = "middle";
    ctx.fillText("Earth", centerX + 12, centerY - 8);
    for (const fraction of RING_FRACTIONS) {
      const labelLd = (displayMaxLD * fraction).toFixed(0);
      const ringRadius = outerRadius * fraction;
      const labelRadius = ringRadius + 12;
      const labelX = centerX + labelRadius * Math.cos(RING_LABEL_ANGLE_RADIANS);
      const labelY = centerY + labelRadius * Math.sin(RING_LABEL_ANGLE_RADIANS);
      ctx.textAlign = labelX > centerX ? "left" : "right";
      ctx.fillText(`${labelLd} LD`, labelX, labelY);
    }
    ctx.textAlign = "left";
    ctx.textBaseline = "alphabetic";
  }, [displayMaxLD, items, selectedId, size]);

  const findNearestPoint = (x: number, y: number) => {
    let best: PlotPoint | null = null;
    let bestDistance = Number.POSITIVE_INFINITY;

    for (const point of pointsRef.current) {
      const dx = x - point.x;
      const dy = y - point.y;
      const distance = Math.hypot(dx, dy);
      if (distance <= point.hitRadius && distance < bestDistance) {
        bestDistance = distance;
        best = point;
      }
    }

    return best;
  };

  const handleClick = (event: MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }
    const rect = canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const hit = findNearestPoint(x, y);
    if (hit) {
      onSelect(hit.id);
    }
  };

  const handleMouseMove = (event: MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }
    const rect = canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const hit = findNearestPoint(x, y);
    if (!hit) {
      setHovered(null);
      return;
    }
    setHovered({
      id: hit.id,
      name: hit.name,
      missDistanceLunar: hit.missDistanceLunar,
      x: layoutRef.current.offsetX + hit.x,
      y: layoutRef.current.offsetY + hit.y
    });
  };

  return (
    <section className="panel plotPanel">
      <div className="panelHeader">
        <h2>Tactical Plot</h2>
        <span className="badge">Canvas</span>
      </div>
      <div className="plotCanvasHost" ref={hostRef}>
        <div className="plotCanvasFrame" ref={frameRef}>
          <canvas
            ref={canvasRef}
            className="plotCanvas"
            onClick={handleClick}
            onMouseMove={handleMouseMove}
            onMouseLeave={() => setHovered(null)}
            aria-label="Tactical plot of near-Earth objects"
            role="img"
          />
        </div>
        {hovered && (
          <div
            className="plotTooltip"
            style={{
              left: `${Math.min(layoutRef.current.offsetX + size - 12, hovered.x + 14)}px`,
              top: `${Math.max(12, hovered.y - 12)}px`
            }}
          >
            <div className="plotTooltipName">{hovered.name}</div>
            <div className="plotTooltipMeta">
              {Number.isFinite(hovered.missDistanceLunar)
                ? `${hovered.missDistanceLunar.toFixed(2)} LD`
                : "Distance N/A"}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
