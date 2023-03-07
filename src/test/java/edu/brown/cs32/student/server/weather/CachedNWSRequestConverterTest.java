package edu.brown.cs32.student.server.weather;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.cache.CacheStats;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class CachedNWSRequestConverterTest {

  /**
   * Test cached converter can convert a valid request
   *
   * @throws Exception
   */
  @Test
  void testConvertNWSRequestSuccess() throws Exception {
    CachedNWSRequestConverter cnc = new CachedNWSRequestConverter(new NWSRequestConverter(), 10, 1);
    OffsetDateTime reqTime = OffsetDateTime.now();
    Data.WeatherRequest request = new Data.WeatherRequest(41.8268, -71.4029, reqTime);
    Data.ForecastAPIPeriod findPeriod = cnc.convertNWSRequest(request);
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

  /**
   * Test cache cached one URL
   *
   * @throws Exception
   */
  @Test
  void testCacheOne() throws Exception {
    CachedNWSRequestConverter cnc = new CachedNWSRequestConverter(new NWSRequestConverter(), 10, 1);
    Data.WeatherRequest request = new Data.WeatherRequest(41.8268, -71.4029, OffsetDateTime.now());
    cnc.convertNWSRequest(request);
    CacheStats stats = cnc.getCache().stats();
    assertEquals(stats.loadSuccessCount(), 1);
    assertEquals(stats.missCount(), 1);
  }

  /**
   * Test cache throw exception with invalid request
   *
   * @throws Exception
   */
  @Test
  void testCacheException() throws Exception {
    CachedNWSRequestConverter cnc = new CachedNWSRequestConverter(new NWSRequestConverter(), 10, 1);
    Data.WeatherRequest request = new Data.WeatherRequest(41.8268, -71.4029, OffsetDateTime.now());
    try {
      cnc.convertNWSRequest(request);
    } catch (Exception e) {
      CacheStats stats = cnc.getCache().stats();
      assertEquals(stats.loadExceptionCount(), 1);
    }
  }

  /**
   * Test cache hit
   *
   * @throws Exception
   */
  @Test
  void testCacheHit() throws Exception {
    CachedNWSRequestConverter cnc = new CachedNWSRequestConverter(new NWSRequestConverter(), 10, 1);
    Data.WeatherRequest request1 = new Data.WeatherRequest(41.8268, -71.4029, OffsetDateTime.now());
    cnc.convertNWSRequest(request1);
    Data.WeatherRequest request2 = new Data.WeatherRequest(41.8268, -71.4000, OffsetDateTime.now());
    cnc.convertNWSRequest(request2);
    CacheStats stats = cnc.getCache().stats();
    assertEquals(stats.loadSuccessCount(), 1);
    assertEquals(stats.hitCount(), 1);
  }

  /**
   * Test cache miss: distance
   *
   * @throws Exception
   */
  @Test
  void testCacheMissDistance() throws Exception {
    CachedNWSRequestConverter cnc = new CachedNWSRequestConverter(new NWSRequestConverter(), 10, 1);
    Data.WeatherRequest request1 = new Data.WeatherRequest(41.8268, -71.4029, OffsetDateTime.now());
    cnc.convertNWSRequest(request1);
    Data.WeatherRequest request2 = new Data.WeatherRequest(41.8268, -71.0000, OffsetDateTime.now());
    cnc.convertNWSRequest(request2);
    CacheStats stats = cnc.getCache().stats();
    assertEquals(stats.loadSuccessCount(), 2);
    assertEquals(stats.missCount(), 2);
  }

  /**
   * Test cache miss: time mvn
   *
   * @throws Exception
   */
  @Test
  void testCacheMissTime() throws Exception {
    CachedNWSRequestConverter cnc = new CachedNWSRequestConverter(new NWSRequestConverter(), 10, 1);
    Data.WeatherRequest request1 = new Data.WeatherRequest(41.8268, -71.4029, OffsetDateTime.now());
    cnc.convertNWSRequest(request1);
    Data.WeatherRequest request2 =
        new Data.WeatherRequest(41.8268, -71.4029, OffsetDateTime.now().minusHours(-2));
    cnc.convertNWSRequest(request2);
    CacheStats stats = cnc.getCache().stats();
    assertEquals(stats.loadSuccessCount(), 2);
    assertEquals(stats.missCount(), 2);
  }

  /**
   * Test cache expire
   *
   * @throws Exception
   */
  @Test
  void testCacheExpire() throws Exception {
    CachedNWSRequestConverter cnc = new CachedNWSRequestConverter(new NWSRequestConverter(), 10, 1);
    Data.WeatherRequest request = new Data.WeatherRequest(41.8268, -71.4029, OffsetDateTime.now());
    cnc.convertNWSRequest(request);
    Thread.sleep(1001);
    cnc.convertNWSRequest(request);
    CacheStats stats = cnc.getCache().stats();
    assertEquals(stats.evictionCount(), 1);
  }

  /**
   * Test cache exceed size
   *
   * @throws Exception
   */
  @Test
  void testCacheExceedSize() throws Exception {
    CachedNWSRequestConverter cnc = new CachedNWSRequestConverter(new NWSRequestConverter(), 1, 1);
    Data.WeatherRequest request1 = new Data.WeatherRequest(41.8268, -71.4029, OffsetDateTime.now());
    cnc.convertNWSRequest(request1);
    Data.WeatherRequest request2 = new Data.WeatherRequest(41.8268, -71.4029, OffsetDateTime.now());
    cnc.convertNWSRequest(request2);
    assertEquals(cnc.getCache().size(), 1);
  }
}
