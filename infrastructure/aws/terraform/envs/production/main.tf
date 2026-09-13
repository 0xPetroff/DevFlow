terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # Filled in with -backend-config, so the same root can be initialised against a different
  # account without editing a tracked file.
  backend "s3" {}
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "devflow"
      Environment = "production"
      ManagedBy   = "terraform"
    }
  }
}

# CloudFront and its certificate are only ever read from us-east-1, whatever region the rest of
# the environment runs in.
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"

  default_tags {
    tags = {
      Project     = "devflow"
      Environment = "production"
      ManagedBy   = "terraform"
    }
  }
}

module "environment" {
  source = "../../modules/environment"

  providers = {
    aws           = aws
    aws.us_east_1 = aws.us_east_1
  }

  environment = "production"
  region      = var.region
  account_id  = var.account_id

  domain_name    = var.domain_name
  subdomain      = ""
  hosted_zone_id = var.hosted_zone_id
  initial_image  = var.initial_image

  vpc_cidr = "10.20.0.0/16"

  # A NAT gateway per zone and an RDS standby: the two places where losing one availability
  # zone would otherwise take the whole environment with it.
  single_nat_gateway       = false
  desired_count            = 2
  min_capacity             = 2
  max_capacity             = 6
  task_cpu                 = 1024
  task_memory              = 2048
  db_instance_class        = "db.t4g.small"
  db_multi_az              = true
  db_backup_retention_days = 14
  db_performance_insights  = true
  container_insights       = true
  log_retention_days       = 90
  cloudfront_price_class   = "PriceClass_200"

  deletion_protection         = true
  secret_recovery_window_days = 30
}
