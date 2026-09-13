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
      Environment = "staging"
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
      Environment = "staging"
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

  environment = "staging"
  region      = var.region
  account_id  = var.account_id

  domain_name    = var.domain_name
  subdomain      = "staging"
  hosted_zone_id = var.hosted_zone_id
  initial_image  = var.initial_image

  vpc_cidr = "10.10.0.0/16"

  # Staging exists to catch mistakes before production sees them, not to survive an outage, so
  # it runs the cheap shape: one task, one NAT gateway, no standby.
  single_nat_gateway       = true
  desired_count            = 1
  min_capacity             = 1
  max_capacity             = 2
  task_cpu                 = 512
  task_memory              = 1024
  db_instance_class        = "db.t4g.micro"
  db_multi_az              = false
  db_backup_retention_days = 1
  log_retention_days       = 7

  # Staging is rebuilt often enough that protection would only ever be an obstacle, and a
  # rebuild under the same names cannot wait a week for a deleted secret to age out.
  deletion_protection         = false
  secret_recovery_window_days = 0
}
