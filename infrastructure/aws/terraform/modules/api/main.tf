terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

data "aws_region" "current" {}

locals {
  container_name = "api"
}

resource "aws_ecr_repository" "this" {
  name                 = var.ecr_repository_name
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = var.tags
}

# Every release tags by commit, so untagged images are only ever the leftovers of a rebuilt
# layer. Thirty tagged images is a few weeks of releases and enough to roll back through.
resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images"
        selection    = { tagStatus = "untagged", countType = "sinceImagePushed", countUnit = "days", countNumber = 7 }
        action       = { type = "expire" }
      },
      {
        rulePriority = 2
        description  = "Keep the last 30 releases"
        selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 30 }
        action       = { type = "expire" }
      }
    ]
  })
}

resource "aws_cloudwatch_log_group" "this" {
  name              = "/devflow/${var.name}/api"
  retention_in_days = var.log_retention_days

  tags = var.tags
}

resource "aws_security_group" "alb" {
  name        = "${var.name}-alb"
  description = "Load balancer for the DevFlow API"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, { Name = "${var.name}-alb" })
}

# The load balancer answers CloudFront, not the internet at large. The managed prefix list is
# the only way to say that without tracking CloudFront's address ranges by hand.
data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_vpc_security_group_ingress_rule" "alb_from_cloudfront" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTPS from CloudFront only"

  prefix_list_id = data.aws_ec2_managed_prefix_list.cloudfront.id
  from_port      = 443
  to_port        = 443
  ip_protocol    = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "alb_to_tasks" {
  security_group_id = aws_security_group.alb.id
  description       = "Forward to the API tasks"

  referenced_security_group_id = aws_security_group.tasks.id
  from_port                    = var.container_port
  to_port                      = var.container_port
  ip_protocol                  = "tcp"
}

resource "aws_security_group" "tasks" {
  name        = "${var.name}-tasks"
  description = "DevFlow API tasks"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, { Name = "${var.name}-tasks" })
}

resource "aws_vpc_security_group_ingress_rule" "tasks_from_alb" {
  security_group_id = aws_security_group.tasks.id
  description       = "Traffic from the load balancer"

  referenced_security_group_id = aws_security_group.alb.id
  from_port                    = var.container_port
  to_port                      = var.container_port
  ip_protocol                  = "tcp"
}

# Outbound is open because a task pulls its image, reads its secret and writes its logs. The
# interface endpoints keep that traffic inside the VPC; this rule is what lets it leave at all.
resource "aws_vpc_security_group_egress_rule" "tasks_egress" {
  security_group_id = aws_security_group.tasks.id
  description       = "Image pulls, logs, Secrets Manager and the database"

  cidr_ipv4   = "0.0.0.0/0"
  ip_protocol = "-1"
}

resource "aws_lb" "this" {
  name               = "${var.name}-api"
  load_balancer_type = "application"
  internal           = false
  subnets            = var.public_subnet_ids
  security_groups    = [aws_security_group.alb.id]

  drop_invalid_header_fields = true
  enable_deletion_protection = var.deletion_protection

  tags = merge(var.tags, { Name = "${var.name}-api" })
}

resource "aws_lb_target_group" "this" {
  name        = "${var.name}-api"
  port        = var.container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = var.vpc_id

  # Readiness, not liveness: it reports the database as well, so a task with no working
  # connection pool never joins the target group.
  health_check {
    path                = "/actuator/health/readiness"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  # Long enough for an in-flight request to finish, short enough that a release is not held up
  # by idle connections. Graceful shutdown inside the container is configured to match.
  deregistration_delay = 30

  lifecycle {
    create_before_destroy = true
  }

  tags = var.tags
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.certificate_arn

  # A request that reaches the load balancer without CloudFront's shared secret is refused
  # here, so the origin cannot be addressed directly even by something inside the prefix list.
  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "application/json"
      status_code  = "403"
      message_body = jsonencode({ title = "Forbidden", status = 403 })
    }
  }

  tags = var.tags
}

resource "aws_lb_listener_rule" "from_cloudfront" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100

  condition {
    http_header {
      http_header_name = "X-Origin-Verify"
      values           = [var.origin_verify_secret]
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }

  tags = var.tags
}

resource "aws_iam_role" "execution" {
  name = "${var.name}-api-execution"

  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
  tags               = var.tags
}

data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# The managed policy covers ECR and logs but not Secrets Manager, and the execution role is what
# resolves a secret into the container's environment before the process starts.
resource "aws_iam_role_policy" "execution_secrets" {
  name = "read-secrets"
  role = aws_iam_role.execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = var.secret_arns
    }]
  })
}

# Deliberately holds no policy. The application talks to PostgreSQL and nothing else in AWS, so
# the role exists to be attached, to name the task in CloudTrail, and to have somewhere to add a
# permission the day one is genuinely needed.
resource "aws_iam_role" "task" {
  name = "${var.name}-api-task"

  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
  tags               = var.tags
}

resource "aws_ecs_cluster" "this" {
  name = var.name

  setting {
    name  = "containerInsights"
    value = var.container_insights ? "enabled" : "disabled"
  }

  tags = var.tags
}

resource "aws_ecs_task_definition" "this" {
  family                   = "${var.name}-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = var.cpu_architecture
  }

  container_definitions = jsonencode([{
    name      = local.container_name
    image     = var.image
    essential = true

    portMappings = [{
      containerPort = var.container_port
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "DEVFLOW_CORS_ALLOWED_ORIGINS", value = var.app_url },
      { name = "JAVA_TOOL_OPTIONS", value = "-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" }
    ]

    # Resolved by the execution role at start, so no secret is ever in the task definition, in
    # a plan, or in the pipeline that deploys it.
    secrets = [
      { name = "SPRING_DATASOURCE_URL", valueFrom = "${var.database_secret_arn}:url::" },
      { name = "SPRING_DATASOURCE_USERNAME", valueFrom = "${var.database_secret_arn}:username::" },
      { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = "${var.database_secret_arn}:password::" },
      { name = "DEVFLOW_JWT_SECRET", valueFrom = var.jwt_secret_arn }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.this.name
        "awslogs-region"        = data.aws_region.current.region
        "awslogs-stream-prefix" = "api"
      }
    }

    # The target group decides what receives traffic; this is what lets ECS replace a container
    # that is still running but no longer answering.
    healthCheck = {
      command     = ["CMD-SHELL", "curl -fsS http://127.0.0.1:${var.container_port}/actuator/health/liveness || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = var.tags
}

resource "aws_ecs_service" "this" {
  name            = "${var.name}-api"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  # Migrations run at start, so two versions of the application share a schema for the length of
  # a release. Flyway's migrations are additive for exactly this reason.
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.this.arn
    container_name   = local.container_name
    container_port   = var.container_port
  }

  # Flyway holds a lock while it migrates, so a task that starts slowly on a release is normal
  # and should not be killed as unhealthy.
  health_check_grace_period_seconds = 120

  # The pipeline registers a new revision and points the service at it, which is the one piece
  # of this service Terraform does not own.
  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }

  depends_on = [aws_lb_listener_rule.from_cloudfront]

  tags = var.tags
}

resource "aws_appautoscaling_target" "this" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.this.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = var.min_capacity
  max_capacity       = var.max_capacity
}

resource "aws_appautoscaling_policy" "cpu" {
  name               = "${var.name}-api-cpu"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.this.service_namespace
  resource_id        = aws_appautoscaling_target.this.resource_id
  scalable_dimension = aws_appautoscaling_target.this.scalable_dimension

  target_tracking_scaling_policy_configuration {
    target_value = 65

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    # Slow to scale in, quick to scale out: a JVM takes a while to start and serve, so shedding
    # a task that is about to be needed again costs more than keeping it.
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}
