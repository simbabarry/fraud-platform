package com.assessment.fraud.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp, int status, String error, String message, String path, List<String> details) {}
