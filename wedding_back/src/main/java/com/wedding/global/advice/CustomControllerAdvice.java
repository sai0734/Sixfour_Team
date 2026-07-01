package com.wedding.global.advice;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.wedding.global.util.CustomJWTException;

/**
 * CustomControllerAdvice
 */
@RestControllerAdvice
public class CustomControllerAdvice {


  @ExceptionHandler(NoSuchElementException.class)
  protected ResponseEntity<?> notExist(NoSuchElementException e) {

      String msg = e.getMessage();

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("msg", msg));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<?> handleIllegalArgumentException(MethodArgumentNotValidException e) {

      String msg = e.getMessage();

      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("msg", msg));
  }

  @ExceptionHandler(CustomJWTException.class)
  protected ResponseEntity<?> handleJWTException(CustomJWTException e) {

      String msg = e.getMessage();

      return ResponseEntity.ok().body(Map.of("error", msg));
  }

  @ExceptionHandler(NotEnoughStockException.class)
    protected  ResponseEntity<?> handleNotEnoughStock(NotEnoughStockException e) {

      String msg = e.getMessage();

      return ResponseEntity.ok().body(Map.of("msg", msg));
  }

  @ExceptionHandler(DuplicateEmailException.class)
    protected  ResponseEntity<?> handleDuplicateEmail(DuplicateEmailException e) {

      String msg = e.getMessage();

      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("msg", msg));
  }
}