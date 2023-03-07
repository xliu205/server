package edu.brown.cs32.student.server;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.lang.reflect.Type;
import java.util.Map;
import okio.BufferedSource;

/**
 * General Response class
 *
 * @param responseMap
 */
public record GeneralResponse(Map<String, Object> responseMap) {
  /** constructor */
  public GeneralResponse() {
    this(null);
  }
  /**
   * @return this response, serialized as Json
   */
  public String serialize() throws Exception {
    try {
      Moshi moshi = new Moshi.Builder().build();
      Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
      return moshi.adapter(type).toJson(responseMap);
    } catch (Exception e) {
      // For debugging purposes, show in the console _why_ this fails
      // Otherwise we'll just get an error 500 from the API in integration
      // testing.
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * deserialize buffered json string to GeneralResponse class
   *
   * @param buffer
   * @return
   * @throws Exception
   */
  public GeneralResponse deserialize(BufferedSource buffer) throws Exception {
    try {
      Moshi moshi = new Moshi.Builder().build();
      Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
      Map<String, Object> response = (Map<String, Object>) moshi.adapter(type).fromJson(buffer);
      return new GeneralResponse(response);
    } catch (Exception e) {
      // For debugging purposes, show in the console _why_ this fails
      // Otherwise we'll just get an error 500 from the API in integration
      // testing.
      e.printStackTrace();
      throw e;
    }
  }
}
