package com.eyelinecom.whoisd.sads2.line.api.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LineContacts {

  // in case of error
  @JsonProperty(value = "statusCode")
  Integer statusCode;
  @JsonProperty(value = "statusMessage")
  String statusMessage;



  @JsonProperty(value = "contacts")
  Contact[] contacts;

  @JsonProperty(value = "count")
  Integer count;

  public Integer getStatusCode() {
    return statusCode;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public boolean isError() {
    return statusCode != null;
  }

  public static class Contact {

    @JsonProperty(value = "displayName")
    String displayName;

    @JsonProperty(value = "mid")
    String mid;

    @JsonProperty(value = "pictureUrl")
    String pictureUrl;

    @JsonProperty(value = "statusMessage")
    String statusMessage;
  }

}
