# Accident hotspot analysis

Spring Boot backend + a browser dashboard for accident hotspot analysis over
a live-simulated incident feed, mapped over real Chennai locations.

## Open in IntelliJ IDEA

1. `File > Open...` and select this folder (the one containing `pom.xml`).
2. IntelliJ detects it as a Maven project and imports dependencies automatically.
   If it doesn't prompt you, open the Maven tool window and click the refresh icon.
3. Open `HotspotApplication.java` and click the green run arrow next to `main`.
   (Or run `mvn spring-boot:run` from the terminal.)
4. Visit **http://localhost:8080** in your browser.

## Signing in

The dashboard is now behind a login page (Spring Security, session-based, in-memory users).
Visiting anything other than `/login.html` while signed out redirects you there.

**Demo credentials:**
- `admin` / `admin123`
- `analyst` / `analyst123`

Click the small **i** icon in the top-right corner of the login card to see creator details
(name + LinkedIn link).

Change or add users in `SecurityConfig.userDetailsService(...)`. Note that CSRF protection is
disabled in `SecurityConfig` purely to keep the plain static `login.html` form simple for this
demo — re-enable it and inject the CSRF token into the form before using this anywhere real.

## What's inside

- `IncidentSimulatorService` — seeds ~260 historical incidents on startup, then
  generates a new one every 4 seconds (`@Scheduled`) and pushes it to connected
  browsers over Server-Sent Events.
- `IncidentController` — exposes:
  - `GET /api/zones` — the 8 monitored Chennai junctions
  - `GET /api/incidents` — full incident history
  - `GET /api/incidents/stream` — the live SSE feed
- `src/main/resources/static/index.html` — the dashboard: a real Leaflet/OpenStreetMap
  view, ranked hotspot list, live feed ticker, hour/weekday/severity charts, a top navbar
  with a live "signed in as ..." chip (via `/api/me`), and a last-updated timestamp.
- `src/main/resources/static/login.html` — a two-panel login page (brand story on the left,
  form on the right) with an About icon revealing creator details.
- `SecurityConfig` — form login, in-memory users, and route protection.
- `AuthController` — exposes `/api/me` so the dashboard navbar can show who's signed in.

## Wiring up a real feed later

Replace the body of `generateLiveIncident()` in `IncidentSimulatorService` with
a call into your real accident data source (a message queue consumer, a polling
job against a government API, a webhook handler, etc.), and construct an
`Incident` from that data instead of the random generator. Everything downstream
(SSE broadcast, map, charts, stats) keeps working unchanged.
