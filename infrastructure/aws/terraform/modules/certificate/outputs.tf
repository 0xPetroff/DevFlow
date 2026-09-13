output "arn" {
  description = "ARN of the issued certificate, available only once ACM has validated it"
  value       = aws_acm_certificate_validation.this.certificate_arn
}
