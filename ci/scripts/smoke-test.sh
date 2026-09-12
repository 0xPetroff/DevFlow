#!/usr/bin/env bash
#
# Checks a deployed environment from outside, through the same origin a browser uses.
#
#   smoke-test.sh <base-url>
#
# The authorization check is the important one: an API answering 200 to an unauthenticated
# /auth/me would mean the security chain had not loaded, which a health probe cannot see.

set -euo pipefail

BASE_URL="${1:?usage: smoke-test.sh <base-url>}"
BASE_URL="${BASE_URL%/}"

ATTEMPTS="${SMOKE_ATTEMPTS:-10}"
INTERVAL="${SMOKE_INTERVAL_SECONDS:-6}"

status_of() {
    curl -sS -o /dev/null -w '%{http_code}' --max-time 10 "$1" || echo 000
}

check() {
    local what="$1" url="$2" expected="$3" actual

    actual=$(status_of "$url")
    if [ "$actual" = "$expected" ]; then
        echo "ok   $what"
        return 0
    fi

    echo "fail $what: expected $expected, got $actual" >&2
    return 1
}

# An unauthenticated /auth/me answering 401 is the readiness signal, rather than the actuator
# probe: it travels the whole public path through CloudFront, and it proves the security chain
# loaded. The container's own readiness has already gated `ecs wait services-stable`, and there is
# no reason to publish the actuator to the internet to repeat it.
echo "waiting for $BASE_URL to serve the API"
for attempt in $(seq 1 "$ATTEMPTS"); do
    if [ "$(status_of "${BASE_URL}/api/auth/me")" = "401" ]; then
        echo "ok   api rejects an unauthenticated call, after ${attempt} attempt(s)"
        break
    fi

    if [ "$attempt" -eq "$ATTEMPTS" ]; then
        echo "fail api never started answering" >&2
        exit 1
    fi
    sleep "$INTERVAL"
done

failures=0
check "app index" "${BASE_URL}/" 200 || failures=$((failures + 1))
# A 404 here means CloudFront is not rewriting unknown paths to index.html, so every link into
# the app from outside would break while the app itself looked fine.
check "deep link served by the SPA" "${BASE_URL}/projects" 200 || failures=$((failures + 1))

# The public demo is only usable if a visitor can get in, and both halves of that are easy to
# lose without anything going red: the API can come up without the demo profile, and the SPA can
# be built without VITE_DEMO_MODE, which drops the credentials panel at compile time. Neither
# shows up in a status code, so both are checked here. Set by the deploy workflow; an ordinary
# installation leaves it unset and skips these.
if [ "${SMOKE_EXPECT_DEMO:-}" = "true" ]; then
    login=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 30 \
        -X POST "${BASE_URL}/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d '{"identifier":"demo","password":"devflow-demo-1"}' || echo 000)
    if [ "$login" = "200" ]; then
        echo "ok   demo account signs in"
    else
        echo "fail demo account signs in: expected 200, got $login" >&2
        echo "     the API is not running the demo profile, so demo-data.sql never seeded" >&2
        failures=$((failures + 1))
    fi

    # Read the hashed entry bundle out of the index rather than guessing its name.
    bundle=$(curl -sS --max-time 10 "${BASE_URL}/" \
        | grep -o '/assets/index-[A-Za-z0-9_-]*\.js' | head -1 || true)
    if [ -n "$bundle" ] && curl -sS --max-time 30 "${BASE_URL}${bundle}" | grep -q 'devflow-demo-1'; then
        echo "ok   login page publishes the demo credentials"
    else
        echo "fail login page publishes the demo credentials" >&2
        echo "     the SPA was built without VITE_DEMO_MODE=true, so the panel is not in ${bundle:-the bundle}" >&2
        failures=$((failures + 1))
    fi
fi

[ "$failures" -eq 0 ] || { echo "$failures smoke check(s) failed" >&2; exit 1; }
echo "smoke tests passed against $BASE_URL"
