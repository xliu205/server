package edu.brown.cs32.student.server.loadcsv;

public class InValidHeaderArgException extends Exception {
  public InValidHeaderArgException(String err) {
    super(err);
  }
}
