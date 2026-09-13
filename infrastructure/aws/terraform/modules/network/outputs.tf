output "vpc_id" {
  description = "Id of the VPC"
  value       = aws_vpc.this.id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC"
  value       = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  description = "Subnets holding the load balancer and the NAT gateways"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Subnets holding the API tasks and the database"
  value       = aws_subnet.private[*].id
}

output "availability_zones" {
  description = "Zones the subnets are spread across"
  value       = local.azs
}
