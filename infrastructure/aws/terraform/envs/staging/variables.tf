variable "region" {
  description = "Region the environment runs in"
  type        = string
  default     = "eu-central-1"
}

variable "account_id" {
  description = "Account id, used to make the web bucket name globally unique"
  type        = string
}

variable "domain_name" {
  description = "Root domain. The app is served from staging.<domain_name>"
  type        = string
}

variable "hosted_zone_id" {
  description = "Route53 zone holding the domain"
  type        = string
}

variable "initial_image" {
  description = "Image for the first task definition, before the pipeline has pushed a release"
  type        = string
}
