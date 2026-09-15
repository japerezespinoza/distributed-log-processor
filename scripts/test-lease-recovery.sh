#!/usr/bin/env bash

set -euo pipefail

COORDINATOR_URL="http://localhost:8080"
LEASE_WAIT_SECONDS=25

echo "=== Distributed Log Processor: Lease Recovery Test ==="
echo

# Verifica que el Coordinator esté respondiendo antes de comenzar
echo "[1/8] Checking coordinator..."
curl -fsS "${COORDINATOR_URL}/status" > /dev/null

echo "✓ Coordinator is running"
echo

# Limpia jobs anteriores para que la prueba tenga exactamente un job disponible
echo "[2/8] Cleaning previous jobs..."
docker compose exec -T postgres \
  psql -U logprocessor -d logprocessor \
  -c "UPDATE jobs
      SET status = 'COMPLETED',
          worker_id = NULL,
          lease_until = NULL;" > /dev/null

echo "✓ Previous jobs removed from queue"
echo

# Crea exactamente un nuevo job QUEUED
echo "[3/8] Creating test job..."

CREATE_RESPONSE=$(
  curl -fsS -X POST "${COORDINATOR_URL}/jobs" \
    -H "Content-Type: application/json" \
    -d '{"source":"/scratch/logs/failure-test.log","pattern":"ERROR"}'
)

# Extrae el ID del job de la respuesta JSON usando Python
JOB_ID=$(
  printf '%s' "${CREATE_RESPONSE}" |
    python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])'
)

echo "✓ Created job: ${JOB_ID}"
echo

# Worker-1 reclama el único job disponible
echo "[4/8] worker-1 claiming job..."

CLAIM_RESPONSE=$(
  curl -fsS -X POST \
    "${COORDINATOR_URL}/jobs/claim?workerId=worker-1"
)

# Extrae el ID que recibió worker-1
CLAIMED_JOB_ID=$(
  printf '%s' "${CLAIM_RESPONSE}" |
    python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])'
)

# Comprueba que el Coordinator asignó exactamente el job que acabamos de crear
if [[ "${JOB_ID}" != "${CLAIMED_JOB_ID}" ]]; then
  echo "✗ FAILED: worker-1 claimed a different job"
  echo "Created: ${JOB_ID}"
  echo "Claimed: ${CLAIMED_JOB_ID}"
  exit 1
fi

echo "✓ worker-1 claimed job ${CLAIMED_JOB_ID}"
echo

# Consulta inmediatamente PostgreSQL para comprobar ownership y lease
echo "[5/8] Checking RUNNING state..."

docker compose exec -T postgres \
  psql -U logprocessor -d logprocessor \
  -c "SELECT id,
             status,
             worker_id,
             lease_until,
             NOW() AS current_time
      FROM jobs
      WHERE id = '${JOB_ID}';"

# Recupera el estado actual directamente desde PostgreSQL
CURRENT_STATE=$(
  docker compose exec -T postgres \
    psql -U logprocessor -d logprocessor \
    -tAc "SELECT status || ':' || COALESCE(worker_id, '')
          FROM jobs
          WHERE id = '${JOB_ID}';"
)

# Verifica que worker-1 realmente sea dueño del job
if [[ "${CURRENT_STATE}" != "RUNNING:worker-1" ]]; then
  echo "✗ FAILED: expected RUNNING:worker-1"
  echo "Actual: ${CURRENT_STATE}"
  exit 1
fi

echo "✓ Job is RUNNING on worker-1"
echo

# Espera más que la duración configurada del lease
echo "[6/8] Waiting ${LEASE_WAIT_SECONDS}s for lease to expire..."
sleep "${LEASE_WAIT_SECONDS}"

echo "✓ Lease wait completed"
echo

# Consulta PostgreSQL después de la expiración
echo "[7/8] Checking automatic recovery..."

docker compose exec -T postgres \
  psql -U logprocessor -d logprocessor \
  -c "SELECT id,
             status,
             worker_id,
             lease_until,
             NOW() AS current_time
      FROM jobs
      WHERE id = '${JOB_ID}';"

# Recupera estado, owner y lease después de esperar
RECOVERED_STATE=$(
  docker compose exec -T postgres \
    psql -U logprocessor -d logprocessor \
    -tAc "SELECT status || ':' ||
                 COALESCE(worker_id, '') || ':' ||
                 COALESCE(lease_until::text, '')
          FROM jobs
          WHERE id = '${JOB_ID}';"
)

# El scheduler debe haber devuelto el job a QUEUED y limpiado owner/lease
if [[ "${RECOVERED_STATE}" != "QUEUED::" ]]; then
  echo "✗ FAILED: job was not recovered after lease expiration"
  echo "Actual: ${RECOVERED_STATE}"
  exit 1
fi

echo "✓ Job automatically returned to QUEUED"
echo

# Worker-2 reclama el job recuperado
echo "[8/8] worker-2 claiming recovered job..."

SECOND_CLAIM=$(
  curl -fsS -X POST \
    "${COORDINATOR_URL}/jobs/claim?workerId=worker-2"
)

# Extrae el ID y worker del segundo claim
SECOND_JOB_ID=$(
  printf '%s' "${SECOND_CLAIM}" |
    python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])'
)

SECOND_WORKER=$(
  printf '%s' "${SECOND_CLAIM}" |
    python3 -c 'import json,sys; print(json.load(sys.stdin)["workerId"])'
)

# Comprueba que se reasignó el mismo job y no se creó uno nuevo
if [[ "${SECOND_JOB_ID}" != "${JOB_ID}" ]]; then
  echo "✗ FAILED: worker-2 received a different job"
  echo "Original: ${JOB_ID}"
  echo "Received: ${SECOND_JOB_ID}"
  exit 1
fi

# Comprueba que el nuevo owner sea worker-2
if [[ "${SECOND_WORKER}" != "worker-2" ]]; then
  echo "✗ FAILED: expected worker-2 ownership"
  exit 1
fi

echo
echo "============================================"
echo "✓ LEASE RECOVERY TEST PASSED"
echo
echo "Job: ${JOB_ID}"
echo "worker-1 -> lease expired -> QUEUED -> worker-2"
echo "Same job ID preserved throughout recovery."
echo "============================================"
