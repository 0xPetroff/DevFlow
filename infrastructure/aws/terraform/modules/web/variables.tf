variable "name" {
  description = "Name prefix for the distribution's function and policies"
  type        = string
}

variable "bucket_name" {
  description = "Bucket the pipeline syncs the built SPA into. Globally unique"
  type        = string
}

variable "aliases" {
  description = "Domain names the distribution answers on"
  type        = list(string)
  default     = []
}

variable "certificate_arn" {
  description = "ACM certificate for the aliases. CloudFront only reads certificates from us-east-1"
  type        = string
}

variable "api_domain_name" {
  description = "Hostname of the load balancer serving /api"
  type        = string
}

variable "origin_verify_secret" {
  description = "Shared secret sent to the load balancer as X-Origin-Verify"
  type        = string
  sensitive   = true
}

variable "price_class" {
  description = "Edge locations to use"
  type        = string
  default     = "PriceClass_100"
}

variable "force_destroy" {
  description = "Allow the bucket to be deleted while it still holds objects"
  type        = bool
  default     = false
}

variable "tags" {
  description = "Tags applied to every resource"
  type        = map(string)
  default     = {}
}
