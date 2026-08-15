package com.dentalclinic.app.service;

public class SchedulingConflictException extends RuntimeException {
    public SchedulingConflictException(String message) { super(message); }
}
