package edu.brown.cs32.student.server;

import java.util.List;

/** Exception for errors when required args are missing */
public class MissingArgException extends Exception {
  /** Constructor */
  public MissingArgException(List<String> Args) {
    super("Missing Args: " + Args.toString());
  }
}
