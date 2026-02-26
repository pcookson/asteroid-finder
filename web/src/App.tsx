import { useEffect, useState } from "react";
import { fetchTodayNeos } from "./api/neos";
import { Dossier } from "./components/Dossier";
import { Roster } from "./components/Roster";
import { TacticalPlot } from "./components/TacticalPlot";
import type { NeoSummary } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "success"; items: NeoSummary[] };

export default function App() {
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadNeos() {
      setState({ kind: "loading" });
      try {
        const items = await fetchTodayNeos();
        if (cancelled) {
          return;
        }
        setState({ kind: "success", items });
        setSelectedId((current) => {
          if (current && items.some((item) => item.id === current)) {
            return current;
          }
          return items[0]?.id ?? null;
        });
      } catch (error) {
        if (cancelled) {
          return;
        }
        setState({
          kind: "error",
          message: error instanceof Error ? error.message : "Request failed"
        });
      }
    }

    void loadNeos();

    return () => {
      cancelled = true;
    };
  }, []);

  const retry = async () => {
    setState({ kind: "loading" });
    try {
      const items = await fetchTodayNeos();
      setState({ kind: "success", items });
      setSelectedId(items[0]?.id ?? null);
    } catch (error) {
      setState({
        kind: "error",
        message: error instanceof Error ? error.message : "Request failed"
      });
    }
  };

  if (state.kind === "loading") {
    return (
      <div className="appShell">
        <header className="topBar">
          <div className="titleBlock">
            <h1>Asteroid Hunter Tactical Console</h1>
            <p>Deep-space contact analysis relay</p>
          </div>
          <div className="topPill">Scanning…</div>
        </header>
        <main className="statePanel">
          <section className="stateCard">
            <h2>Scanning…</h2>
            <p>Querying today&apos;s near-Earth objects from mission telemetry.</p>
          </section>
        </main>
      </div>
    );
  }

  if (state.kind === "error") {
    return (
      <div className="appShell">
        <header className="topBar">
          <div className="titleBlock">
            <h1>Asteroid Hunter Tactical Console</h1>
            <p>Deep-space contact analysis relay</p>
          </div>
          <div className="topPill">Link Lost</div>
        </header>
        <main className="statePanel">
          <section className="stateCard">
            <h2>Transmission Error</h2>
            <p>Unable to load today&apos;s NEO roster. {state.message}</p>
            <button type="button" onClick={() => void retry()}>
              Retry Scan
            </button>
          </section>
        </main>
      </div>
    );
  }

  const selected =
    state.items.find((item) => item.id === selectedId) ?? state.items[0] ?? null;

  return (
    <div className="appShell">
      <header className="topBar">
        <div className="titleBlock">
          <h1>Asteroid Hunter Tactical Console</h1>
          <p>Live NEO roster for today&apos;s close approaches</p>
        </div>
        <div className="topPill">{state.items.length} Contacts</div>
      </header>

      {state.items.length === 0 ? (
        <main className="statePanel">
          <section className="stateCard">
            <h2>No Contacts</h2>
            <p>No near-Earth objects reported for today.</p>
            <button type="button" onClick={() => void retry()}>
              Refresh
            </button>
          </section>
        </main>
      ) : (
        <main className="contentGrid">
          <Roster
            items={state.items}
            selectedId={selected?.id ?? null}
            onSelect={setSelectedId}
          />
          <Dossier item={selected} />
          <TacticalPlot
            items={state.items}
            selectedId={selected?.id ?? null}
            onSelect={setSelectedId}
          />
        </main>
      )}
    </div>
  );
}
