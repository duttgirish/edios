package org.iki.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Global exception mappers for REST endpoints.
 */
public class ExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ExceptionMappers.class);

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapIllegalArgumentException(IllegalArgumentException e) {
        LOG.warnf("Bad request: %s", e.getMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST,
                new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapJsonProcessingException(JsonProcessingException e) {
        LOG.warnf("Malformed JSON: %s", e.getOriginalMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST,
                new ErrorResponse("BAD_REQUEST", "Malformed JSON in request body"));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapNullPointerException(NullPointerException e) {
        LOG.errorf(e, "Null pointer: %s", e.getMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST,
                new ErrorResponse("BAD_REQUEST", "Missing required field in request"));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapGenericException(Exception e) {
        LOG.errorf(e, "Unexpected error: %s", e.getMessage());
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    public record ErrorResponse(String code, String message) {}
}