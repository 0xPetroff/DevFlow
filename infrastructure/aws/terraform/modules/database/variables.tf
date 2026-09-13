variable "name" {
  description = "Name prefix for the instance and its secret"
  type        = string
}

variable "vpc_id" {
  description = "VPC the database lives in"
  type        = string
}

variable "subnet_ids" {
  description = "Private subnets for the subnet group"
  type        = list(string)
}

variable "api_security_group_id" {
  description = "The only security group allowed to reach port 5432"
  type        = string
}

variable "engine_version" {
  description = "PostgreSQL version, matching what the application is developed against"
  type        = string
  default     = "17.4"
}

variable "parameter_group_family" {
  description = "Parameter group family, which has to match the engine's major version"
  type        = string
  default     = "postgres17"
}

variable "instance_class" {
  description = "Instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "Initial storage in GiB"
  type        = number
  default     = 20
}

variable "max_allocated_storage" {
  description = "Ceiling for storage autoscaling in GiB"
  type        = number
  default     = 100
}

variable "database_name" {
  description = "Name of the database Flyway migrates"
  type        = string
  default     = "devflow"
}

variable "master_username" {
  description = "Master user"
  type        = string
  default     = "devflow"
}

variable "multi_az" {
  description = "Run a standby in a second availability zone"
  type        = bool
  default     = false
}

variable "backup_retention_days" {
  description = "How many days of automated backups to keep"
  type        = number
  default     = 7

  validation {
    condition     = var.backup_retention_days >= 1
    error_message = "Zero disables automated backups entirely, which is never what an environment holding real data wants."
  }
}

variable "performance_insights_enabled" {
  description = "Enable Performance Insights"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Refuse to delete the instance, and take a final snapshot when it is deleted"
  type        = bool
  default     = true
}

variable "secret_recovery_window_days" {
  description = "Days Secrets Manager keeps a deleted secret recoverable. Zero deletes immediately"
  type        = number
  default     = 7
}

variable "tags" {
  description = "Tags applied to every resource"
  type        = map(string)
  default     = {}
}
