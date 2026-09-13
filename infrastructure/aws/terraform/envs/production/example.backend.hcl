# Copy to backend.hcl and fill in: tofu init -backend-config=backend.hcl
#
# The bucket and the lock table are not created here. State that describes the environment it
# lives in cannot be bootstrapped by that environment.

bucket       = "devflow-terraform-state-000000000000"
key          = "production/terraform.tfstate"
region       = "eu-central-1"
encrypt      = true
use_lockfile = true
