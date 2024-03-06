package gov.cdc.dex.validation.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import io.micronaut.management.health.indicator.annotation.Liveness;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

@Singleton
// Only create bean when configuration property
// endpoints.health.version.enabled equals true,
// and HealthEndpoint bean to expose /health endpoint is available.
@Requires(property = HealthEndpoint.PREFIX + ".version.enabled", value = "true")
@Requires(beans = HealthEndpoint.class)
@Liveness
public class HealthVersionIndicator<Map> extends AbstractHealthIndicator<Map> {
    public static final String NAME = "application";
    @Override
    protected Map getHealthInformation() {
        HashMap<String, String> info = new HashMap<>();
        try {
            this.healthStatus = HealthStatus.UP;
            String version = this.getVersion();
            info.put("version", version);
        } catch (IOException e) {
            this.healthStatus = HealthStatus.UNKNOWN;
            info.put("exception", e.getMessage());
        }

        return (Map)info;

    }

    @Override
    protected String getName() {
        return NAME;
    }

    private String getVersion() throws IOException {
        java.io.InputStream input = this.getClass().getClassLoader().getResourceAsStream("project.properties");
        java.util.Properties props = new Properties();
        props.load(input);
        return props.getProperty("app.version");
    }
}
