package edu.brown.cs32.student.server.viewcsv;

import edu.brown.cs32.student.server.GeneralResponse;
import edu.brown.cs32.student.server.MissingArgException;
import edu.brown.cs32.student.server.loadcsv.InValidHeaderArgException;
import java.io.IOException;
import java.util.ArrayList;
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
public class ViewCSVHandler implements Route {
  private final List<List<String>> csvData;
  private final List<String> csvHeader;

  private final String PREFIX = "data/";
  HashMap<String, Object> result = new HashMap<>();

  /**
   * Constructor accepts some shared state
   *
   * @param csvData the shared csvData (exclude Header if csv has head)
   * @param csvHeader the shared csvHeader
   */
  public ViewCSVHandler(List<List<String>> csvData, List<String> csvHeader) {
    this.csvData = csvData;
    this.csvHeader = csvHeader;
  }

  /**
   * View a loaded csv file
   *
   * @param request the request to handle
   * @param response use to modify properties of the response
   * @return response content
   * @throws Exception This is part of the interface; we don't have to throw anything.
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {

    if (csvData.isEmpty()) {
      result.put("result", "error_bad_request");
      result.put("detail", "No CSV data loaded");
      return new GeneralResponse(result).serialize();
    }
    try {
      result.put("result", "success");
      if (csvHeader.isEmpty()) {
        result.put("detail", csvData);
      } else {
        List<List<String>> csvwithheader = new ArrayList<>();
        csvwithheader.add(csvHeader); // Add the header as the first element
        csvwithheader.addAll(csvData);
        result.put("detail", csvwithheader);
      }
      return new GeneralResponse(result).serialize();
    } catch (MissingArgException e) {
      result.put("result", "error_bad_request");
      result.put("detail", e.getMessage());
    } catch (IOException e) {
      result.put("result", "error_datasource");
      result.put("detail", "File access failed.");
    } catch (InValidHeaderArgException e) {
      result.put("result", "error_bad_request");
      result.put("detail", e.getMessage());
    } catch (Exception e) {
      // open file failed, error
      e.printStackTrace();
      result.put("error_datasource", "File access failed.");
    }
    return new GeneralResponse(result).serialize();
  }
}
