package com.vitaliy.medcard.exception;

public class ShareLinkExpiredException extends RuntimeException {

    public ShareLinkExpiredException(String message) {
        super(message);
    }
}
