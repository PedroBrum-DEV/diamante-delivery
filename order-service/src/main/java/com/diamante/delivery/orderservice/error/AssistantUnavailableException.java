package com.diamante.delivery.orderservice.error;

public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(Throwable cause) {
        super("Assistant is unavailable right now", cause);
    }
}
