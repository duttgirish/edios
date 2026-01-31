package org.iki.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.iki.engine.CelRuleEngine;
import org.iki.model.Rule;
import org.iki.service.RuleCacheService;

import java.time.Instant;
import java.util.List;

/**
 * REST endpoint for rule management and monitoring.
 */
@Path("/rules")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Rule Management", description = "Endpoints for managing and monitoring CEL rules")
public class RuleManagementResource {

    @Inject
    RuleCacheService ruleCacheService;

    @Inject
    CelRuleEngine celRuleEngine;

    @GET
    @Operation(summary = "Get cached rules", description = "Returns all currently cached CEL rules")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "List of cached rules",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Rule.class)))
    })
    public List<Rule> getCachedRules() {
        return ruleCacheService.getCachedRules();
    }

    @GET
    @Path("/stats")
    @Operation(summary = "Get rule statistics", description = "Returns cache statistics including rule counts and refresh info")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Rule cache statistics",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RuleStats.class)))
    })
    public RuleStats getStats() {
        return new RuleStats(
                ruleCacheService.getCachedRules().size(),
                celRuleEngine.getCachedRuleCount(),
                ruleCacheService.getLastRefreshTime(),
                ruleCacheService.isLastRefreshSucceeded()
        );
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh rules", description = "Forces a refresh of the rules cache from the source")
    @APIResponses({
            @APIResponse(responseCode = "202", description = "Rule refresh initiated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RefreshResponse.class)))
    })
    public Response refreshRules() {
        ruleCacheService.forceRefresh();
        return Response.accepted()
                .entity(new RefreshResponse("Rule refresh initiated"))
                .build();
    }

    public record RuleStats(int cachedRules, int compiledRules, Instant lastRefreshTime,
                            boolean lastRefreshSucceeded) {}
    public record RefreshResponse(String message) {}
}