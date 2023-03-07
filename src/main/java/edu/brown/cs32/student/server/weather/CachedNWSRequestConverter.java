package edu.brown.cs32.student.server.weather;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/** NWSRequestConverter with cache */
public class CachedNWSRequestConverter {
  private final NWSRequestConverter wrappedConverter;

  private final LoadingCache<Data.WeatherRequest, Data.ForecastAPIPeriod> cache;

  /**
   * Constructor
   *
   * @param wrappedConverter
   * @param cacheSize
   * @param cacheTime
   */
  public CachedNWSRequestConverter(
      NWSRequestConverter wrappedConverter, int cacheSize, int cacheTime) {
    this.wrappedConverter = wrappedConverter;
    this.cache =
        CacheBuilder.newBuilder()
            // How many entries maximum in the cache?
            .maximumSize(cacheSize)
            // How long should entries remain in the cache?
            .expireAfterWrite(cacheTime, TimeUnit.SECONDS)
            // Keep statistical info around for profiling purposes
            .recordStats()
            .build(
                new CacheLoader<Data.WeatherRequest, Data.ForecastAPIPeriod>() {
                  @Override
                  public Data.ForecastAPIPeriod load(Data.WeatherRequest key)
                      throws IOException, ForecastNotFoundInNWSDataException,
                          TimeNotFoundInNWSDataException {
                    // If this isn't yet present in the cache, load it:
                    return wrappedConverter.convertNWSRequest(key);
                  }
                });
  }

  /**
   * Cache getter
   *
   * @return local cache
   */
  public LoadingCache<Data.WeatherRequest, Data.ForecastAPIPeriod> getCache() {
    return cache;
  }

  /**
   * Find points in the cache and get the url, if not found, load into the cache
   *
   * @param latLon
   * @return url
   * @throws Exception
   */
  public Data.ForecastAPIPeriod convertNWSRequest(Data.WeatherRequest request) throws Exception {
    Data.ForecastAPIPeriod result = null;
    boolean hit = false;
    // System.out.println(latLon.toString());
    for (Data.WeatherRequest key : cache.asMap().keySet()) {
      Double latKey = key.lat();
      Double lonKey = key.lon();
      double dist = distance(request.lat(), request.lon(), latKey, lonKey);
      // System.out.println(key.toString());
      // System.out.println(dist);
      LocalDateTime timeKey = key.datetime().toLocalDateTime();
      LocalDateTime reqTime = request.datetime().toLocalDateTime();
      Duration duration = Duration.between(timeKey, reqTime);
      // difference of (currTime - responseTime) i in the range of [-1 ~ 1]h
      boolean timeClose =
          duration.compareTo(Duration.ofHours(-1)) >= 0
              && duration.compareTo(Duration.ofHours(1)) <= 0;
      if (dist <= 3 && timeClose) {
        hit = true;
        result = cache.getUnchecked(key);
        break;
      }
    }
    if (!hit) result = cache.getUnchecked(request);
    // System.out.println(cache.stats());
    return result;
  }

  /** calculate lat lon distance */
  private double distance(double lat1, double lon1, double lat2, double lon2) {
    double theta = lon1 - lon2;
    double dist =
        Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
            + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
    dist = Math.acos(dist);
    dist = rad2deg(dist);
    dist = dist * 60 * 1.1515;
    return (dist);
  }

  /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
  /*::  This function converts decimal degrees to radians             :*/
  /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
  private double deg2rad(double deg) {
    return (deg * Math.PI / 180.0);
  }

  /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
  /*::  This function converts radians to decimal degrees             :*/
  /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
  private double rad2deg(double rad) {
    return (rad * 180.0 / Math.PI);
  }
}
