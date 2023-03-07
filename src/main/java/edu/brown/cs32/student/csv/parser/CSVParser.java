package edu.brown.cs32.student.csv.parser;

import edu.brown.cs32.student.csv.creator.CreatorFromRow;
import edu.brown.cs32.student.csv.exception.FactoryFailureException;
import edu.brown.cs32.student.csv.exception.WrongFormatCSVException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** Parse the CSV data into user-specified `List T` data from a given `Reader` */
public class CSVParser<T> {
  private boolean hasHead;
  private List<String> header;
  private BufferedReader br;
  private CreatorFromRow<T> creator;
  /** Constructor */
  public CSVParser(boolean hasHead, Reader r, CreatorFromRow<T> c) {
    this.hasHead = hasHead;
    if (hasHead) header = new ArrayList<>();
    else header = null;
    br = new BufferedReader(r);
    creator = c;
  }

  /**
   * Header Getter
   *
   * @return the CSV header
   */
  public List<String> getHeader() {
    return new ArrayList<>(header);
  }

  public Boolean getHasHeader() {
    return hasHead;
  }
  /**
   * Parse the CSV data
   *
   * @return a List of each parsed row
   */
  public List<T> parse(List<T> ret, List<String> header) throws Exception {
    try {
      ret.clear();
      header.clear();
      String line;
      int count = 0;
      int rowItemNum = -1;
      while ((line = br.readLine()) != null) {
        count++;
        List<String> tmp = List.of(line.split(","));
        if (count == 1) {
          if (hasHead) {
            header.addAll(tmp);
          }
          rowItemNum = tmp.size();
          continue;
        }
        if (tmp.size() != rowItemNum) {
          throw new WrongFormatCSVException(
              "Wrong CSV data format! Line "
                  + count
                  + " has "
                  + tmp.size()
                  + " columns, but should be "
                  + rowItemNum);
        }
        ret.add(creator.create(tmp));
      }
    } catch (IOException e) {
      throw e;
    } catch (FactoryFailureException e) {
      System.err.println(e.getMessage());
      System.err.println(e.getRow().toString());
      throw e;
    }
    return ret;
  }
}
