package com.whut.training.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionUsesMatchingHttpStatus() {
        var response = handler.handleBusiness(new BusinessException(403, "forbidden"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().code());
        assertEquals("forbidden", response.getBody().message());
    }

    @Test
    void invalidBusinessStatusFallsBackToInternalServerError() {
        var response = handler.handleBusiness(new BusinessException(200, "invalid status"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().code());
    }

    @Test
    void missingResourceUsesNotFoundStatus() {
        var response = handler.handleNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/missing")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().code());
    }
}
