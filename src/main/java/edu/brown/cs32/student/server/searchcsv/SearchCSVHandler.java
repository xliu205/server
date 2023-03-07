package edu.brown.cs32.student.server.searchcsv;

import edu.brown.cs32.student.csv.exception.NoHeaderException;
import edu.brown.cs32.student.csv.search.Search;
import edu.brown.cs32.student.server.GeneralResponse;
import edu.brown.cs32.student.server.MissingArgException;
import java.util.HashMap;
import java.util.List;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handler class for the soup ordering API endpoint.
 *
 * <p>This endpoint is similar to the endpoint(s) you'll need to create for Sprint 2. It takes a
 * basic GET request with no Json body, and returns a Json object in reply. The responses are more
 * complex, but this should serve as a reference.
 */
public class SearchCSVHandler implements Route {
  private final List<List<String>> csvData;
  private final List<String> csvHeader;
  private final String PREFIX = "data/";

  /**
   * Constructor accepts some shared state
   *
   * @param csvData the shared csvData (exclude Header if csv has head)
   * @param csvHeader the shared csvHeader
   */
  public SearchCSVHandler(List<List<String>> csvData, List<String> csvHeader) {
    this.csvData = csvData;
    this.csvHeader = csvHeader;
  }
  /**
   * Search in a loaded csv file
   *
   * @param request the request to handle
   * @param response use to modify properties of the response
   * @return response content
   * @throws Exception This is part of the interface; we don't have to throw anything.
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    String query = request.queryParams("query");
    HashMap<String, Object> result = new HashMap<>();
    if (csvData.isEmpty()) {
      result.put("result", "error_bad_request");
      result.put("detail", "No CSV data loaded");
      return new GeneralResponse(result).serialize();
    }

    try {
      if (query == null) {
        result.put("result", "error_bad_request");
        result.put("detail", "Need query field to search.");
        return new GeneralResponse(result).serialize();
      }
      Search srh = new Search(!csvHeader.isEmpty(), csvHeader, csvData);
      result.put("request", new SearchCSVRequest(query));
      List<List<String>> res = srh.search(query);
      result.put("search result", res);
      return new GeneralResponse(result).serialize();

    } catch (MissingArgException | IllegalArgumentException e) {
      result.put("result", "error_bad_request");
      result.put("detail", e.getMessage());

    } catch (NoHeaderException e) {
      result.put("result", "error_datasource");
      result.put("detail", e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new GeneralResponse(result).serialize();
  }

  public record SearchCSVRequest(String query) {}
  ;
}
