package edu.brown.cs32.student.csv.exception;
/**
 * Exception for errors when try to use name as column identifier when searching but there is no
 * head
 */
public class NoHeaderException extends Exception {
  /** Constructor */
  public NoHeaderException(String errorMessage) {
    super(errorMessage);
  }
}
