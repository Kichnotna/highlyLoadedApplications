#!/bin/bash

docker-entrypoint.sh postgres &

until pg_isready -h localhost -p 5432 -U "$POSTGRES_USER"; do
  echo "Ждём PostgreSQL..."
  sleep 1
done

psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f /scripts/init.sql

wait -n
