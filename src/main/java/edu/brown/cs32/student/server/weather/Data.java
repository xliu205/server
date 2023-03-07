package edu.brown.cs32.student.server.weather;

import java.time.OffsetDateTime;
import java.util.List;

public class Data {
  /**
   * PointAPIData
   *
   * @param properties
   */
  public record PointAPIData(PointAPIProperties properties) {}

  /**
   * PointAPIProperties
   *
   * @param forecastHourly
   */
  public record PointAPIProperties(String forecastHourly) {}

  /**
   * ForecastAPIData
   *
   * @param properties
   */
  public record ForecastAPIData(ForecastAPIProperties properties) {}

  /**
   * ForecastAPIProperties
   *
   * @param periods
   */
  public record ForecastAPIProperties(List<ForecastAPIPeriod> periods) {}

  /**
   * ForecastAPIPeriod
   *
   * @param startTime
   * @param endTime
   * @param temperature
   * @param temperatureUnit
   */
  public record ForecastAPIPeriod(
      String startTime, String endTime, int temperature, String temperatureUnit) {}

  /**
   * WeatherRequest
   *
   * @param lat
   * @param lon
   * @param datetime
   */
  public record StringWeatherRequest(String lat, String lon, String datetime) {}

  public record WeatherRequest(double lat, double lon, OffsetDateTime datetime) {}
}
