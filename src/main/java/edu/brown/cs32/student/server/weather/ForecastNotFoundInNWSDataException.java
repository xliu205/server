package edu.brown.cs32.student.server.weather;

/** Forecast Not Found In NWS Data Exception */
public class ForecastNotFoundInNWSDataException extends Exception {
  public ForecastNotFoundInNWSDataException(String err) {
    super(err);
  }
}
