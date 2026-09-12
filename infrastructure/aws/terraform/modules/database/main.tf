terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

resource "aws_db_subnet_group" "this" {
  name       = var.name
  subnet_ids = var.subnet_ids

  tags = merge(var.tags, { Name = var.name })
}

# No ingress rule of its own: the only rule is the one the API's security group is granted
# below, so the database is reachable from exactly one place and nothing can widen that by
# editing a CIDR list.
resource "aws_security_group" "this" {
  name        = "${var.name}-db"
  description = "PostgreSQL, reachable only from the API tasks"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, { Name = "${var.name}-db" })
}

resource "aws_vpc_security_group_ingress_rule" "from_api" {
  security_group_id = aws_security_group.this.id
  description       = "PostgreSQL from the API tasks"

  referenced_security_group_id = var.api_security_group_id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "random_password" "master" {
  length  = 40
  special = false
}

# Rotation is left to Secrets Manager rather than Terraform: a password rotated in state is a
# password in every plan output and every state file that ever held it.
resource "aws_secretsmanager_secret" "database" {
  name                    = "${var.name}/database"
  description             = "DevFlow database credentials and connection string"
  recovery_window_in_days = var.secret_recovery_window_days

  tags = var.tags
}

resource "aws_secretsmanager_secret_version" "database" {
  secret_id = aws_secretsmanager_secret.database.id

  secret_string = jsonencode({
    username = var.master_username
    password = random_password.master.result
    host     = aws_db_instance.this.address
    port     = aws_db_instance.this.port
    dbname   = var.database_name
    url      = "jdbc:postgresql://${aws_db_instance.this.endpoint}/${var.database_name}"
  })
}

resource "aws_db_parameter_group" "this" {
  name   = var.name
  family = var.parameter_group_family

  parameter {
    name  = "rds.force_ssl"
    value = "1"
  }

  # Anything slower than a second is worth seeing. The dashboard's aggregates are the queries
  # most likely to appear here first.
  parameter {
    name  = "log_min_duration_statement"
    value = "1000"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_db_instance" "this" {
  identifier     = var.name
  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.database_name
  username = var.master_username
  password = random_password.master.result
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.this.id]
  parameter_group_name   = aws_db_parameter_group.this.name
  publicly_accessible    = false

  multi_az                = var.multi_az
  backup_retention_period = var.backup_retention_days
  backup_window           = "02:00-03:00"
  maintenance_window      = "sun:03:30-sun:04:30"

  # Flyway owns the schema, so a minor engine upgrade is never coordinated with a release.
  auto_minor_version_upgrade = true
  apply_immediately          = false

  performance_insights_enabled    = var.performance_insights_enabled
  enabled_cloudwatch_logs_exports = ["postgresql"]

  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = !var.deletion_protection
  final_snapshot_identifier = var.deletion_protection ? "${var.name}-final" : null

  tags = merge(var.tags, { Name = var.name })
}
