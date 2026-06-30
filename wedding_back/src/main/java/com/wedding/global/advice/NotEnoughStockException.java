package com.wedding.global.advice;

public class NotEnoughStockException extends RuntimeException{
    public NotEnoughStockException(String msg) {
        super(msg);
    }
}
