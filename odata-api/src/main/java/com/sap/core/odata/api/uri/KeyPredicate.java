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
package com.sap.core.odata.api.uri;

import com.sap.core.odata.api.edm.EdmProperty;

/**
 * Key predicate, consisting of a simple-type property and its value as String literal
 * @com.sap.core.odata.DoNotImplement
 * @author SAP AG
 */
public interface KeyPredicate {

  /**
   * <p>Gets the literal String in default representation.</p>
   * <p>The description for {@link com.sap.core.odata.api.edm.EdmLiteral} has some motivation for using
   * this representation.</p> 
   * @return String literal in default (<em>not</em> URI) representation
   * @see com.sap.core.odata.api.edm.EdmLiteralKind
   */
  public String getLiteral();

  /**
   * Gets the key property.
   * @return {@link EdmProperty} simple-type property
   */
  public EdmProperty getProperty();

}
