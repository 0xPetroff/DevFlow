variable "domain_name" {
  description = "Name the certificate is issued for"
  type        = string
}

variable "hosted_zone_id" {
  description = "Route53 zone the validation records are written into"
  type        = string
}

variable "tags" {
  description = "Tags applied to the certificate"
  type        = map(string)
  default     = {}
}
