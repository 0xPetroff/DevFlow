package com.devflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Restores the demo data on every start, so a public demo comes back to a known state after
 * visitors have had their way with it.
 *
 * <p>Not a Flyway migration: a versioned one would take a number out of the schema's own
 * sequence and block the next real migration below it, and a repeatable one re-runs only when
 * its checksum changes rather than on every start.
 *
 * <p>The script deletes the seeded rows by their fixed ids before inserting them, so it never
 * issues an unscoped DELETE and anything a visitor created alongside the demo data survives.
 * Flyway has already migrated by the time this runs.
 */
@Component
@Profile("demo")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final String SCRIPT = "demo/demo-data.sql";

    private final DataSource dataSource;

    public DemoDataLoader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("Demo profile active: resetting demo data and publishing a known password. "
                + "This must never run on an installation holding real data.");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(SCRIPT));
        // One transaction, so a failure half way leaves no partial demo behind.
        populator.setContinueOnError(false);
        populator.execute(dataSource);

        log.info("Demo data restored from {}", SCRIPT);
    }
}
