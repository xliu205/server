package edu.brown.cs32.student.csv.search;

import java.util.List;

/** `QueryTree` Node */
class Node {
  boolean isOp;
  String Op;
  String queryName;
  List<Node> children;
}
