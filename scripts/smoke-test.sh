#!/usr/bin/env bash
#
# Bowmenn API smoke test
# ----------------------
# Exercises every endpoint exposed by the API and asserts the expected
# HTTP status and (where relevant) response body fields.
#
# Usage:
#   ./scripts/smoke-test.sh                 # tests http://localhost:8080
#   BASE_URL=http://host:port ./scripts/smoke-test.sh
#   ADMIN_EMAIL=... ADMIN_PASSWORD=... ./scripts/smoke-test.sh
#
# Requires: bash, curl, python3. Exits non-zero if any assertion fails.

set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@bowmenn.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
STAMP="$(date +%s)$RANDOM"

PASS=0
FAIL=0
FAILURES=()

# ---- output helpers ---------------------------------------------------------
if [ -t 1 ]; then GREEN=$'\e[32m'; RED=$'\e[31m'; YEL=$'\e[33m'; BLU=$'\e[36m'; DIM=$'\e[2m'; RST=$'\e[0m'
else GREEN=""; RED=""; YEL=""; BLU=""; DIM=""; RST=""; fi

section() { printf "\n${BLU}== %s ==${RST}\n" "$1"; }

pass() { PASS=$((PASS+1)); printf "  ${GREEN}PASS${RST} %s\n" "$1"; }
fail() { FAIL=$((FAIL+1)); FAILURES+=("$1"); printf "  ${RED}FAIL${RST} %s\n" "$1"; }

# ---- JSON extraction (no jq dependency) ------------------------------------
# json '<expr>'  where expr is python indexing into the variable `d`, e.g. "['data']['id']"
json() { python3 -c "import sys,json
try:
    d=json.load(sys.stdin)
    v=eval('d'+sys.argv[1])
    print('' if v is None else v)
except Exception:
    print('')" "$1"; }

# ---- HTTP helper ------------------------------------------------------------
# req METHOD PATH [TOKEN] [JSON_BODY]
# Populates globals: HTTP_CODE, HTTP_BODY
req() {
  local method="$1" path="$2" token="${3:-}" body="${4:-}"
  local args=(-s -o /tmp/bwn_body.$$ -w '%{http_code}' -X "$method" "$BASE_URL$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  if [ -n "$body" ]; then args+=(-H 'Content-Type: application/json' -d "$body"); fi
  HTTP_CODE="$(curl "${args[@]}")"
  HTTP_BODY="$(cat /tmp/bwn_body.$$ 2>/dev/null)"
  rm -f /tmp/bwn_body.$$
}

# multipart upload helper: upload PATH TOKEN FILE NOTE
upload() {
  local path="$1" token="$2" file="$3" note="$4"
  HTTP_CODE="$(curl -s -o /tmp/bwn_body.$$ -w '%{http_code}' -X POST "$BASE_URL$path" \
      -H "Authorization: Bearer $token" -F "file=@$file" -F "note=$note")"
  HTTP_BODY="$(cat /tmp/bwn_body.$$ 2>/dev/null)"; rm -f /tmp/bwn_body.$$
}

# ---- assertions -------------------------------------------------------------
# check_status LABEL EXPECTED_CODE
check_status() {
  local label="$1" want="$2"
  if [ "$HTTP_CODE" = "$want" ]; then pass "$label (HTTP $HTTP_CODE)"
  else fail "$label — expected HTTP $want, got $HTTP_CODE ${DIM}body=$HTTP_BODY${RST}"; fi
}
# check_field LABEL 'expr' EXPECTED
check_field() {
  local label="$1" expr="$2" want="$3" got
  got="$(printf '%s' "$HTTP_BODY" | json "$expr")"
  if [ "$got" = "$want" ]; then pass "$label ($expr=$got)"
  else fail "$label — expected $expr='$want', got '$got' ${DIM}body=$HTTP_BODY${RST}"; fi
}
extract() { printf '%s' "$HTTP_BODY" | json "$1"; }

# =============================================================================
printf "${BLU}Bowmenn API smoke test${RST}\n"
printf "  target: %s\n  run id: %s\n" "$BASE_URL" "$STAMP"

# ---- connectivity -----------------------------------------------------------
section "Connectivity"
req GET /api-docs
if [ "$HTTP_CODE" = "000" ]; then
  printf "${RED}Cannot reach %s. Start the app first: (cd bowmenn-api && ./mvnw spring-boot:run)${RST}\n" "$BASE_URL"
  exit 2
fi
check_status "OpenAPI /api-docs reachable" 200
req GET /swagger-ui/index.html
check_status "Swagger UI reachable" 200

# ---- Authentication ---------------------------------------------------------
section "Authentication"
CUST_EMAIL="cust_${STAMP}@test.com"
DRV_EMAIL="drv_${STAMP}@test.com"
DRV2_EMAIL="drv2_${STAMP}@test.com"
PW="secret1"

req POST /api/auth/register "" "{\"fullName\":\"Cust $STAMP\",\"email\":\"$CUST_EMAIL\",\"password\":\"$PW\",\"phone\":\"+2348010000000\",\"role\":\"CUSTOMER\"}"
check_status "register customer" 200
check_field "register customer -> role" "['data']['user']['role']" "CUSTOMER"
CUST_TOKEN="$(extract "['data']['token']")"
CUST_ID="$(extract "['data']['user']['id']")"

req POST /api/auth/register "" "{\"fullName\":\"Driver $STAMP\",\"email\":\"$DRV_EMAIL\",\"password\":\"$PW\",\"role\":\"DRIVER\"}"
check_status "register driver #1" 200
DRV_TOKEN="$(extract "['data']['token']")"
DRV_ID="$(extract "['data']['user']['id']")"

req POST /api/auth/register "" "{\"fullName\":\"Driver2 $STAMP\",\"email\":\"$DRV2_EMAIL\",\"password\":\"$PW\",\"role\":\"DRIVER\"}"
check_status "register driver #2" 200
DRV2_ID="$(extract "['data']['user']['id']")"

req POST /api/auth/register "" "{\"fullName\":\"Hax $STAMP\",\"email\":\"hax_${STAMP}@test.com\",\"password\":\"$PW\",\"role\":\"ADMIN\"}"
check_status "reject self-register as ADMIN" 400
check_field "self-register admin -> message" "['message']" "Cannot self-register as admin"

req POST /api/auth/register "" "{\"fullName\":\"Dup $STAMP\",\"email\":\"$CUST_EMAIL\",\"password\":\"$PW\",\"role\":\"CUSTOMER\"}"
check_status "reject duplicate email" 400

req POST /api/auth/register "" "{\"fullName\":\"X\",\"email\":\"not-an-email\",\"password\":\"1\",\"role\":\"CUSTOMER\"}"
check_status "reject invalid payload (validation)" 400
check_field "validation -> message" "['message']" "Validation failed"

req POST /api/auth/login "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"
check_status "admin login" 200
check_field "admin login -> role" "['data']['user']['role']" "ADMIN"
ADMIN_TOKEN="$(extract "['data']['token']")"

req POST /api/auth/login "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"wrong-password\"}"
check_status "reject bad credentials" 401

req GET /api/auth/me "$CUST_TOKEN"
check_status "GET /api/auth/me (customer)" 200
check_field "me -> email" "['data']['email']" "$CUST_EMAIL"

# ---- Shipments (customer) ---------------------------------------------------
section "Shipments — Customer"
SHIP_BODY="{\"pickupAddress\":\"Lagos\",\"pickupLat\":6.5244,\"pickupLng\":3.3792,\"deliveryAddress\":\"Ibadan\",\"deliveryLat\":7.3775,\"deliveryLng\":3.9470,\"cargoDescription\":\"Electronics\",\"cargoWeight\":500.0,\"truckType\":\"MEDIUM\"}"

req POST /api/shipments "$CUST_TOKEN" "$SHIP_BODY"
check_status "create shipment S1" 200
check_field "S1 status" "['data']['status']" "PENDING"
S1="$(extract "['data']['id']")"
S1_TRACK="$(extract "['data']['trackingNumber']")"
S1_PRICE="$(extract "['data']['estimatedPrice']")"
printf "    ${DIM}S1=%s track=%s price=%s${RST}\n" "$S1" "$S1_TRACK" "$S1_PRICE"

req POST /api/shipments "$CUST_TOKEN" "$SHIP_BODY"; S2="$(extract "['data']['id']")"
req POST /api/shipments "$CUST_TOKEN" "$SHIP_BODY"; S3="$(extract "['data']['id']")"
[ -n "$S2" ] && pass "create shipment S2" || fail "create shipment S2"
[ -n "$S3" ] && pass "create shipment S3" || fail "create shipment S3"

req GET /api/shipments/my "$CUST_TOKEN"
check_status "GET /api/shipments/my" 200

req GET "/api/shipments/$S1" "$CUST_TOKEN"
check_status "GET /api/shipments/{id}" 200
check_field "get by id -> id" "['data']['id']" "$S1"

req GET "/api/shipments/track/$S1_TRACK" "$CUST_TOKEN"
check_status "GET /api/shipments/track/{trackingNumber}" 200
check_field "track -> tracking" "['data']['trackingNumber']" "$S1_TRACK"

# ---- Admin ------------------------------------------------------------------
section "Admin Dashboard"
req GET /api/admin/shipments "$ADMIN_TOKEN";  check_status "GET /api/admin/shipments" 200
req GET /api/admin/drivers   "$ADMIN_TOKEN";  check_status "GET /api/admin/drivers" 200
req GET /api/admin/customers "$ADMIN_TOKEN";  check_status "GET /api/admin/customers" 200
req GET /api/admin/stats     "$ADMIN_TOKEN";  check_status "GET /api/admin/stats" 200

req PUT "/api/admin/shipments/$S1/assign" "$ADMIN_TOKEN" "{\"driverId\":\"$DRV_ID\"}"
check_status "assign driver to S1" 200
check_field "S1 -> ASSIGNED" "['data']['status']" "ASSIGNED"

req PUT "/api/admin/shipments/$S2/assign" "$ADMIN_TOKEN" "{\"driverId\":\"$DRV_ID\"}"
check_field "S2 -> ASSIGNED" "['data']['status']" "ASSIGNED"

# admin can drive status transitions too: cancel S3 (PENDING -> CANCELLED)
req PUT "/api/admin/shipments/$S3/status" "$ADMIN_TOKEN" "{\"status\":\"CANCELLED\",\"note\":\"admin cancel\"}"
check_status "admin cancel S3" 200
check_field "S3 -> CANCELLED" "['data']['status']" "CANCELLED"

# toggle a user's active flag, verify a deactivated user cannot log in
req PUT "/api/admin/users/$DRV2_ID/toggle-status" "$ADMIN_TOKEN"
check_status "toggle driver#2 status" 200
check_field "driver#2 -> inactive" "['data']['isActive']" "False"
req POST /api/auth/login "" "{\"email\":\"$DRV2_EMAIL\",\"password\":\"$PW\"}"
check_status "deactivated user login rejected" 403
req PUT "/api/admin/users/$DRV2_ID/toggle-status" "$ADMIN_TOKEN"   # reactivate
check_field "driver#2 -> active again" "['data']['isActive']" "True"

# ---- Driver -----------------------------------------------------------------
section "Driver Portal"
req GET /api/driver/shipments "$DRV_TOKEN"
check_status "GET /api/driver/shipments" 200

# happy path on S1: accept -> picked_up -> in_transit
req PUT "/api/driver/shipments/$S1/accept" "$DRV_TOKEN"
check_status "driver accept S1" 200
check_field "S1 -> ACCEPTED" "['data']['status']" "ACCEPTED"

req PUT "/api/driver/shipments/$S1/status" "$DRV_TOKEN" "{\"status\":\"PICKED_UP\"}"
check_field "S1 -> PICKED_UP" "['data']['status']" "PICKED_UP"
req PUT "/api/driver/shipments/$S1/status" "$DRV_TOKEN" "{\"status\":\"IN_TRANSIT\"}"
check_field "S1 -> IN_TRANSIT" "['data']['status']" "IN_TRANSIT"

# invalid transition rejected
req PUT "/api/driver/shipments/$S1/status" "$DRV_TOKEN" "{\"status\":\"ACCEPTED\"}"
check_status "reject invalid transition IN_TRANSIT->ACCEPTED" 400

# reject path on S2
req PUT "/api/driver/shipments/$S2/reject" "$DRV_TOKEN"
check_status "driver reject S2" 200
check_field "S2 -> REJECTED" "['data']['status']" "REJECTED"

# ---- Proof of Delivery ------------------------------------------------------
section "Proof of Delivery"
POD_FILE="$(mktemp /tmp/bwn_pod_XXXX).png"
# a real 1x1 PNG so the storage provider accepts it as an image
printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==' | base64 -d > "$POD_FILE"

upload "/api/pod/$S1" "$DRV_TOKEN" "$POD_FILE" "Delivered to reception"
check_status "POST /api/pod/{shipmentId}" 200
POD_URL="$(extract "['data']['imageUrl']")"
case "$POD_URL" in
  http*://ik.imagekit.io/*) pass "POD stored on ImageKit ($POD_URL)"; POD_FETCH="$POD_URL" ;;
  http*)                    pass "POD imageUrl returned ($POD_URL)";  POD_FETCH="$POD_URL" ;;
  /uploads/*)               pass "POD stored locally ($POD_URL)";     POD_FETCH="$BASE_URL$POD_URL" ;;
  *)                        fail "POD imageUrl missing/invalid ('$POD_URL')"; POD_FETCH="" ;;
esac

# The stored image must actually be retrievable over HTTP.
if [ -n "$POD_FETCH" ]; then
  code="$(curl -s -o /dev/null -w '%{http_code}' "$POD_FETCH")"
  if [ "$code" = "200" ]; then pass "POD image is publicly served (HTTP 200)"
  else fail "POD image not served — GET $POD_FETCH returned $code"; fi
fi

req GET "/api/shipments/$S1" "$ADMIN_TOKEN"
check_field "S1 auto -> DELIVERED after POD" "['data']['status']" "DELIVERED"

req GET "/api/pod/$S1" "$ADMIN_TOKEN"
check_status "GET /api/pod/{shipmentId}" 200
check_field "GET pod -> shipmentId" "['data']['shipmentId']" "$S1"

# duplicate POD rejected (shipment already has one)
upload "/api/pod/$S1" "$DRV_TOKEN" "$POD_FILE" "second try"
check_status "reject duplicate POD" 400
rm -f "$POD_FILE"

# ---- RBAC / access control --------------------------------------------------
section "Access Control (RBAC)"
req GET /api/admin/shipments ""
if [ "$HTTP_CODE" = "403" ] || [ "$HTTP_CODE" = "401" ]; then pass "no-token -> /api/admin blocked ($HTTP_CODE)"; else fail "no-token -> /api/admin blocked (got $HTTP_CODE)"; fi
req GET /api/admin/shipments "$CUST_TOKEN"; check_status "customer -> admin = 403" 403
req GET /api/driver/shipments "$CUST_TOKEN"; check_status "customer -> driver = 403" 403
req POST /api/shipments "$DRV_TOKEN" "$SHIP_BODY"; check_status "driver -> create shipment = 403" 403

# ---- Object-level authorization (IDOR regressions) --------------------------
section "Object-level Authorization"
# A second customer and the second driver must not be able to touch CUSTOMER's shipment S1.
OTHER_EMAIL="other_${STAMP}@test.com"
req POST /api/auth/register "" "{\"fullName\":\"Other Cust\",\"email\":\"$OTHER_EMAIL\",\"password\":\"$PW\",\"role\":\"CUSTOMER\"}"
OTHER_TOKEN="$(extract "['data']['token']")"
req POST /api/auth/login "" "{\"email\":\"$DRV2_EMAIL\",\"password\":\"$PW\"}"
DRV2_TOKEN="$(extract "['data']['token']")"

req GET "/api/shipments/$S1" "$OTHER_TOKEN"
check_status "other customer cannot read another's shipment" 403

req GET "/api/shipments/track/$S1_TRACK" "$OTHER_TOKEN"
check_status "other customer cannot track another's shipment" 403

req PUT "/api/driver/shipments/$S1/accept" "$DRV2_TOKEN"
check_status "unassigned driver cannot act on a shipment" 403

req GET "/api/pod/$S1" "$OTHER_TOKEN"
check_status "other customer cannot read another's POD" 403

POD_FILE2="$(mktemp /tmp/bwn_pod2_XXXX).png"
printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==' | base64 -d > "$POD_FILE2"
upload "/api/pod/$S2" "$OTHER_TOKEN" "$POD_FILE2" "should be denied"
check_status "non-driver cannot upload POD (self-certify delivery)" 403

req GET "/api/shipments/$S1" "$CUST_TOKEN"
check_status "owner can still read their own shipment" 200
req GET "/api/shipments/$S1" "$ADMIN_TOKEN"
check_status "admin can still read any shipment" 200
rm -f "$POD_FILE2"

# =============================================================================
section "Summary"
printf "  ${GREEN}%d passed${RST}, " "$PASS"
if [ "$FAIL" -eq 0 ]; then printf "${GREEN}0 failed${RST}\n"; printf "\n${GREEN}ALL CHECKS PASSED${RST}\n"; exit 0
else
  printf "${RED}%d failed${RST}\n" "$FAIL"
  printf "\n${RED}Failures:${RST}\n"; for f in "${FAILURES[@]}"; do printf "  - %s\n" "$f"; done
  exit 1
fi
