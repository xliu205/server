package edu.brown.cs32.student.csv.creator;

import edu.brown.cs32.student.csv.exception.FactoryFailureException;
import java.util.List;

/** A creator for test purpose that creates star */
public class StarCreator implements CreatorFromRow<Star> {
  Star rowData;
  /**
   * Create a Star object given the row data
   *
   * @param row the parsed row CSV data
   * @return a Star object
   * @throws FactoryFailureException for errors when creator handles the row data
   */
  @Override
  public Star create(List<String> row) throws FactoryFailureException {
    if (row.size() != 5) throw new FactoryFailureException("Cannot construct Star object", row);
    Star s =
        new Star(
            Integer.parseInt(row.get(0)),
            row.get(1),
            Double.parseDouble(row.get(2)),
            Double.parseDouble(row.get(3)),
            Double.parseDouble(row.get(4)));
    return s;
  }
}
