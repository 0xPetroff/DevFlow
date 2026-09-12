#!/usr/bin/env bash
#
# Rolls a new API image onto an ECS Fargate service.
#
#   deploy-ecs.sh <cluster> <service> <image>
#
# The task definition is Terraform's, not the pipeline's: this reads the revision the service is
# running, swaps only the image, and registers that as a new revision. Anything else about the
# task, from its memory to its secrets, keeps whatever the infrastructure declared, so a release
# can never quietly become the source of truth for configuration.

set -euo pipefail

CLUSTER="${1:?usage: deploy-ecs.sh <cluster> <service> <image>}"
SERVICE="${2:?missing service}"
IMAGE="${3:?missing image}"

CONTAINER_NAME="${ECS_CONTAINER_NAME:-api}"

current_arn=$(aws ecs describe-services \
    --cluster "$CLUSTER" --services "$SERVICE" \
    --query 'services[0].taskDefinition' --output text)

echo "current task definition: $current_arn"

new_definition=$(aws ecs describe-task-definition \
    --task-definition "$current_arn" \
    --query 'taskDefinition' --output json \
    | jq --arg image "$IMAGE" --arg name "$CONTAINER_NAME" '
        .containerDefinitions |= map(if .name == $name then .image = $image else . end)
        | del(.taskDefinitionArn, .revision, .status, .requiresAttributes,
              .compatibilities, .registeredAt, .registeredBy)')

jq -e --arg image "$IMAGE" --arg name "$CONTAINER_NAME" \
    'any(.containerDefinitions[]; .name == $name and .image == $image)' >/dev/null \
    <<<"$new_definition" \
    || { echo "no container named $CONTAINER_NAME in $current_arn" >&2; exit 1; }

new_arn=$(aws ecs register-task-definition \
    --cli-input-json "$new_definition" \
    --query 'taskDefinition.taskDefinitionArn' --output text)

echo "registered: $new_arn"

aws ecs update-service \
    --cluster "$CLUSTER" --service "$SERVICE" \
    --task-definition "$new_arn" \
    --output text --query 'service.serviceName' >/dev/null

# Returns only once the new tasks pass the target group health check and the old ones are gone.
# A deployment that never stabilises fails here rather than being reported as a success.
echo "waiting for the service to stabilise"
aws ecs wait services-stable --cluster "$CLUSTER" --services "$SERVICE"

echo "$SERVICE is running $IMAGE"
