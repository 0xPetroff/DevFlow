variable "environment" {
  description = "Environment name, which also names every resource and the DevFlow environment the pipeline deploys to"
  type        = string

  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "DevFlow's environments are staging and production."
  }
}

variable "region" {
  description = "Region everything but CloudFront and its certificate runs in"
  type        = string
}

variable "account_id" {
  description = "Account id, used to make the bucket name globally unique"
  type        = string
}

variable "domain_name" {
  description = "Root domain, for example devflow.example.com"
  type        = string
}

variable "subdomain" {
  description = "Subdomain the app answers on. Empty serves it at the root of the domain"
  type        = string
  default     = ""
}

variable "hosted_zone_id" {
  description = "Route53 zone holding the domain"
  type        = string
}

variable "initial_image" {
  description = "Image for the first task definition. Every release after it comes from the pipeline"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR for the VPC. Staging and production must not overlap if they are ever peered"
  type        = string
}

variable "single_nat_gateway" {
  description = "Run one NAT gateway rather than one per zone"
  type        = bool
  default     = false
}

variable "cpu_architecture" {
  description = "X86_64 or ARM64"
  type        = string
  default     = "ARM64"
}

variable "task_cpu" {
  description = "Fargate CPU units per task"
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Fargate memory per task, in MiB"
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Tasks to run"
  type        = number
  default     = 2
}

variable "min_capacity" {
  description = "Autoscaling floor"
  type        = number
  default     = 1
}

variable "max_capacity" {
  description = "Autoscaling ceiling"
  type        = number
  default     = 6
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_multi_az" {
  description = "Run an RDS standby in a second zone"
  type        = bool
  default     = false
}

variable "db_backup_retention_days" {
  description = "Days of automated backups to keep"
  type        = number
  default     = 7
}

variable "db_performance_insights" {
  description = "Enable Performance Insights on the database"
  type        = bool
  default     = false
}

variable "cloudfront_price_class" {
  description = "Edge locations the distribution uses"
  type        = string
  default     = "PriceClass_100"
}

variable "log_retention_days" {
  description = "CloudWatch log retention for the API"
  type        = number
  default     = 30
}

variable "container_insights" {
  description = "Enable Container Insights on the ECS cluster"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Protect the database and the load balancer, and keep the bucket from being emptied on destroy"
  type        = bool
  default     = true
}

variable "secret_recovery_window_days" {
  description = "Days a deleted secret stays recoverable. Zero lets a torn-down environment be rebuilt under the same names immediately"
  type        = number
  default     = 7
}

variable "tags" {
  description = "Extra tags, merged with the project, environment and ownership tags"
  type        = map(string)
  default     = {}
}
