package com.eyelinecom.whoisd.sads2.line.api.types;

import com.eyelinecom.whoisd.sads2.events.Event;
import com.eyelinecom.whoisd.sads2.eventstat.LoggableExternalRequest;
import com.eyelinecom.whoisd.sads2.profile.Profile;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LineCallback {

  @JsonProperty(value = "result")
  private Request[] result;

  public Request[] getRequest() {
    return result;
  }

  public String getText() {
    return result[0].content.text;
  }

  public void setServiceId(String serviceId) {
    for (Request request : result) {
      request.setServiceId(serviceId);
    }
  }

  public static class Request implements LoggableExternalRequest {

    @JsonProperty(value = "content")
    private Content content;

    @JsonProperty(value = "eventType")
    private Long eventType;

    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "from")
    private String from;

    @JsonProperty(value = "to")
    private String[] to;

    @JsonProperty(value = "fromChannel")
    private Integer fromChannel;

    @JsonProperty(value = "toChannel")
    private Integer toChannel;

    private transient String serviceId;
    private transient Profile profile;
    private transient Event event;

    public String getText() {
      return content.text;
    }

    public void setServiceId(String serviceId) {
      this.serviceId = serviceId;
    }

    public Profile getProfile() {
      return profile;
    }

    public void setProfile(Profile profile) {
      this.profile = profile;
    }

    public Event getEvent() {
      return event;
    }

    public void setEvent(Event event) {
      this.event = event;
    }

    public String getUserId() {
      return content.from;
    }

    public String getServiceId() {
      return serviceId;
    }

    public boolean isVerifyRequest() {
      return "uffffffffffffffffffffffffffffffff".equals(content.from);
    }

    public boolean isImage() {
      return content.contentType == 2;
    }

    public String getId() {
      return content.id;
    }

    public ContentType getContentType() {
      return ContentType.get(content.contentType);
    }

    public Location getLocation() {
      return content.location;
    }

    @Override
    public Object getLoggableData() {
      return this;
    }
  }

  public static class Content {

    @JsonProperty(value = "toType")
    private Integer toType;

    @JsonProperty(value = "createdTime")
    private Long createdTime;

    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "from")
    private String from;

    @JsonProperty(value = "to")
    private String[] to;

    @JsonProperty(value = "text")
    private String text;

    @JsonProperty(value = "contentType")
    private Integer contentType;

    @JsonProperty(value = "location")
    Location location;

    // TODO: localtion, contentMetadata, deliveredTime,...

  }

  public static class Location {

    @JsonProperty(value = "title")
    String title;

    @JsonProperty(value = "latitude")
    public Double latitude;

    @JsonProperty(value = "longitude")
    public Double longitude;

  }


}
