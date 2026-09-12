output "app_url" {
  description = "DEPLOY_BASE_URL for the pipeline"
  value       = module.environment.app_url
}

output "ecs_cluster" {
  description = "First argument to deploy-ecs.sh"
  value       = module.environment.ecs_cluster
}

output "ecs_service" {
  description = "Second argument to deploy-ecs.sh"
  value       = module.environment.ecs_service
}

output "ecr_repository_url" {
  description = "Repository the pipeline pushes to"
  value       = module.environment.ecr_repository_url
}

output "web_bucket" {
  description = "WEB_BUCKET for deploy-web.sh"
  value       = module.environment.web_bucket
}

output "cloudfront_distribution_id" {
  description = "CLOUDFRONT_DISTRIBUTION_ID for deploy-web.sh"
  value       = module.environment.cloudfront_distribution_id
}
