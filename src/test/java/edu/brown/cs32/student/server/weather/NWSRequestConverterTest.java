package edu.brown.cs32.student.server.weather;

import static org.junit.jupiter.api.Assertions.*;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NWSRequestConverterTest {

  DecimalFormat df = new DecimalFormat("0.0000");
  NWSRequestConverter nc = new NWSRequestConverter();

  /**
   * Test converter can convert a valid request
   *
   * @throws Exception
   */
  @Test
  void testConvertNWSRequestSuccess() throws Exception {
    OffsetDateTime reqTime = OffsetDateTime.now();
    Data.WeatherRequest request = new Data.WeatherRequest(41.8268, -71.4029, reqTime);
    Data.ForecastAPIPeriod findPeriod = nc.convertNWSRequest(request);
    LocalDateTime startTime = OffsetDateTime.parse(findPeriod.startTime()).toLocalDateTime();
    LocalDateTime endTime = OffsetDateTime.parse(findPeriod.endTime()).toLocalDateTime();
    LocalDateTime currTime = reqTime.toLocalDateTime();
    Duration duration1 = Duration.between(startTime, currTime);
    Duration duration2 = Duration.between(currTime, endTime);
    // difference of (currTime - responseTime) i in the range of [-1 ~ 1]h
    boolean timeClose =
        duration1.compareTo(Duration.ofHours(0)) >= 0
            && duration2.compareTo(Duration.ofHours(0)) >= 0;
    assertEquals(timeClose, true);
  }

  /** Test converter cannot convert a invalid request */
  @Test
  void testConvertNWSRequestFailure() {
    List<Double> latLon = new ArrayList<>(List.of(8.0000, -71.4029));
    try {
      OffsetDateTime reqTime = OffsetDateTime.now();
      Data.WeatherRequest request = new Data.WeatherRequest(8.0000, -71.4029, reqTime);
      nc.convertNWSRequest(request);
    } catch (Exception e) {
      String expected =
          "Unable to provide data for requested URL: https://api.weather.gov/points/8.0000,-71.4029";
      assertEquals(e.getMessage(), expected);
    }
  }
}
