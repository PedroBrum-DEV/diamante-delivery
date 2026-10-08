#!/usr/bin/env bash
# Fires 50 simultaneous orders for the promotion dish (id 1, stock 10).
# Expected: exactly 10 x 201 (confirmed), the rest rejected, stock ending at 0.
#
# Note: with the rate-limit bonus enabled (20 req/s) part of the 50 requests is answered
# with 429 before reaching the stock check. The number of CONFIRMED orders is still 10.
# To see the pure 201 / 409 split, start the order-service with:
#   ./gradlew :order-service:bootRun --args='--delivery.rate-limit.orders-per-second=0'
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"

echo "Stock before: $(curl -s "$BASE_URL/dishes/1")"
echo
echo "Status codes of 50 simultaneous POST /orders:"
seq 1 50 | xargs -P 50 -I{} curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST "$BASE_URL/orders" \
  -H 'Content-Type: application/json' \
  -d '{"dishId":1,"quantity":1}' | sort | uniq -c
echo
echo "Stock after:  $(curl -s "$BASE_URL/dishes/1")"
