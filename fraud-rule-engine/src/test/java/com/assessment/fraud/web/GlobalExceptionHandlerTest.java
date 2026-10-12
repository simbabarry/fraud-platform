package com.assessment.fraud.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.assessment.fraud.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/fraud-flags");
    }

    @Test
    void handleValidationReturnsBadRequestWithFieldDetails() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "must be positive"));
        MethodParameter parameter = new MethodParameter(Dummy.class.getDeclaredMethod("sample", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().details()).containsExactly("amount: must be positive");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/fraud-flags");
    }

    @Test
    void handleInvalidQueryReturnsBadRequestForPropertyReference() {
        PropertyReferenceException ex = new PropertyReferenceException(
                "string", TypeInformation.of(Object.class), List.of());

        ResponseEntity<ApiError> response = handler.handleInvalidQuery(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("Invalid sort property");
        assertThat(response.getBody().details()).isNotEmpty();
    }

    @Test
    void handleInvalidQueryReturnsBadRequestWhenPropertyReferenceIsCause() {
        PropertyReferenceException cause = new PropertyReferenceException(
                "string", TypeInformation.of(Object.class), List.of());
        InvalidDataAccessApiUsageException ex = new InvalidDataAccessApiUsageException("bad sort", cause);

        ResponseEntity<ApiError> response = handler.handleInvalidQuery(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("Invalid sort property");
    }

    @Test
    void handleInvalidQueryFallsBackToGenericWhenNotSortRelated() {
        InvalidDataAccessApiUsageException ex = new InvalidDataAccessApiUsageException("other api misuse");

        ResponseEntity<ApiError> response = handler.handleInvalidQuery(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Unexpected error");
        assertThat(response.getBody().details()).containsExactly("InvalidDataAccessApiUsageException");
    }

    @Test
    void handleStatusUsesProvidedReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found");

        ResponseEntity<ApiError> response = handler.handleStatus(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Assessment not found");
        assertThat(response.getBody().error()).isEqualTo("Not Found");
    }

    @Test
    void handleStatusFallsBackToReasonPhraseWhenReasonMissing() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN);

        ResponseEntity<ApiError> response = handler.handleStatus(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo("Forbidden");
    }

    @Test
    void handleGenericReturnsInternalServerError() {
        ResponseEntity<ApiError> response = handler.handleGeneric(new IllegalStateException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().details()).containsExactly("IllegalStateException");
    }

    @Test
    void invalidSortUsesExceptionClassNameWhenRootMessageMissing() {
        PropertyReferenceException cause = new PropertyReferenceException(
                "string", TypeInformation.of(Object.class), List.of()) {
            @Override
            public String getMessage() {
                return null;
            }
        };
        InvalidDataAccessApiUsageException ex = new InvalidDataAccessApiUsageException(null, cause);

        ResponseEntity<ApiError> response = handler.handleInvalidQuery(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().details()).containsExactly("InvalidDataAccessApiUsageException");
    }

    private static class Dummy {
        @SuppressWarnings("unused")
        void sample(String value) {}
    }
}
