import type { NeoSummary } from "../types";

type DossierProps = {
  item: NeoSummary | null;
};

function formatApproachTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Unknown";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "full",
    timeStyle: "medium"
  }).format(date);
}

function formatKm(value: number): string {
  return Number.isFinite(value)
    ? `${Math.round(value).toLocaleString()} km`
    : "N/A";
}

function formatLunar(value: number): string {
  return Number.isFinite(value) ? `${value.toFixed(2)} LD` : "N/A";
}

function formatVelocity(value: number): string {
  return Number.isFinite(value) ? `${value.toFixed(2)} km/s` : "N/A";
}

function formatDiameter(min: number, max: number): string {
  if (!Number.isFinite(min) || !Number.isFinite(max)) {
    return "N/A";
  }
  return `${Math.round(min).toLocaleString()}-${Math.round(max).toLocaleString()} m`;
}

export function Dossier({ item }: DossierProps) {
  if (!item) {
    return (
      <section className="panel dossierPanel">
        <div className="panelHeader">
          <h2>Dossier</h2>
        </div>
        <p className="muted">No target selected.</p>
      </section>
    );
  }

  return (
    <section className="panel dossierPanel">
      <div className="panelHeader">
        <h2>Dossier</h2>
        <span className={`statusLamp ${item.isHazardous ? "hot" : "calm"}`}>
          {item.isHazardous ? "Hazardous" : "Tracked"}
        </span>
      </div>

      <div className="dossierTitleBlock">
        <div className="eyebrow">Designation</div>
        <h3>{item.name}</h3>
        <div className="monoId">ID {item.id}</div>
      </div>

      <div className="dossierGrid">
        <div className="kv">
          <span>Approach Time</span>
          <strong>{formatApproachTime(item.closeApproachTime)}</strong>
        </div>
        <div className="kv">
          <span>Orbiting Body</span>
          <strong>{item.orbitingBody || "Earth"}</strong>
        </div>
        <div className="kv">
          <span>Miss Distance</span>
          <strong>{formatKm(item.missDistanceKm)}</strong>
        </div>
        <div className="kv">
          <span>Miss Distance (Lunar)</span>
          <strong>{formatLunar(item.missDistanceLunar)}</strong>
        </div>
        <div className="kv">
          <span>Relative Velocity</span>
          <strong>{formatVelocity(item.relativeVelocityKmPerSec)}</strong>
        </div>
        <div className="kv">
          <span>Diameter Range</span>
          <strong>{formatDiameter(item.diameterMinMeters, item.diameterMaxMeters)}</strong>
        </div>
      </div>
    </section>
  );
}

