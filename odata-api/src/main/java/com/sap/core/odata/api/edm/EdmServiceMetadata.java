package com.sap.core.odata.api.edm;

public interface EdmServiceMetadata {

  //TODO completely, Exception Handling

  byte[] getMetadata();

  String getDataServiceVersion();

  // TODO
  // EdmEntityContainerInfo getEntityContainerInfo;
}