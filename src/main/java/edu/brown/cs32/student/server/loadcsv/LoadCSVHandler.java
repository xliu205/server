package edu.brown.cs32.student.server.loadcsv;

import edu.brown.cs32.student.csv.creator.StringListCreator;
import edu.brown.cs32.student.csv.parser.CSVParser;
import edu.brown.cs32.student.server.GeneralResponse;
import edu.brown.cs32.student.server.MissingArgException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.utils.StringUtils;

/**
 * Handler class for the soup ordering API endpoint.
 *
 * <p>This endpoint is similar to the endpoint(s) you'll need to create for Sprint 2. It takes a
 * basic GET request with no Json body, and returns a Json object in reply. The responses are more
 * complex, but this should serve as a reference.
 */
public class LoadCSVHandler implements Route {
  private final List<List<String>> csvData;
  private final List<String> csvHeader;
  private final String PREFIX = "data/";
  //
  //    /**
  //     * Constructor accepts some shared state
  //     * @param csvData the shared csvData (exclude Header if csv has head)
  //     * @param csvHeader the shared csvHeader
  //     *
  //     */

  public LoadCSVHandler(List<List<String>> csvData, List<String> csvHeader) {
    this.csvData = csvData;
    this.csvHeader = csvHeader;
  }

  /**
   * Load a csv file
   *
   * @param request the request to handle
   * @param response use to modify properties of the response
   * @return response content
   * @throws Exception This is part of the interface; we don't have to throw anything.
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    String fileName = request.queryParams("filepath");
    String header = request.queryParams("header");
    HashMap<String, Object> result = new HashMap<>();

    try {
      // check if filepath and header param given
      List<String> missingArgs = new ArrayList<>();
      if (StringUtils.isBlank(fileName)) missingArgs.add("filepath");
      if (StringUtils.isBlank(header)) missingArgs.add("header");
      if (!missingArgs.isEmpty()) throw new MissingArgException(missingArgs);
      if (!header.equals("false") && !header.equals("true"))
        throw new InValidHeaderArgException(
            "header should be either true or false, but get " + header);

      // limit the filepath only this folder.
      String file = PREFIX + fileName;

      FileReader reader = new FileReader(file);
      if (header.equals("true")) {
        CSVParser<List<String>> parser =
            new CSVParser(header.equals("true"), reader, new StringListCreator());
        parser.parse(csvData, csvHeader);
      } else if (header.equals("false")) {
        CSVParser<List<String>> parser =
            new CSVParser(header.equals("false"), reader, new StringListCreator());
        parser.parse(csvData, csvHeader);
      }
      result.put("result", "success");
      result.put("request", new LoadCSVRequest(fileName, header));
      result.put("detail", "Successfully loaded file: " + fileName);
      return new GeneralResponse(result).serialize();

    } catch (MissingArgException e) {
      result.put("result", "error_bad_request");
      result.put("detail", e.getMessage());
    } catch (IOException e) {
      result.put("result", "error_datasource");
      result.put("detail", "Fail to load file: " + fileName);
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

  public record LoadCSVRequest(String filepath, String header) {}
  ;
}
