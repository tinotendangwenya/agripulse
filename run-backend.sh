#!/usr/bin/env bash
# Starts the Spring app with the secrets from .env. Never "source" .env: the password contains a backtick.
set -euo pipefail
cd "$(dirname "$0")"
get() { grep -m1 "^$1=" .env | cut -d= -f2-; }
export JWT_SECRET="$(get JWT_SECRET)"
export DB_PASSWORD="$(get DB_PASSWORD)"
[ -n "$JWT_SECRET" ] || { echo "JWT_SECRET is missing in .env"; exit 1; }
cd backend && ./mvnw spring-boot:run
