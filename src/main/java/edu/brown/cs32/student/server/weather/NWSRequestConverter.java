package edu.brown.cs32.student.server.weather;

import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import okio.Buffer;
import okio.BufferedSource;

public class NWSRequestConverter {
  private static final DecimalFormat df = new DecimalFormat("0.0000");

  /**
   * convert lat and Lon request to hourly forecast url
   *
   * @param request
   * @return
   * @throws Exception
   */
  public Data.ForecastAPIPeriod convertNWSRequest(Data.WeatherRequest request)
      throws IOException, ForecastNotFoundInNWSDataException, TimeNotFoundInNWSDataException {
    String addr =
        "https://api.weather.gov/points/"
            + df.format(request.lat())
            + ","
            + df.format(request.lon());
    try {
      URL requestURL = new URL(addr);
      HttpURLConnection clientConnection1 = (HttpURLConnection) requestURL.openConnection();
      BufferedSource buffer1 = new Buffer().readFrom(clientConnection1.getInputStream());
      Moshi moshi = new Moshi.Builder().build();
      Data.PointAPIData pointAPIData = moshi.adapter(Data.PointAPIData.class).fromJson(buffer1);
      clientConnection1.disconnect();

      if (pointAPIData.properties().forecastHourly() == null) {
        throw new ForecastNotFoundInNWSDataException(
            "Unable to provide data for requested URL: " + addr);
      }

      URL requestForecastURL = new URL(pointAPIData.properties().forecastHourly());
      HttpURLConnection clientConnection2 = (HttpURLConnection) requestForecastURL.openConnection();
      Data.ForecastAPIData forecastAPIData =
          moshi
              .adapter(Data.ForecastAPIData.class)
              .fromJson(new Buffer().readFrom(clientConnection2.getInputStream()));
      clientConnection2.disconnect();
      Data.ForecastAPIPeriod findPeriod = null;
      for (Data.ForecastAPIPeriod period : forecastAPIData.properties().periods()) {
        OffsetDateTime startTime = OffsetDateTime.parse(period.startTime());
        OffsetDateTime endTime = OffsetDateTime.parse(period.endTime());
        if (fallIntoRange(startTime, endTime, request.datetime())) {
          findPeriod = period;
          break;
        }
      }
      if (findPeriod == null) {
        throw new TimeNotFoundInNWSDataException(
            "Unable to provide data for requested time: " + request.datetime().toString());
      }
      return findPeriod;
    } catch (IOException e) {
      throw new IOException("Unable to provide data for requested URL: " + addr);
    } catch (ForecastNotFoundInNWSDataException | TimeNotFoundInNWSDataException e) {
      throw e;
    }
  }

  private boolean fallIntoRange(OffsetDateTime start, OffsetDateTime end, OffsetDateTime req) {
    return req.isEqual(start) || (req.isAfter(start) && req.isBefore(end));
  }
}
