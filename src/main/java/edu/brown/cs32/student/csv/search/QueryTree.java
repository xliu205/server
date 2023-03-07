package edu.brown.cs32.student.csv.search;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/** Parse the query string into a Tree to handle comprehensive and, or, not and basic query */
public class QueryTree {
  Node root;
  Deque<String> inDeque;

  public QueryTree(List<String> input) {
    inDeque = new LinkedList<>();
    for (String s : input) {
      inDeque.addLast(s);
    }
    root = Build();
  }
  /**
   * Build the query tree using pre-order traversal
   *
   * @return the root node
   */
  private Node Build() {
    String s = inDeque.removeFirst();
    Node curr = new Node();
    curr.children = new ArrayList<>();
    if (s.equals("and") || s.equals("or") || s.equals("not")) {
      curr.isOp = true;
      curr.Op = s;
      if (s.equals("and") || s.equals("or")) {
        curr.children.add(Build());
        curr.children.add(Build());
      } else {
        curr.children.add(Build());
      }
    } else {
      curr.isOp = false;
      curr.queryName = s;
    }
    return curr;
  }
}
