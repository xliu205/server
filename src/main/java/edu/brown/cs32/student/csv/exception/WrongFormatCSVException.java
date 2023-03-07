package edu.brown.cs32.student.csv.exception;

public class WrongFormatCSVException extends Exception {
  public WrongFormatCSVException(String errorMessage) {
    super(errorMessage);
  }
}
