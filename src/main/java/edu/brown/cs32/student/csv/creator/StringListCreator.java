package edu.brown.cs32.student.csv.creator;

import java.util.ArrayList;
import java.util.List;

/** The default creator */
public class StringListCreator implements CreatorFromRow<List<String>> {
  /**
   * Create a List String given the row data
   *
   * @param row the parsed row CSV data
   * @return a List String
   */
  @Override
  public List<String> create(List<String> row) {
    return new ArrayList<>(row);
  }
}
