output "bucket_name" {
  description = "Bucket deploy-web.sh syncs into"
  value       = aws_s3_bucket.this.id
}

output "distribution_id" {
  description = "Distribution deploy-web.sh invalidates"
  value       = aws_cloudfront_distribution.this.id
}

output "distribution_domain_name" {
  description = "CloudFront hostname, for an alias record"
  value       = aws_cloudfront_distribution.this.domain_name
}

output "distribution_hosted_zone_id" {
  description = "CloudFront's hosted zone, fixed for every distribution"
  value       = aws_cloudfront_distribution.this.hosted_zone_id
}
