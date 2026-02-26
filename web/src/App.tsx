import { useEffect, useState } from "react";

type ApiState = {
  status: "checking..." | "ok" | "error";
  errorMessage: string;
};

export default function App() {
  const [apiState, setApiState] = useState<ApiState>({
    status: "checking...",
    errorMessage: ""
  });

  useEffect(() => {
    let cancelled = false;

    async function checkHealth() {
      try {
        const response = await fetch("/api/health");
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data = (await response.json()) as { status?: string };
        if (!cancelled) {
          setApiState({
            status: data.status === "ok" ? "ok" : "error",
            errorMessage: data.status === "ok" ? "" : "Unexpected API response"
          });
        }
      } catch (error) {
        if (!cancelled) {
          const message =
            error instanceof Error ? error.message : "Request failed";
          setApiState({
            status: "error",
            errorMessage: message
          });
        }
      }
    }

    void checkHealth();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main
      style={{
        fontFamily: "system-ui, sans-serif",
        minHeight: "100vh",
        display: "grid",
        placeItems: "center",
        margin: 0,
        background: "#0b1220",
        color: "#e8eefc",
        padding: "2rem"
      }}
    >
      <section
        style={{
          width: "min(640px, 100%)",
          border: "1px solid #2d3a59",
          borderRadius: "12px",
          padding: "1.25rem 1.5rem",
          background: "#111a2d"
        }}
      >
        <h1 style={{ margin: "0 0 0.75rem", fontSize: "1.5rem" }}>
          Asteroid Hunter Tactical Console
        </h1>
        <p style={{ margin: 0, fontSize: "1rem" }}>
          API Status: {apiState.status}
        </p>
        {apiState.status === "error" && (
          <p style={{ margin: "0.5rem 0 0", color: "#ffb4b4", fontSize: "0.9rem" }}>
            {apiState.errorMessage}
          </p>
        )}
      </section>
    </main>
  );
}
