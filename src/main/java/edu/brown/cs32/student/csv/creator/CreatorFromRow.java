package edu.brown.cs32.student.csv.creator;

import edu.brown.cs32.student.csv.exception.FactoryFailureException;
import java.util.List;

/** An inferface for user-developed creator to create data structure from a row */
public interface CreatorFromRow<T> {
  /**
   * Create a T object given the row data
   *
   * @param row the parsed row CSV data
   * @return a T object
   * @throws FactoryFailureException for errors when creator handles the row data
   */
  T create(List<String> row) throws FactoryFailureException;
}
