package edu.brown.cs32.student.csv.search;

import edu.brown.cs32.student.csv.exception.NoHeaderException;
import java.util.*;

/** Search the result in the CSV data given a query */
public class Search {
  private boolean hasHead;
  private List<String> header;
  private List<List<String>> data;
  /** Constructor */
  public Search(boolean hasHead, List<String> header, List<List<String>> data) {
    this.hasHead = hasHead;
    this.header = header;
    this.data = data;
  }
  /**
   * Search the result in the CSV data given a query
   *
   * @param query the input query string
   * @return a list of searched row data
   */
  public List<List<String>> search(String query)
      throws NoHeaderException, IllegalArgumentException {
    List<List<String>> ret = new ArrayList<>();
    if (data.isEmpty()) return ret;
    List<String> tmp = List.of(query.split(",|\\(|\\)"));
    QueryTree qt = new QueryTree(tmp);
    Set<Integer> resSet = null;
    try {
      resSet = searchHelper(qt.root);
    } catch (NoHeaderException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw e;
    }
    List<Integer> resArray = new ArrayList<>();
    for (int n : resSet) resArray.add(n);
    Collections.sort(resArray);
    for (int n : resArray) ret.add(data.get(n));
    return ret;
  }
  /**
   * Search recursively in query tree
   *
   * @param root the query tree node
   * @return a integer set indicating the searched result
   * @throws NoHeaderException for errors when creator handles the row data
   * @throws IllegalArgumentException for invalid query format
   */
  private Set<Integer> searchHelper(Node root) throws NoHeaderException, IllegalArgumentException {
    if (!root.isOp) return searchOnce(root.queryName);
    else {
      Set<Integer> ret = new HashSet<>();
      if (root.Op.equals("not")) {
        Set<Integer> tmp = searchHelper(root.children.get(0));
        for (int i = 0; i < data.size(); i++) {
          if (!tmp.contains(i)) ret.add(i);
        }
      } else if (root.Op.equals("and")) {
        Set<Integer> tmp1 = searchHelper(root.children.get(0));
        Set<Integer> tmp2 = searchHelper(root.children.get(1));
        for (int n : tmp1) {
          if (tmp2.contains(n)) ret.add(n);
        }
      } else if (root.Op.equals("or")) {
        Set<Integer> tmp1 = searchHelper(root.children.get(0));
        Set<Integer> tmp2 = searchHelper(root.children.get(1));
        ret = tmp1;
        for (int n : tmp2) {
          ret.add(n);
        }
      }
      return ret;
    }
  }

  /**
   * Search a basic query in the data
   *
   * @param query the basic query
   * @return a integer set indicating the searched result
   * @throws NoHeaderException for errors when creator handles the row data
   * @throws IllegalArgumentException for invalid query format
   */
  private Set<Integer> searchOnce(String query) throws NoHeaderException, IllegalArgumentException {
    Set<Integer> ret = new HashSet<>();
    List<String> tmp = List.of(query.split(";"));
    if (tmp.size() != 1 && tmp.size() != 3)
      throw new IllegalArgumentException(
          "Wrong query format! Received " + tmp.size() + " args, but should be 1 or 3");
    String target = tmp.get(0);
    boolean hasConstraint = (tmp.size() == 3);
    if (hasConstraint) {
      boolean byIdx = tmp.get(2).equals("idx");
      if (!byIdx && !hasHead)
        throw new NoHeaderException(
            "Cannot use column name as identifier when the CSV has no header");
      int colIdx = -1;
      if (byIdx) colIdx = Integer.parseInt(tmp.get(1));
      else {
        String colName = tmp.get(1);
        for (int i = 0; i < header.size(); i++) {
          if (colName.equals(header.get(i))) {
            colIdx = i;
            break;
          }
        }
      }
      if (colIdx < 0 || colIdx >= data.get(0).size()) return ret;
      for (int i = 0; i < data.size(); i++) {
        if (data.get(i).get(colIdx).equals(target)) ret.add(i);
      }
    } else {
      for (int i = 0; i < data.size(); i++) {
        for (String s : data.get(i)) {
          if (s.equals(target)) {
            ret.add(i);
            break;
          }
        }
      }
    }
    return ret;
  }
}
