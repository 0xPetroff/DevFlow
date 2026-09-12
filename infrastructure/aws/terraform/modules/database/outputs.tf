output "endpoint" {
  description = "Host and port of the instance"
  value       = aws_db_instance.this.endpoint
}

output "security_group_id" {
  description = "The database's security group"
  value       = aws_security_group.this.id
}

output "secret_arn" {
  description = "Secret holding the credentials and the JDBC URL, read by the API task at start"
  value       = aws_secretsmanager_secret.database.arn
}

output "database_name" {
  description = "Name of the database"
  value       = aws_db_instance.this.db_name
}
