variable "name" {
  description = "Name prefix, also the ECS cluster name"
  type        = string
}

variable "vpc_id" {
  description = "VPC the service runs in"
  type        = string
}

variable "public_subnet_ids" {
  description = "Subnets for the load balancer"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "Subnets for the tasks, which have no public address"
  type        = list(string)
}

variable "ecr_repository_name" {
  description = "Repository the pipeline pushes the API image to"
  type        = string
  default     = "devflow/backend"
}

variable "image" {
  description = "Image for the initial task definition. Later releases are the pipeline's, and the service ignores changes to this"
  type        = string
}

variable "container_port" {
  description = "Port the application listens on"
  type        = number
  default     = 8080
}

variable "task_cpu" {
  description = "Fargate CPU units"
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Fargate memory in MiB"
  type        = number
  default     = 1024
}

variable "cpu_architecture" {
  description = "X86_64 or ARM64. Graviton is cheaper and the image is built multi-arch"
  type        = string
  default     = "ARM64"

  validation {
    condition     = contains(["X86_64", "ARM64"], var.cpu_architecture)
    error_message = "Fargate supports X86_64 and ARM64."
  }
}

variable "desired_count" {
  description = "Tasks to run before autoscaling adjusts it"
  type        = number
  default     = 2
}

variable "min_capacity" {
  description = "Floor for autoscaling"
  type        = number
  default     = 1
}

variable "max_capacity" {
  description = "Ceiling for autoscaling"
  type        = number
  default     = 6
}

variable "certificate_arn" {
  description = "ACM certificate for the load balancer, issued in this region"
  type        = string
}

variable "app_url" {
  description = "Public origin of the app, used as the CORS allowlist. The SPA is served from it, so in practice nothing needs the grant"
  type        = string
}

variable "origin_verify_secret" {
  description = "Shared secret CloudFront sends as X-Origin-Verify. Requests without it are refused at the listener"
  type        = string
  sensitive   = true
}

variable "database_secret_arn" {
  description = "Secret holding the database credentials"
  type        = string
}

variable "jwt_secret_arn" {
  description = "Secret holding the JWT signing key"
  type        = string
}

variable "secret_arns" {
  description = "Every secret the execution role may read"
  type        = list(string)
}

variable "log_retention_days" {
  description = "CloudWatch log retention"
  type        = number
  default     = 30
}

variable "container_insights" {
  description = "Enable Container Insights on the cluster"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Refuse to delete the load balancer"
  type        = bool
  default     = true
}

variable "tags" {
  description = "Tags applied to every resource"
  type        = map(string)
  default     = {}
}
