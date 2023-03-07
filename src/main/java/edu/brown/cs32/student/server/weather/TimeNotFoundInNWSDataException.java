package edu.brown.cs32.student.server.weather;

/** Time Not Found In NWS Data Exception */
public class TimeNotFoundInNWSDataException extends Exception {
  public TimeNotFoundInNWSDataException(String err) {
    super(err);
  }
}
