-- Deleting a project cascades to both environments and deployments, but PostgreSQL fires those
-- cascades in constraint-creation order: environments go first, and RESTRICT is checked the moment
-- that row is deleted, while its deployments are still present. The delete therefore failed for any
-- project that had both.
--
-- NO ACTION is the fix rather than CASCADE. It still refuses to delete an environment that has
-- deployment history, which is the rule worth keeping, but the check is deferred to the end of the
-- statement, by which point the project's own cascade has already removed the deployments.
ALTER TABLE deployments DROP CONSTRAINT fk_deployments_environment;

ALTER TABLE deployments
    ADD CONSTRAINT fk_deployments_environment
    FOREIGN KEY (environment_id) REFERENCES environments (id);
