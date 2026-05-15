#!/usr/bin/env bash
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "=== Spouštím backend (Spring Boot, dev profil — H2) ==="
(cd "$ROOT/backend" && mvn spring-boot:run -s .mvn/settings.xml -q -Dspring-boot.run.profiles=dev) &
BACKEND_PID=$!

echo "=== Spouštím frontend (Vite) ==="
(cd "$ROOT/frontend" && VITE_API_BASE_URL=http://localhost:8080 npm run dev) &
FRONTEND_PID=$!

echo ""
echo "Backend:  http://localhost:8080 (dev profil, H2 databáze)"
echo "Frontend: http://localhost:5173 (API → localhost:8080)"
echo ""
echo "Zastavíš Ctrl+C"

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT TERM
wait
