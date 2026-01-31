package org.iki.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.iki.engine.CelRuleEngine;
import org.iki.service.RuleCacheService;

import java.time.Instant;

/**
 * Readiness health check for the rule cache.
 * Reports unhealthy if no rules are loaded or compiled.
 */
@Readiness
@ApplicationScoped
public class RuleCacheHealthCheck implements HealthCheck {

    @Inject
    RuleCacheService ruleCacheService;

    @Inject
    CelRuleEngine celRuleEngine;

    @Override
    public HealthCheckResponse call() {
        int cachedRules = ruleCacheService.getCachedRules().size();
        int compiledRules = celRuleEngine.getCachedRuleCount();
        Instant lastRefresh = ruleCacheService.getLastRefreshTime();
        boolean refreshOk = ruleCacheService.isLastRefreshSucceeded();

        var builder = HealthCheckResponse.named("rule-cache")
                .withData("cachedRules", cachedRules)
                .withData("compiledRules", compiledRules)
                .withData("lastRefreshSucceeded", refreshOk);

        if (lastRefresh != null) {
            builder.withData("lastRefreshTime", lastRefresh.toString());
        }

        if (cachedRules > 0 && compiledRules > 0) {
            return builder.up().build();
        }

        return builder.down()
                .withData("reason", cachedRules == 0
                        ? "No rules loaded"
                        : "Rules loaded but none compiled successfully")
                .build();
    }
}