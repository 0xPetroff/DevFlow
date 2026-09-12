output "app_url" {
  description = "Public address of the application, and DEPLOY_BASE_URL for the pipeline"
  value       = "https://${local.app_fqdn}"
}

output "ecs_cluster" {
  description = "First argument to deploy-ecs.sh"
  value       = module.api.cluster_name
}

output "ecs_service" {
  description = "Second argument to deploy-ecs.sh"
  value       = module.api.service_name
}

output "ecr_repository_url" {
  description = "Repository the pipeline pushes the API image to"
  value       = module.api.ecr_repository_url
}

output "web_bucket" {
  description = "WEB_BUCKET for deploy-web.sh"
  value       = module.web.bucket_name
}

output "cloudfront_distribution_id" {
  description = "CLOUDFRONT_DISTRIBUTION_ID for deploy-web.sh"
  value       = module.web.distribution_id
}

output "database_endpoint" {
  description = "Host and port of the database, reachable only from the API tasks"
  value       = module.database.endpoint
}

output "log_group" {
  description = "CloudWatch log group holding the API logs"
  value       = module.api.log_group_name
}
