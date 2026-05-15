#!/usr/bin/env bash
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "=== Spouštím backend (Spring Boot) ==="
(cd "$ROOT/backend" && mvn spring-boot:run -s .mvn/settings.xml -q) &
BACKEND_PID=$!

echo "=== Spouštím frontend (Vite) ==="
(cd "$ROOT/frontend" && npm run dev) &
FRONTEND_PID=$!

echo ""
echo "Backend:  http://localhost:8080"
echo "Frontend: http://localhost:5173"
echo ""
echo "Zastavíš Ctrl+C"

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT TERM
wait
