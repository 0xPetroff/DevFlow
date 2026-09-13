terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source                = "hashicorp/aws"
      version               = "~> 6.0"
      configuration_aliases = [aws.us_east_1]
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

locals {
  name     = "devflow-${var.environment}"
  app_fqdn = var.subdomain == "" ? var.domain_name : "${var.subdomain}.${var.domain_name}"
  api_fqdn = "api-${var.environment}.${var.domain_name}"

  tags = merge(var.tags, {
    Project     = "devflow"
    Environment = var.environment
    ManagedBy   = "terraform"
  })
}

data "aws_route53_zone" "this" {
  zone_id = var.hosted_zone_id
}

# CloudFront only reads certificates from us-east-1, and the load balancer only reads them from
# its own region, so the same names are certified twice.
module "certificate_edge" {
  source = "../certificate"

  providers = { aws = aws.us_east_1 }

  domain_name    = local.app_fqdn
  hosted_zone_id = var.hosted_zone_id
  tags           = local.tags
}

module "certificate_regional" {
  source = "../certificate"

  domain_name    = local.api_fqdn
  hosted_zone_id = var.hosted_zone_id
  tags           = local.tags
}

resource "random_password" "origin_verify" {
  length  = 48
  special = false
}

resource "random_password" "jwt" {
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${local.name}/jwt-secret"
  description             = "HS256 signing key for DevFlow access tokens"
  recovery_window_in_days = var.secret_recovery_window_days

  tags = local.tags
}

# The application requires at least 32 bytes once decoded; 48 random characters encoded is 48.
# Rotating this invalidates every access token in flight, which is the intended blast radius.
resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = base64encode(random_password.jwt.result)
}

module "network" {
  source = "../network"

  name               = local.name
  region             = var.region
  vpc_cidr           = var.vpc_cidr
  single_nat_gateway = var.single_nat_gateway
  tags               = local.tags
}

module "api" {
  source = "../api"

  name               = local.name
  vpc_id             = module.network.vpc_id
  public_subnet_ids  = module.network.public_subnet_ids
  private_subnet_ids = module.network.private_subnet_ids

  image            = var.initial_image
  certificate_arn  = module.certificate_regional.arn
  app_url          = "https://${local.app_fqdn}"
  cpu_architecture = var.cpu_architecture

  task_cpu      = var.task_cpu
  task_memory   = var.task_memory
  desired_count = var.desired_count
  min_capacity  = var.min_capacity
  max_capacity  = var.max_capacity

  origin_verify_secret = random_password.origin_verify.result
  database_secret_arn  = module.database.secret_arn
  jwt_secret_arn       = aws_secretsmanager_secret.jwt.arn
  secret_arns          = [module.database.secret_arn, aws_secretsmanager_secret.jwt.arn]

  log_retention_days  = var.log_retention_days
  container_insights  = var.container_insights
  deletion_protection = var.deletion_protection

  tags = local.tags
}

module "database" {
  source = "../database"

  name       = local.name
  vpc_id     = module.network.vpc_id
  subnet_ids = module.network.private_subnet_ids

  # The database's only ingress rule names this group, so the API tasks are the one thing that
  # can reach port 5432 and no CIDR list can widen it.
  api_security_group_id = module.api.task_security_group_id

  instance_class               = var.db_instance_class
  multi_az                     = var.db_multi_az
  backup_retention_days        = var.db_backup_retention_days
  performance_insights_enabled = var.db_performance_insights
  deletion_protection          = var.deletion_protection
  secret_recovery_window_days  = var.secret_recovery_window_days

  tags = local.tags
}

module "web" {
  source = "../web"

  providers = { aws = aws.us_east_1 }

  name            = local.name
  bucket_name     = "${local.name}-web-${var.account_id}"
  aliases         = [local.app_fqdn]
  certificate_arn = module.certificate_edge.arn
  api_domain_name = local.api_fqdn
  price_class     = var.cloudfront_price_class
  force_destroy   = !var.deletion_protection

  origin_verify_secret = random_password.origin_verify.result

  tags = local.tags
}

resource "aws_route53_record" "app" {
  zone_id = data.aws_route53_zone.this.zone_id
  name    = local.app_fqdn
  type    = "A"

  alias {
    name                   = module.web.distribution_domain_name
    zone_id                = module.web.distribution_hosted_zone_id
    evaluate_target_health = false
  }
}

# The API has a name of its own so CloudFront has something to forward to, and so the
# certificate on the load balancer can be validated. Nothing is expected to call it directly:
# the listener refuses anything without CloudFront's shared secret.
resource "aws_route53_record" "api" {
  zone_id = data.aws_route53_zone.this.zone_id
  name    = local.api_fqdn
  type    = "A"

  alias {
    name                   = module.api.alb_dns_name
    zone_id                = module.api.alb_zone_id
    evaluate_target_health = true
  }
}
