/*******************************************************************************
 * Copyright 2013 SAP AG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.sap.core.odata.api.processor;

import java.util.Set;

import com.sap.core.odata.api.commons.HttpStatusCodes;
import com.sap.core.odata.api.rt.RuntimeDelegate;

/**
 * An <code>ODataResponse</code> is usually created by a {@link ODataProcessor} during request handling. The handler can use a serializer to create an 
 * OData body (== response entity) and can set various response headers.<p>
 * A respons can be created using the builder pattern:
 * <pre>
 * {@code
 * ODataResponse response = ODataResponse.entity("hello world").setStatus(200).build();
 * }
 * </pre>
 * @author SAP AG
 */
public abstract class ODataResponse {

  /**
   * Do not subclass ODataResponse!
   */
  protected ODataResponse() {}

  /**
   * @return http status code of this response
   */
  public abstract HttpStatusCodes getStatus();

  /**
   * @return a response entity which become the body part of a response message
   */
  public abstract Object getEntity();

  /**
   * @param name http response header name
   * @return a header value or null if not set
   */
  public abstract String getHeader(String name);

  /**
   * @return content header value or null if not set
   */
  public abstract String getContentHeader();

  public abstract String getIdLiteral();

  /**
   * @return etag value or null if not available
   */
  public abstract String getETag();

  /**
   * @return a set of all available header names
   */
  public abstract Set<String> getHeaderNames();

  /**
   * Case insensitive check if the header is available in this ODataResponse
   * @param header
   * @return true/false
   */
  public abstract boolean containsHeader(String header);

  /**
   * @param status http status code
   * @return a builder object
   */
  public static ODataResponseBuilder status(final HttpStatusCodes status) {
    final ODataResponseBuilder b = ODataResponseBuilder.newInstance();
    b.status(status);
    return b;
  }

  /**
   * @param response
   * @return a new builder object
   */
  public static ODataResponseBuilder fromResponse(final ODataResponse response) {
    final ODataResponseBuilder b = ODataResponseBuilder.newInstance();
    b.fromResponse(response);
    return b;
  }

  /**
   * @param entity
   * @return a builder object
   */
  public static ODataResponseBuilder entity(final Object entity) {
    final ODataResponseBuilder b = ODataResponseBuilder.newInstance();
    b.entity(entity);
    return b;
  }

  /**
   * @param name http header name
   * @param value associated value
   * @return a builder object
   */
  public static ODataResponseBuilder header(final String name, final String value) {
    final ODataResponseBuilder b = ODataResponseBuilder.newInstance();
    b.header(name, value);
    return b;
  }

  /**
   * @param value content header value
   * @return a builder object
   */
  public static ODataResponseBuilder contentHeader(final String value) {
    final ODataResponseBuilder b = ODataResponseBuilder.newInstance();
    b.contentHeader(value);
    return b;
  }

  /**
   * @return returns a new builder object
   */
  public static ODataResponseBuilder newBuilder() {
    return ODataResponseBuilder.newInstance();
  }

  /**
   * Implementation of the builder pattern to create instances of these type of object. 
   * @author SAP AG
   */
  public static abstract class ODataResponseBuilder {

    protected ODataResponseBuilder() {}

    private static ODataResponseBuilder newInstance() {
      final ODataResponseBuilder b = RuntimeDelegate.createODataResponseBuilder();
      return b;
    }

    public abstract ODataResponse build();

    public abstract ODataResponseBuilder status(HttpStatusCodes status);

    public abstract ODataResponseBuilder entity(Object entity);

    public abstract ODataResponseBuilder header(String name, String value);

    public abstract ODataResponseBuilder idLiteral(String idLiteral);

    public abstract ODataResponseBuilder eTag(String eTag);

    public abstract ODataResponseBuilder contentHeader(String contentHeader);

    protected abstract ODataResponseBuilder fromResponse(ODataResponse response);
  }
}
