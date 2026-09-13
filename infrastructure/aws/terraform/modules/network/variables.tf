variable "name" {
  description = "Name prefix for every resource in this network"
  type        = string
}

variable "region" {
  description = "Region the VPC endpoints are created in"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC, large enough to be split into /20 subnets"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zone_count" {
  description = "How many availability zones to spread the subnets across"
  type        = number
  default     = 2

  validation {
    condition     = var.availability_zone_count >= 2 && var.availability_zone_count <= 3
    error_message = "RDS and the load balancer both need at least two zones, and three is the most this layout allocates."
  }
}

variable "single_nat_gateway" {
  description = "Run one NAT gateway instead of one per zone. Cheaper, and a single point of failure for egress"
  type        = bool
  default     = false
}

variable "tags" {
  description = "Tags applied to every resource"
  type        = map(string)
  default     = {}
}
