output "cluster_name" {
  description = "ECS cluster, the first argument to deploy-ecs.sh"
  value       = aws_ecs_cluster.this.name
}

output "service_name" {
  description = "ECS service, the second argument to deploy-ecs.sh"
  value       = aws_ecs_service.this.name
}

output "container_name" {
  description = "Container the pipeline swaps the image on"
  value       = local.container_name
}

output "alb_dns_name" {
  description = "Origin hostname CloudFront forwards /api to"
  value       = aws_lb.this.dns_name
}

output "alb_zone_id" {
  description = "Hosted zone of the load balancer, for an alias record"
  value       = aws_lb.this.zone_id
}

output "task_security_group_id" {
  description = "Security group the database grants access to"
  value       = aws_security_group.tasks.id
}

output "ecr_repository_url" {
  description = "Repository the pipeline pushes to"
  value       = aws_ecr_repository.this.repository_url
}

output "log_group_name" {
  description = "CloudWatch log group holding the application logs"
  value       = aws_cloudwatch_log_group.this.name
}
