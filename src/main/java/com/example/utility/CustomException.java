package com.example.utility;

import lombok.Data;

@Data
public class CustomException extends Exception {
    private String message;

    public CustomException(String message) {
        super(message);
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomException(Throwable cause) {
        super(cause);
    }
}
