import { useEffect, useRef } from "react";
import type { NeoSummary } from "../types";

type RosterProps = {
  items: NeoSummary[];
  selectedId: string | null;
  onSelect: (id: string) => void;
};

function formatTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Unknown time";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

export function Roster({ items, selectedId, onSelect }: RosterProps) {
  const rowRefs = useRef(new Map<string, HTMLButtonElement>());

  useEffect(() => {
    if (!selectedId) {
      return;
    }
    const el = rowRefs.current.get(selectedId);
    el?.scrollIntoView({ block: "nearest", behavior: "smooth" });
  }, [selectedId]);

  return (
    <section className="panel rosterPanel">
      <div className="panelHeader">
        <h2>Contacts / Targets</h2>
        <span className="badge">{items.length}</span>
      </div>
      <div className="rosterList" role="list">
        {items.map((item) => {
          const selected = item.id === selectedId;

          return (
            <button
              key={item.id}
              type="button"
              role="listitem"
              className={`rosterRow${selected ? " isSelected" : ""}`}
              ref={(el) => {
                if (el) {
                  rowRefs.current.set(item.id, el);
                } else {
                  rowRefs.current.delete(item.id);
                }
              }}
              onClick={() => onSelect(item.id)}
            >
              <div className="rosterRowTop">
                <span className="rosterName">{item.name}</span>
                {item.isHazardous && <span className="hazardTag">Hazard</span>}
              </div>
              <div className="rosterMeta">
                <span>{formatTime(item.closeApproachTime)}</span>
                <span>
                  {Number.isFinite(item.missDistanceLunar)
                    ? `${item.missDistanceLunar.toFixed(2)} LD`
                    : "Distance N/A"}
                </span>
              </div>
            </button>
          );
        })}
      </div>
    </section>
  );
}
