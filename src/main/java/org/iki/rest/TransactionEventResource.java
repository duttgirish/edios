package org.iki.rest;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.iki.model.TransactionEvent;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Non-blocking REST endpoint for transaction event ingestion.
 * Immediately dispatches events to the Vert.x Event Bus for parallel processing.
 */
@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Transaction Events", description = "Endpoints for ingesting transaction events")
public class TransactionEventResource {

    private static final Logger LOG = Logger.getLogger(TransactionEventResource.class);
    static final String EVENT_BUS_ADDRESS = "transaction.process";

    @ConfigProperty(name = "app.events.max-batch-size", defaultValue = "1000")
    int maxBatchSize;

    @Inject
    EventBus eventBus;

    @POST
    @Operation(summary = "Ingest transaction events",
            description = "Accepts a batch of transaction events and dispatches each to the event bus for CEL rule evaluation. Returns immediately without waiting for processing to complete.")
    @RequestBody(description = "List of transaction events to process",
            required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TransactionEvent.class)))
    @APIResponses({
            @APIResponse(responseCode = "202", description = "Events accepted for processing",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AcceptedResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid request - empty or null events list",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response ingestEvents(List<TransactionEvent> events) {
        if (events == null || events.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Events list cannot be null or empty"))
                    .build();
        }

        if (events.size() > maxBatchSize) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "Batch size %d exceeds maximum allowed %d".formatted(events.size(), maxBatchSize)))
                    .build();
        }

        LOG.infof("Received batch of %d transaction events", events.size());

        DeliveryOptions options = new DeliveryOptions()
                .setLocalOnly(true)
                .setSendTimeout(5000);

        int dispatched = 0;
        for (TransactionEvent event : events) {
            try {
                eventBus.send(EVENT_BUS_ADDRESS, event, options);
                dispatched++;
            } catch (Exception e) {
                LOG.errorf(e, "Failed to dispatch event for CIN: %s", event.cin());
            }
        }

        LOG.infof("Dispatched %d/%d events to event bus", dispatched, events.size());

        return Response.accepted()
                .entity(new AcceptedResponse(dispatched, events.size()))
                .build();
    }

    public record AcceptedResponse(int dispatched, int total) {}
    public record ErrorResponse(String message) {}
}