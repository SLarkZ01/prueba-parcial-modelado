#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
OUTPUT_DIR="${OUTPUT_DIR:-docs/openapi}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-120}"
NO_START_APP="${NO_START_APP:-false}"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

API_DOCS_JSON_URL="${API_BASE_URL}/v3/api-docs"
API_DOCS_YAML_URL="${API_BASE_URL}/v3/api-docs.yaml"

mkdir -p "$OUTPUT_DIR"

JSON_OUTPUT_PATH="${OUTPUT_DIR}/openapi.json"
YAML_OUTPUT_PATH="${OUTPUT_DIR}/openapi.yaml"

STARTED_APP_PID=""

is_api_ready() {
  curl -fsS "$API_DOCS_JSON_URL" >/dev/null 2>&1
}

stop_app_if_started() {
  if [[ -n "$STARTED_APP_PID" ]] && kill -0 "$STARTED_APP_PID" >/dev/null 2>&1; then
    echo "Deteniendo proceso temporal de Spring Boot..."
    kill "$STARTED_APP_PID"
  fi
}

trap stop_app_if_started EXIT

if ! is_api_ready; then
  if [[ "$NO_START_APP" == "true" ]]; then
    echo "La aplicacion no esta disponible en ${API_BASE_URL}. Inicia Spring Boot o usa NO_START_APP=false." >&2
    exit 1
  fi

  echo "Iniciando Spring Boot para exportar OpenAPI..."
  mkdir -p target
  ./mvnw spring-boot:run > target/openapi-export.out.log 2> target/openapi-export.err.log &
  STARTED_APP_PID="$!"

  DEADLINE=$((SECONDS + TIMEOUT_SECONDS))
  until is_api_ready; do
    if ! kill -0 "$STARTED_APP_PID" >/dev/null 2>&1; then
      echo "Spring Boot se detuvo antes de exponer OpenAPI. Revisa target/openapi-export.out.log y target/openapi-export.err.log" >&2
      exit 1
    fi

    if (( SECONDS > DEADLINE )); then
      echo "Timeout esperando OpenAPI en ${API_DOCS_JSON_URL}" >&2
      exit 1
    fi

    sleep 2
  done
fi

echo "Exportando OpenAPI JSON..."
curl -fsS "$API_DOCS_JSON_URL" -o "$JSON_OUTPUT_PATH"

echo "Exportando OpenAPI YAML..."
curl -fsS "$API_DOCS_YAML_URL" -o "$YAML_OUTPUT_PATH"

echo "Archivos generados correctamente:"
echo " - $JSON_OUTPUT_PATH"
echo " - $YAML_OUTPUT_PATH"
