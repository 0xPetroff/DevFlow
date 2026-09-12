#!/usr/bin/env bash
#
# Publishes the built SPA to S3 and invalidates CloudFront.
#
#   deploy-web.sh <environment> <dist-directory>
#
# Uploaded in two passes, because the two kinds of file have opposite caching rules: the hashed
# bundles under assets/ are immutable and go first, so no request can ever reach a new index.html
# whose bundles are not there yet.

set -euo pipefail

ENVIRONMENT="${1:?usage: deploy-web.sh <environment> <dist-directory>}"
DIST="${2:?missing dist directory}"

BUCKET="${WEB_BUCKET:-devflow-${ENVIRONMENT}-web}"
DISTRIBUTION_ID="${CLOUDFRONT_DISTRIBUTION_ID:?CLOUDFRONT_DISTRIBUTION_ID is not set}"

[ -d "$DIST" ] || { echo "no build output at $DIST" >&2; exit 1; }
[ -f "$DIST/index.html" ] || { echo "$DIST holds no index.html" >&2; exit 1; }

aws s3 sync "$DIST" "s3://${BUCKET}" \
    --exclude 'index.html' \
    --cache-control 'public, max-age=31536000, immutable' \
    --no-progress

# Deleting here rather than in the first pass: --delete alongside --exclude index.html would
# remove the very file the second pass is about to replace.
aws s3 sync "$DIST" "s3://${BUCKET}" \
    --delete \
    --cache-control 'no-cache' \
    --no-progress

aws cloudfront create-invalidation \
    --distribution-id "$DISTRIBUTION_ID" \
    --paths '/index.html' '/' \
    --query 'Invalidation.Id' --output text

echo "published to s3://${BUCKET}"
