package edu.brown.cs32.student.csv.exception;

import java.util.ArrayList;
import java.util.List;

/** Exception for errors when creator handles the row data */
public class FactoryFailureException extends Exception {
  /** Row data */
  final List<String> row;
  /** Constructor */
  public FactoryFailureException(String message, List<String> row) {
    super(message);
    this.row = new ArrayList<>(row);
  }
  /**
   * Row getter
   *
   * @return the row
   */
  public List<String> getRow() {
    return row;
  }
}
