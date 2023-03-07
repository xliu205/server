package edu.brown.cs32.student.server.weather;

import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.brown.cs32.student.server.GeneralResponse;
import edu.brown.cs32.student.server.MissingArgException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.utils.StringUtils;

/** Handler class for the weather API endpoint. */
public class WeatherHandler implements Route {
  private final CachedNWSRequestConverter cachedNWSRequestConverter;

  /** constructor */
  public WeatherHandler() {
    cachedNWSRequestConverter = new CachedNWSRequestConverter(new NWSRequestConverter(), 10, 60);
  }

  /**
   * get weather data using NWS API
   *
   * @param request the request to handle
   * @param response use to modify properties of the response
   * @return response content
   * @throws Exception This is part of the interface; we don't have to throw anything.
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    Map<String, Object> responseMap = new HashMap<>();
    String lat = request.queryParams("lat");
    String lon = request.queryParams("lon");
    String datetime = request.queryParams("datetime");
    OffsetDateTime reqTime = null;
    try {
      if (StringUtils.isBlank(lat) && StringUtils.isBlank(lon)) {
        throw new MissingArgException(new ArrayList<String>(Arrays.asList("lat", "lon")));
      } else if (StringUtils.isBlank(lat)) {
        throw new MissingArgException(new ArrayList<String>(Arrays.asList("lat")));
      } else if (StringUtils.isBlank(lon)) {
        throw new MissingArgException(new ArrayList<String>(Arrays.asList("lon")));
      }
      if (StringUtils.isBlank(datetime)) {
        reqTime = OffsetDateTime.now();
      } else {
        reqTime = OffsetDateTime.parse(datetime);
      }
      Data.WeatherRequest weatherRequest =
          new Data.WeatherRequest(Double.parseDouble(lat), Double.parseDouble(lon), reqTime);
      Data.ForecastAPIPeriod findPeriod =
          cachedNWSRequestConverter.convertNWSRequest(weatherRequest);
      Data.StringWeatherRequest stringWeatherRequest =
          new Data.StringWeatherRequest(lat, lon, datetime);
      responseMap.put("result", "success");
      responseMap.put("request", stringWeatherRequest);
      responseMap.put("time", findPeriod.startTime());
      responseMap.put("temperature", findPeriod.temperature() + findPeriod.temperatureUnit());
    }
    //    catch (IOException e) {
    //      responseMap.put("result", "error_datasource");
    //      if (e.getMessage().indexOf("Unable") == -1) {
    //        DecimalFormat df = new DecimalFormat("0.0000");
    //        List<Double> latLon =
    //            new ArrayList<>(List.of(Double.parseDouble(lat), Double.parseDouble(lon)));
    //        String addr =
    //            "https://api.weather.gov/points/"
    //                + df.format(latLon.get(0))
    //                + ","
    //                + df.format(latLon.get(1));
    //        responseMap.put("detail", "Unable to provide data for requested URL: " + addr);
    //      } else responseMap.put("detail", e.getMessage());
    //    }
    catch (DateTimeParseException e) {
      responseMap.put("result", "error_bad_request");
      responseMap.put("detail", "Invalid datetime format: " + datetime);
    } catch (TimeNotFoundInNWSDataException | ForecastNotFoundInNWSDataException e) {
      responseMap.put("result", "error_datasource");
      responseMap.put("detail", e.getMessage());
    } catch (MissingArgException e) {
      responseMap.put("result", "error_bad_request");
      responseMap.put("detail", e.getMessage());
    } catch (UncheckedExecutionException e) {
      responseMap.put("result", "error_datasource");
      responseMap.put("detail", e.getMessage().split(":", 2)[1].trim());
    } catch (NumberFormatException e) {
      responseMap.put("result", "error_datasource");
      responseMap.put("detail", "lat=" + lat + ", lon=" + lon + " cannot be converted to numbers");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new GeneralResponse(responseMap).serialize();
  }
}
