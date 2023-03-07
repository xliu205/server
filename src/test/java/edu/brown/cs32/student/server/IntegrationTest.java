package edu.brown.cs32.student.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.brown.cs32.student.server.loadcsv.LoadCSVHandler;
import edu.brown.cs32.student.server.searchcsv.SearchCSVHandler;
import edu.brown.cs32.student.server.viewcsv.ViewCSVHandler;
import edu.brown.cs32.student.server.weather.WeatherHandler;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testng.annotations.BeforeClass;
import spark.Spark;

public class IntegrationTest {
  List<List<String>> csvData;
  List<String> csvHeader;

  @BeforeClass
  public static void setup_before_everything() {
    Spark.port(0);
    Logger.getLogger("").setLevel(Level.WARNING); // empty name = root logger
  }

  @BeforeEach
  void setUp() {
    csvData = new ArrayList<>();
    csvHeader = new ArrayList<>();
    Spark.get("/searchcsv", new SearchCSVHandler(csvData, csvHeader));
    Spark.get("/loadcsv", new LoadCSVHandler(csvData, csvHeader));
    Spark.get("/viewcsv", new ViewCSVHandler(csvData, csvHeader));
    Spark.get("/weather", new WeatherHandler());
    Spark.init();
    Spark.awaitInitialization(); // don't continue until the server is listening
  }

  @AfterEach
  void tearDown() {
    csvData = new ArrayList<>();
    csvHeader = new ArrayList<>();
    Spark.unmap("/searchcsv");
    Spark.unmap("/loadcsv");
    Spark.unmap("/viewcsv");
    Spark.unmap("/weather");
    Spark.awaitStop(); // don't proceed until the server is stopped
  }

  private static HttpURLConnection tryRequest(String apiCall) throws IOException {
    // Configure the connection (but don't actually send the request yet)
    URL requestURL = new URL("http://localhost:" + Spark.port() + "/" + apiCall);
    HttpURLConnection clientConnection = (HttpURLConnection) requestURL.openConnection();

    // The default method is "GET", which is what we're using here.
    // If we were using "POST", we'd need to say so.
    // clientConnection.setRequestMethod("GET");

    clientConnection.connect();
    return clientConnection;
  }

  /**
   * test get Weather Successfully with CurrentTime
   *
   * @throws Exception
   */
  @Test
  void testWeatherSuccessCurrentTime() throws Exception {
    HttpURLConnection clientConnection = tryRequest("weather?lat=41.826820&lon=-71.402931");
    // Get an OK response (the *connection* worked, the *API* provides an error response)
    assertEquals(200, clientConnection.getResponseCode());

    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));

    LocalDateTime responseTime =
        OffsetDateTime.parse((String) response.responseMap().get("time")).toLocalDateTime();
    LocalDateTime currTime = OffsetDateTime.now().toLocalDateTime();
    Duration duration = Duration.between(responseTime, currTime);
    // difference of (currTime - responseTime) i in the range of [0-1]h
    assertTrue(
        duration.compareTo(Duration.ofHours(0)) >= 0
            && duration.compareTo(Duration.ofHours(1)) <= 0);
    clientConnection.disconnect();
  }

  /**
   * test get Weather Successfully with RequestTime
   *
   * @throws Exception
   */
  @Test
  void testWeatherSuccessRequestTime() throws Exception {
    OffsetDateTime reqTime = OffsetDateTime.now();
    HttpURLConnection clientConnection =
        tryRequest("weather?lat=41.826820&lon=-71.402931&datetime=" + reqTime);
    // Get an OK response (the *connection* worked, the *API* provides an error response)
    assertEquals(200, clientConnection.getResponseCode());

    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));

    LocalDateTime responseTime =
        OffsetDateTime.parse((String) response.responseMap().get("time")).toLocalDateTime();
    Duration duration = Duration.between(responseTime, reqTime.toLocalDateTime());
    // difference of (requestTime - responseTime) i in the range of [0-1]h
    assertTrue(
        duration.compareTo(Duration.ofHours(0)) >= 0
            && duration.compareTo(Duration.ofHours(1)) <= 0);
  }

  /**
   * test get Weather with missing field
   *
   * @throws Exception
   */
  @Test
  void testWeatherMissingField() throws Exception {
    HttpURLConnection clientConnection = tryRequest("weather");
    // Get an OK response (the *connection* worked, the *API* provides an error response)
    assertEquals(200, clientConnection.getResponseCode());

    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));

    Map<String, Object> ExpectedResponseMap = new HashMap<>();
    ExpectedResponseMap.put("result", "error_bad_request");
    String detail =
        new MissingArgException(new ArrayList<String>(Arrays.asList("lat", "lon"))).getMessage();
    ExpectedResponseMap.put("detail", detail);
    assertEquals(response.responseMap(), ExpectedResponseMap);
    clientConnection.disconnect();
  }

  /**
   * test get Weather with Invalid Point
   *
   * @throws Exception
   */
  @Test
  void testWeatherInvalidPoint() throws Exception {
    HttpURLConnection clientConnection = tryRequest("weather?lat=4&lon=-7");
    // Get an OK response (the *connection* worked, the *API* provides an error response)
    assertEquals(200, clientConnection.getResponseCode());

    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));

    Map<String, Object> ExpectedResponseMap = new HashMap<>();
    ExpectedResponseMap.put("result", "error_datasource");
    ExpectedResponseMap.put(
        "detail",
        "Unable to provide data for requested URL: https://api.weather.gov/points/4.0000,-7.0000");
    assertEquals(response.responseMap(), ExpectedResponseMap);
    clientConnection.disconnect();
  }

  /**
   * test get Weather with Invalid Datetime
   *
   * @throws Exception
   */
  @Test
  void testWeatherInvalidDatetime() throws Exception {
    HttpURLConnection clientConnection =
        tryRequest("weather?lat=41.826820&lon=-71.402931&datetime=abc");
    // Get an OK response (the *connection* worked, the *API* provides an error response)
    assertEquals(200, clientConnection.getResponseCode());

    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));

    Map<String, Object> ExpectedResponseMap = new HashMap<>();
    ExpectedResponseMap.put("result", "error_bad_request");
    ExpectedResponseMap.put("detail", "Invalid datetime format: abc");
    assertEquals(response.responseMap(), ExpectedResponseMap);
    clientConnection.disconnect();
  }

  /**
   * test get Weather with Datetime Not Found from NWS
   *
   * @throws Exception
   */
  @Test
  void testWeatherDatetimeNotFound() throws Exception {
    HttpURLConnection clientConnection =
        tryRequest("weather?lat=41.826820&lon=-71.402931&datetime=2020-02-28T22:00:01-05:00");
    // Get an OK response (the *connection* worked, the *API* provides an error response)
    assertEquals(200, clientConnection.getResponseCode());

    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));

    Map<String, Object> ExpectedResponseMap = new HashMap<>();
    ExpectedResponseMap.put("result", "error_datasource");
    ExpectedResponseMap.put(
        "detail", "Unable to provide data for requested time: 2020-02-28T22:00:01-05:00");
    assertEquals(response.responseMap(), ExpectedResponseMap);
    clientConnection.disconnect();
  }
  /*
   * load csv successful
   * */
  @Test
  void testCanLoadCSV() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    assertEquals("success", response.responseMap().get("result"));
    assertEquals("Successfully loaded file: ten-star.csv", response.responseMap().get("detail"));
    clientConnection.disconnect();
  }

  /*
   * load csv unsuccessful
   * */
  @Test
  void testCanNotLoadCSV() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.cs&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    assertEquals("error_datasource", response.responseMap().get("result"));
    assertEquals("Fail to load file: ten-star.cs", response.responseMap().get("detail"));
    clientConnection.disconnect();
  }

  /*
   * load csv without specifying header
   * */
  @Test
  void testLoadCSVMissingArgument() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    assertEquals("error_bad_request", response.responseMap().get("result"));
    assertEquals("Missing Args: [header]", response.responseMap().get("detail"));
    clientConnection.disconnect();
  }

  /*
   * load csv without wrong header argument
   * */
  @Test
  void testLoadCSVWrongHeader() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=1");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    assertEquals("error_bad_request", response.responseMap().get("result"));
    assertEquals(
        "header should be either true or false, but get 1", response.responseMap().get("detail"));
    clientConnection.disconnect();
  }
  /*
   * load two csv file and view
   * */
  @Test
  void testLoadAndViewTwoCSV() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("viewcsv");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("loadcsv?filepath=test.csv&header=false");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("viewcsv");
    assertEquals(200, clientConnection.getResponseCode());
    List<List<String>> expected = new ArrayList<>();
    expected.add(List.of(new String[] {"John Doe", "20", "Male", "Computer Science", "3.5"}));
    expected.add(List.of(new String[] {"Jane Smith", "", "Female", "Business", "3.2"}));
    expected.add(
        List.of(new String[] {"David Lee", "21", "Male", "Mechanical Engineering", "3.9"}));
    expected.add(List.of(new String[] {"Amy Chen", "18", "Female", "Biology", "3.7"}));
    expected.add(List.of(new String[] {"Michael Johnson", "22", "Male", "History", "3.1"}));
    expected.add(List.of(new String[] {"Emily Brown", "20", "Female", "Psychology", "3.8"}));
    expected.add(List.of(new String[] {"Grace Lee", "21", "Female", "Chemistry", "3.6"}));
    expected.add(List.of(new String[] {"Daniel Park", "18", "Male", "English", "3.3"}));
    expected.add(List.of(new String[] {"Sophia Lee", "20", "Female", "Mathematics", "3.8"}));

    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    ExpecteResponseMap.put("result", "success");
    ExpecteResponseMap.put("detail", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*
   * successfully viewed csv with header
   * */
  @Test
  void testViewCSVwithHeader() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("viewcsv");
    assertEquals(200, clientConnection.getResponseCode());

    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    List<List<String>> expected = new ArrayList<>();
    expected.add(List.of(new String[] {"StarID", "ProperName", "X", "Y", "Z"}));
    expected.add(List.of(new String[] {"0", "Sol", "0", "0", "0"}));
    expected.add(List.of(new String[] {"1", "", "282.43485", "0.00449", "5.36884"}));
    expected.add(List.of(new String[] {"2", "", "43.04329", "0.00285", "-15.24144"}));
    expected.add(List.of(new String[] {"3", "", "277.11358", "0.02422", "223.27753"}));
    expected.add(List.of(new String[] {"3759", "96 G. Psc", "7.26388", "1.55643", "0.68697"}));
    expected.add(
        List.of(new String[] {"70667", "Proxima Centauri", "-0.47175", "-0.36132", "-1.15037"}));
    expected.add(
        List.of(new String[] {"71454", "Rigel Kentaurus B", "-0.50359", "-0.42128", "-1.1767"}));
    expected.add(
        List.of(new String[] {"71457", "Rigel Kentaurus A", "-0.50362", "-0.42139", "-1.17665"}));
    expected.add(
        List.of(new String[] {"87666", "Barnard's Star", "-0.01729", "-1.81533", "0.14824"}));
    expected.add(List.of(new String[] {"118721", "", "-2.28262", "0.64697", "0.29354"}));

    ExpecteResponseMap.put("result", "success");
    ExpecteResponseMap.put("detail", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*
   * successfully viewed csv without header
   * */
  @Test
  void testViewNoHeaderCSV() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=test.csv&header=false");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("viewcsv");
    assertEquals(200, clientConnection.getResponseCode());

    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    List<List<String>> expected = new ArrayList<>();
    expected.add(List.of(new String[] {"John Doe", "20", "Male", "Computer Science", "3.5"}));
    expected.add(List.of(new String[] {"Jane Smith", "", "Female", "Business", "3.2"}));
    expected.add(
        List.of(new String[] {"David Lee", "21", "Male", "Mechanical Engineering", "3.9"}));
    expected.add(List.of(new String[] {"Amy Chen", "18", "Female", "Biology", "3.7"}));
    expected.add(List.of(new String[] {"Michael Johnson", "22", "Male", "History", "3.1"}));
    expected.add(List.of(new String[] {"Emily Brown", "20", "Female", "Psychology", "3.8"}));
    expected.add(List.of(new String[] {"Grace Lee", "21", "Female", "Chemistry", "3.6"}));
    expected.add(List.of(new String[] {"Daniel Park", "18", "Male", "English", "3.3"}));
    expected.add(List.of(new String[] {"Sophia Lee", "20", "Female", "Mathematics", "3.8"}));
    ExpecteResponseMap.put("result", "success");
    ExpecteResponseMap.put("detail", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*
   * view csv unsuccessful
   * */
  @Test
  void testCanNotViewCSV() throws Exception {
    HttpURLConnection clientConnection = tryRequest("viewcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    assertEquals("error_bad_request", response.responseMap().get("result"));
    assertEquals("No CSV data loaded", response.responseMap().get("detail"));
    clientConnection.disconnect();
  }

  /*
   * no csv data loaded before search
   * */
  @Test
  void testCSVNotFound() throws Exception {
    HttpURLConnection clientConnection =
        tryRequest("searchcsv?query=or(Barnard%27s%20Star,and(0;3;idx,not(5.36884;Z;name)))");
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));

    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    ExpecteResponseMap.put("result", "error_bad_request");
    ExpecteResponseMap.put("detail", "No CSV data loaded");
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*
   * no query entered when search
   * */
  @Test
  void testNoQueryFound() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?bdnc");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));

    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    ExpecteResponseMap.put("result", "error_bad_request");
    ExpecteResponseMap.put("detail", "Need query field to search.");
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*
   * search CSV data with header using basic query without identifier successfully, display result
   * */
  @Test
  void testSearchBasicQuery() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=-0.01729");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "-0.01729");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    expected.add(
        List.of(new String[] {"87666", "Barnard's Star", "-0.01729", "-1.81533", "0.14824"}));
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*
   * search CSV data without header using basic query without identifier successfully, display result
   * */
  @Test
  void testSearchBasicQueryNoHeader() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=test.csv&header=false");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=3.8");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "3.8");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    expected.add(List.of(new String[] {"Emily Brown", "20", "Female", "Psychology", "3.8"}));
    expected.add(List.of(new String[] {"Sophia Lee", "20", "Female", "Mathematics", "3.8"}));
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*
   * Search CSV data with header using basic query with name identifier, and the result is
   * not empty
   * */
  @Test
  void testSearchWithHeaderIdentifier() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=-0.01729;X;name");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "-0.01729;X;name");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    expected.add(
        List.of(new String[] {"87666", "Barnard's Star", "-0.01729", "-1.81533", "0.14824"}));
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*
   * Search CSV data with header using basic query with index identifier, and the result is
   * empty
   */
  @Test
  void testSearchWithHeaderIdentifierNoResult() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=-0.01729;Y;name");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "-0.01729;Y;name");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*
   * Search CSV data without header using basic query with index identifier, and the result
   * is not empty
   */
  @Test
  void testSearchWOHeaderIdentifierIDX() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=test.csv&header=false");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=3.8;4;idx");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "3.8;4;idx");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    expected.add(List.of(new String[] {"Emily Brown", "20", "Female", "Psychology", "3.8"}));
    expected.add(List.of(new String[] {"Sophia Lee", "20", "Female", "Mathematics", "3.8"}));
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /* Search CSV data with header using basic query with index identifier, and the result is
  empty
  */
  @Test
  void testSearchWithHeaderIdentifierIDXNoResult() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=-0.01729;3;idx");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "-0.01729;3;idx");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /* Search CSV data without header using basic query with index identifier, and the result is
    empty
  */
  @Test
  void testSearchWithNOHeaderIdentifierIDXNoResult() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=test.csv&header=false");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=3.8;1;idx");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "3.8;1;idx");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /* Search CSV data with header using basic query with index identifier,
   and the result is empty
  */
  @Test
  void testSearchNoTargetExist() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=-0.12345;1;idx");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "-0.12345;1;idx");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /*Test Wrong format of query
   * */
  @Test
  void testWrongQuery() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=-0.12345;1");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "-0.12345;1");
    ExpecteResponseMap.put("result", "error_bad_request");
    ExpecteResponseMap.put("request", query);
    ExpecteResponseMap.put("detail", "Wrong query format! Received 2 args, but should be 1 or 3");

    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /* test not query*/
  @Test
  void testNotQuery() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=not(-0.01729;2;idx)");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "not(-0.01729;2;idx)");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    expected.add(List.of(new String[] {"0", "Sol", "0", "0", "0"}));
    expected.add(List.of(new String[] {"1", "", "282.43485", "0.00449", "5.36884"}));
    expected.add(List.of(new String[] {"2", "", "43.04329", "0.00285", "-15.24144"}));
    expected.add(List.of(new String[] {"3", "", "277.11358", "0.02422", "223.27753"}));
    expected.add(List.of(new String[] {"3759", "96 G. Psc", "7.26388", "1.55643", "0.68697"}));
    expected.add(
        List.of(new String[] {"70667", "Proxima Centauri", "-0.47175", "-0.36132", "-1.15037"}));
    expected.add(
        List.of(new String[] {"71454", "Rigel Kentaurus B", "-0.50359", "-0.42128", "-1.1767"}));
    expected.add(
        List.of(new String[] {"71457", "Rigel Kentaurus A", "-0.50362", "-0.42139", "-1.17665"}));
    expected.add(List.of(new String[] {"118721", "", "-2.28262", "0.64697", "0.29354"}));
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }

  /* test or query*/
  @Test
  void testOrQuery() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=or(-0.01729;2;idx,Proxima%20Centauri)");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "or(-0.01729;2;idx,Proxima Centauri)");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    expected.add(
        List.of(new String[] {"70667", "Proxima Centauri", "-0.47175", "-0.36132", "-1.15037"}));
    expected.add(
        List.of(new String[] {"87666", "Barnard's Star", "-0.01729", "-1.81533", "0.14824"}));
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }
  /* test and query*/
  @Test
  void testAndQuery() throws Exception {
    HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=ten-star.csv&header=true");
    assertEquals(200, clientConnection.getResponseCode());
    clientConnection = tryRequest("searchcsv?query=and(-0.01729;2;idx,Barnard's%20Star)");
    assertEquals(200, clientConnection.getResponseCode());
    GeneralResponse response =
        new GeneralResponse().deserialize(new Buffer().readFrom(clientConnection.getInputStream()));
    Map<String, Object> ExpecteResponseMap = new HashMap<>();
    Map<String, Object> query = new HashMap<>();
    query.put("query", "and(-0.01729;2;idx,Barnard's Star)");
    ExpecteResponseMap.put("request", query);
    List<List<String>> expected = new ArrayList<>();
    expected.add(
        List.of(new String[] {"87666", "Barnard's Star", "-0.01729", "-1.81533", "0.14824"}));
    ExpecteResponseMap.put("search result", expected);
    assertEquals(response.responseMap(), ExpecteResponseMap);
    clientConnection.disconnect();
  }
}
