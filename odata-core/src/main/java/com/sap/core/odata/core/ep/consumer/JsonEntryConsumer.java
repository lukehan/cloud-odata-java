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
package com.sap.core.odata.core.ep.consumer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.stream.JsonReader;
import com.sap.core.odata.api.edm.Edm;
import com.sap.core.odata.api.edm.EdmEntitySet;
import com.sap.core.odata.api.edm.EdmException;
import com.sap.core.odata.api.edm.EdmMultiplicity;
import com.sap.core.odata.api.edm.EdmNavigationProperty;
import com.sap.core.odata.api.ep.EntityProviderException;
import com.sap.core.odata.api.ep.EntityProviderReadProperties;
import com.sap.core.odata.api.ep.callback.OnReadInlineContent;
import com.sap.core.odata.api.ep.callback.ReadEntryResult;
import com.sap.core.odata.api.ep.callback.ReadFeedResult;
import com.sap.core.odata.api.ep.entry.ODataEntry;
import com.sap.core.odata.api.ep.feed.ODataFeed;
import com.sap.core.odata.core.ep.aggregator.EntityInfoAggregator;
import com.sap.core.odata.core.ep.aggregator.EntityPropertyInfo;
import com.sap.core.odata.core.ep.aggregator.NavigationPropertyInfo;
import com.sap.core.odata.core.ep.entry.EntryMetadataImpl;
import com.sap.core.odata.core.ep.entry.MediaMetadataImpl;
import com.sap.core.odata.core.ep.entry.ODataEntryImpl;
import com.sap.core.odata.core.ep.util.FormatJson;
import com.sap.core.odata.core.uri.ExpandSelectTreeNodeImpl;

public class JsonEntryConsumer {

  private final Map<String, Object> properties = new HashMap<String, Object>();
  private final MediaMetadataImpl mediaMetadata = new MediaMetadataImpl();
  private final EntryMetadataImpl entryMetadata = new EntryMetadataImpl();
  private final ExpandSelectTreeNodeImpl expandSelectTree = new ExpandSelectTreeNodeImpl();
  private final Map<String, Object> typeMappings;
  private final EntityInfoAggregator eia;
  private final JsonReader reader;
  private final EntityProviderReadProperties readProperties;
  private final ODataEntryImpl entryResult;

  public JsonEntryConsumer(final JsonReader reader, final EntityInfoAggregator eia, final EntityProviderReadProperties readProperties) {
    typeMappings = readProperties.getTypeMappings();
    this.eia = eia;
    this.readProperties = readProperties;
    this.reader = reader;
    entryResult = new ODataEntryImpl(properties, mediaMetadata, entryMetadata, expandSelectTree);
  }

  public ODataEntry readSingleEntry() throws EntityProviderException {
    try {
      reader.beginObject();
      String nextName = reader.nextName();
      if (FormatJson.D.equals(nextName)) {
        reader.beginObject();
        readEntryContent();
        reader.endObject();
      } else {
        handleName(nextName);
        readEntryContent();
      }
      reader.endObject();
    } catch (IOException e) {
      throw new EntityProviderException(EntityProviderException.COMMON, e);
    } catch (EdmException e) {
      throw new EntityProviderException(EntityProviderException.COMMON, e);
    }

    return entryResult;
  }

  public ODataEntry readFeedEntry() throws EdmException, EntityProviderException, IOException {
    reader.beginObject();
    readEntryContent();
    reader.endObject();
    return entryResult;
  }

  private void readEntryContent() throws IOException, EdmException, EntityProviderException {
    while (reader.hasNext()) {
      String name = reader.nextName();
      handleName(name);
    }

    //TODO: Ca validate created entry
  }

  private void handleName(final String name) throws IOException, EdmException, EntityProviderException {
    if (FormatJson.METADATA.equals(name)) {
      readMetadata();
      validateMetadata();
    } else {
      EntityPropertyInfo propertyInfo = eia.getPropertyInfo(name);
      if (propertyInfo != null) {
        JsonPropertyConsumer jpc = new JsonPropertyConsumer();
        Object propertyValue = jpc.readPropertyValue(reader, propertyInfo, typeMappings.get(name));
        if (properties.containsKey(name)) {
          throw new EntityProviderException(EntityProviderException.DOUBLE_PROPERTY.addContent(name));
        }
        properties.put(name, propertyValue);
      } else {
        readNavigationProperty(name);
      }
    }
  }

  private void readMetadata() throws IOException, EdmException, EntityProviderException {
    String name = null;
    reader.beginObject();
    while (reader.hasNext()) {
      name = reader.nextName();
      String value = reader.nextString();

      if (FormatJson.ID.equals(name)) {
        entryMetadata.setId(value);
      } else if (FormatJson.URI.equals(name)) {
        entryMetadata.setUri(value);
      } else if (FormatJson.TYPE.equals(name)) {
        String fullQualifiedName = eia.getEntityType().getNamespace() + Edm.DELIMITER + eia.getEntityType().getName();
        if (!fullQualifiedName.equals(value)) {
          throw new EntityProviderException(EntityProviderException.INVALID_ENTITYTYPE.addContent(fullQualifiedName).addContent(value));
        }
      } else if (FormatJson.ETAG.equals(name)) {
        entryMetadata.setEtag(value);
      } else if (FormatJson.EDIT_MEDIA.equals(name)) {
        mediaMetadata.setEditLink(value);
      } else if (FormatJson.MEDIA_SRC.equals(name)) {
        mediaMetadata.setSourceLink(value);
      } else if (FormatJson.MEDIA_ETAG.equals(name)) {
        mediaMetadata.setEtag(value);
      } else if (FormatJson.CONTENT_TYPE.equals(name)) {
        mediaMetadata.setContentType(value);
      } else {
        throw new EntityProviderException(EntityProviderException.INVALID_CONTENT.addContent(name).addContent(FormatJson.METADATA));
      }
    }

    reader.endObject();
  }

  private void validateMetadata() throws EdmException, EntityProviderException {
    if (eia.getEntityType().hasStream()) {
      if (mediaMetadata.getSourceLink() == null) {
        throw new EntityProviderException(EntityProviderException.MISSING_ATTRIBUTE.addContent(FormatJson.MEDIA_SRC).addContent(FormatJson.METADATA));
      }
      if (mediaMetadata.getContentType() == null) {
        throw new EntityProviderException(EntityProviderException.MISSING_ATTRIBUTE.addContent(FormatJson.CONTENT_TYPE).addContent(FormatJson.METADATA));
      }
      //TODO Mime Type Mapping
    } else {
      if (mediaMetadata.getContentType() != null || mediaMetadata.getEditLink() != null
          || mediaMetadata.getEtag() != null || mediaMetadata.getSourceLink() != null) {
        throw new EntityProviderException(EntityProviderException.MEDIA_DATA_NOT_INITIAL);
      }
    }
  }

  private void readNavigationProperty(final String navigationPropertyName) throws IOException, EntityProviderException, EdmException {
    NavigationPropertyInfo navigationPropertyInfo = eia.getNavigationPropertyInfo(navigationPropertyName);
    if (navigationPropertyInfo == null) {
      throw new EntityProviderException(EntityProviderException.ILLEGAL_ARGUMENT.addContent(navigationPropertyName));
    }

    reader.beginObject();
    String name = reader.nextName();
    if (FormatJson.DEFERRED.equals(name)) {
      reader.beginObject();
      String uri = reader.nextName();
      if (FormatJson.URI.equals(uri)) {
        String value = reader.nextString();
        entryMetadata.putAssociationUri(navigationPropertyInfo.getName(), value);
      } else {
        throw new EntityProviderException(EntityProviderException.ILLEGAL_ARGUMENT.addContent(uri));
      }
      reader.endObject();
    } else {
      EdmNavigationProperty navigationProperty = (EdmNavigationProperty) eia.getEntityType().getProperty(navigationPropertyName);
      EdmEntitySet inlineEntitySet = eia.getEntitySet().getRelatedEntitySet(navigationProperty);
      EntityInfoAggregator inlineEia = EntityInfoAggregator.create(inlineEntitySet);
      EntityProviderReadProperties inlineReadProperties;
      ExpandSelectTreeNodeImpl expandSelectTreeNodeImpl = new ExpandSelectTreeNodeImpl();
      expandSelectTreeNodeImpl.setAllExplicitly();
      expandSelectTree.putLinkNode(navigationPropertyName, expandSelectTreeNodeImpl);
      OnReadInlineContent callback = readProperties.getCallback();
      if (callback == null) {
        inlineReadProperties = EntityProviderReadProperties.init().mergeSemantic(readProperties.getMergeSemantic()).build();

      } else {
        inlineReadProperties = callback.receiveReadProperties(readProperties, navigationProperty);
      }

      if (navigationProperty.getMultiplicity() == EdmMultiplicity.ONE) {
        JsonEntryConsumer inlineConsumer = new JsonEntryConsumer(reader, inlineEia, inlineReadProperties);
        ODataEntry entry = inlineConsumer.readInlineEntry(name);
        if (callback == null) {
          properties.put(navigationPropertyName, entry);
          entryResult.setContainsInlineEntry(true);
        } else {
          ReadEntryResult result = new ReadEntryResult(inlineReadProperties, navigationProperty, entry);
          callback.handleReadEntry(result);
        }
      } else {
        JsonFeedConsumer inlineConsumer = new JsonFeedConsumer(reader, inlineEia, inlineReadProperties);
        ODataFeed feed = inlineConsumer.readInlineFeed(name);
        if (callback == null) {
          properties.put(navigationPropertyName, feed);
          entryResult.setContainsInlineEntry(true);
        } else {
          ReadFeedResult result = new ReadFeedResult(inlineReadProperties, navigationProperty, feed);
          callback.handleReadFeed(result);
        }
      }

    }
    reader.endObject();
  }

  private ODataEntry readInlineEntry(final String name) throws EdmException, EntityProviderException, IOException {
    //consume the already started content
    handleName(name);
    //consume the rest of the entry content
    readEntryContent();
    return entryResult;
  }

}
