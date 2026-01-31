package org.iki.service;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.iki.engine.CelRuleEngine;
import org.iki.model.Rule;
import org.iki.repository.RuleRepository;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service responsible for caching rules in-memory and periodically refreshing them.
 */
@ApplicationScoped
public class RuleCacheService {

    private static final Logger LOG = Logger.getLogger(RuleCacheService.class);

    private final AtomicReference<List<Rule>> cachedRules = new AtomicReference<>(Collections.emptyList());
    private volatile Instant lastRefreshTime;
    private volatile boolean lastRefreshSucceeded;

    @Inject
    RuleRepository ruleRepository;

    @Inject
    CelRuleEngine celRuleEngine;

    /**
     * Load rules on application startup.
     */
    void onStart(@Observes StartupEvent event) {
        LOG.info("Loading rules on application startup");
        refreshRules();
    }

    /**
     * Periodically refresh rules from the source.
     * Default: every 60 seconds.
     */
    @Scheduled(every = "${app.rules.refresh-interval:60s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void scheduledRefresh() {
        LOG.debug("Scheduled rule refresh triggered");
        refreshRules();
    }

    /**
     * Refresh rules from the repository and recompile CEL expressions.
     */
    public void refreshRules() {
        ruleRepository.findAllActiveRules()
                .subscribe().with(
                        rules -> {
                            List<Rule> immutableRules = List.copyOf(rules);
                            cachedRules.set(immutableRules);
                            int compiled = celRuleEngine.compileAndCacheRules(immutableRules);
                            lastRefreshTime = Instant.now();
                            lastRefreshSucceeded = true;
                            LOG.infof("Rules cache refreshed: %d rules loaded, %d compiled",
                                    immutableRules.size(), compiled);
                        },
                        error -> {
                            lastRefreshSucceeded = false;
                            LOG.error("Failed to refresh rules cache", error);
                        }
                );
    }

    /**
     * Get the currently cached rules (unmodifiable).
     *
     * @return List of cached rules
     */
    public List<Rule> getCachedRules() {
        return cachedRules.get();
    }

    /**
     * Force an immediate refresh of the rules cache.
     */
    public void forceRefresh() {
        LOG.info("Forcing rules cache refresh");
        refreshRules();
    }

    /**
     * Returns the time of the last successful or failed refresh attempt.
     */
    public Instant getLastRefreshTime() {
        return lastRefreshTime;
    }

    /**
     * Returns whether the last refresh attempt succeeded.
     */
    public boolean isLastRefreshSucceeded() {
        return lastRefreshSucceeded;
    }
}