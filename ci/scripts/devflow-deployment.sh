#!/usr/bin/env bash
#
# Drives a release through DevFlow's own deployment state machine, authenticating with a
# project-scoped API key. Every stage of a pipeline that touches a release calls this, so the
# history in DevFlow and the history in the CI provider can never disagree.
#
#   create                          Queue a deployment as PENDING and print its id
#   start <id>                      Move it to RUNNING, waiting for approval when required
#   finish <id> <STATUS> [reason]   SUCCESS, FAILED or CANCELLED
#
# Reads DEVFLOW_API_URL, DEVFLOW_API_KEY and DEVFLOW_PROJECT_ID from the environment, plus the
# build metadata the workflow exports.

set -euo pipefail

POLL_SECONDS="${DEVFLOW_POLL_SECONDS:-15}"
APPROVAL_TIMEOUT_SECONDS="${DEVFLOW_APPROVAL_TIMEOUT_SECONDS:-1800}"

fail() {
    echo "devflow-deployment: $*" >&2
    exit 1
}

require_env() {
    for name in "$@"; do
        [ -n "${!name:-}" ] || fail "$name is not set"
    done
}

# Sets API_STATUS and API_BODY, and returns non-zero for any status the caller should handle.
# The status code is appended on its own line by -w, so a body containing newlines stays intact.
# A transport failure is reported as 599 rather than an empty status, which would compare as 0.
api() {
    local method="$1" path="$2" body="${3:-}"
    local response
    local -a args=(-sS -X "$method" "${DEVFLOW_API_URL}${path}"
                   -H "X-DevFlow-Api-Key: ${DEVFLOW_API_KEY}"
                   -w '\n%{http_code}')

    if [ -n "$body" ]; then
        args+=(-H 'Content-Type: application/json' -d "$body")
    fi

    response=$(curl "${args[@]}") || response=$'could not reach the API\n599'

    API_STATUS="${response##*$'\n'}"
    API_BODY="${response%$'\n'*}"

    [ "$API_STATUS" -lt 400 ]
}

create() {
    require_env DEVFLOW_API_URL DEVFLOW_API_KEY DEVFLOW_PROJECT_ID DEPLOY_ENVIRONMENT \
        RELEASE_VERSION GIT_COMMIT BRANCH_NAME

    local payload
    payload=$(jq -n \
        --arg projectId "$DEVFLOW_PROJECT_ID" \
        --arg environmentName "$DEPLOY_ENVIRONMENT" \
        --arg releaseVersion "$RELEASE_VERSION" \
        --arg commitHash "$GIT_COMMIT" \
        --arg commitMessage "${COMMIT_MESSAGE:-}" \
        --arg branch "$BRANCH_NAME" \
        --arg pipelineUrl "${BUILD_URL:-}" \
        '{$projectId, $environmentName, $releaseVersion, $commitHash, $commitMessage, $branch,
          $pipelineUrl} | with_entries(select(.value != ""))')

    api POST /deployments "$payload" || fail "could not queue the deployment (HTTP $API_STATUS): $API_BODY"
    jq -er '.id' <<<"$API_BODY"
}

status_of() {
    api GET "/deployments/$1" || fail "could not read deployment $1 (HTTP $API_STATUS): $API_BODY"
    jq -er '.status' <<<"$API_BODY"
}

# An environment with requiresApproval refuses a start from anyone but a project administrator,
# so a 403 here means the release is queued and waiting for a person, not that the key is wrong:
# an unusable key would already have failed at create.
start() {
    local id="$1"
    require_env DEVFLOW_API_URL DEVFLOW_API_KEY

    if api PUT "/deployments/$id/status" '{"status":"RUNNING"}'; then
        echo "deployment $id is running"
        return 0
    fi

    [ "$API_STATUS" = "403" ] || fail "could not start deployment $id (HTTP $API_STATUS): $API_BODY"

    echo "deployment $id needs an administrator to approve it in DevFlow"
    echo "waiting up to ${APPROVAL_TIMEOUT_SECONDS}s"

    local waited=0 status
    while [ "$waited" -lt "$APPROVAL_TIMEOUT_SECONDS" ]; do
        sleep "$POLL_SECONDS"
        waited=$((waited + POLL_SECONDS))

        status=$(status_of "$id")
        case "$status" in
            RUNNING)
                echo "approved after ${waited}s"
                return 0
                ;;
            PENDING)
                ;;
            *)
                fail "deployment $id is $status, so the release will not proceed"
                ;;
        esac
    done

    finish "$id" CANCELLED "no approval within ${APPROVAL_TIMEOUT_SECONDS}s"
    fail "deployment $id was not approved in time"
}

finish() {
    local id="$1" status="$2" reason="${3:-}"
    require_env DEVFLOW_API_URL DEVFLOW_API_KEY

    local payload
    payload=$(jq -n --arg status "$status" --arg failureReason "$reason" \
        '{$status, $failureReason} | with_entries(select(.value != ""))')

    api PUT "/deployments/$id/status" "$payload" \
        || fail "could not report $status for deployment $id (HTTP $API_STATUS): $API_BODY"
    echo "deployment $id reported as $status"
}

command -v jq >/dev/null || fail "jq is required"

case "${1:-}" in
    create) create ;;
    start) start "${2:?usage: start <deployment-id>}" ;;
    finish) finish "${2:?usage: finish <deployment-id> <status> [reason]}" "${3:?missing status}" "${4:-}" ;;
    *) fail "usage: $(basename "$0") create | start <id> | finish <id> <status> [reason]" ;;
esac
