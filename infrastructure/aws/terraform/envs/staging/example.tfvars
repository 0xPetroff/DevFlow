# Copy to staging.tfvars and fill in. Real tfvars are gitignored.
#
#   tofu init -backend-config=backend.hcl
#   tofu plan -var-file=staging.tfvars

region         = "eu-central-1"
account_id     = "000000000000"
domain_name    = "devflow.example.com"
hosted_zone_id = "Z0000000000000000000"

# Only the first task definition. Every release after it is the pipeline's, and the service
# ignores changes to this value.
initial_image = "000000000000.dkr.ecr.eu-central-1.amazonaws.com/devflow/backend:bootstrap"
