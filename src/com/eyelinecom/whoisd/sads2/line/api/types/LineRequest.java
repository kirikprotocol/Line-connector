package com.eyelinecom.whoisd.sads2.line.api.types;

import com.eyelinecom.whoisd.sads2.line.util.MarshalUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class LineRequest {

  @JsonProperty(value = "to")
  String[] to;

  @JsonProperty(value = "toChannel")
  Integer toChannel;

  @JsonProperty(value = "eventType")
  String eventType;

  @JsonProperty(value = "content")
  Content content;

  private static LineRequest createRequest(String userId, ContentType contentType) {
    LineRequest request = new LineRequest();
    request.to = new String[]{userId};
    request.toChannel = 1383378250;
    request.eventType = "138311608800106203";
    request.content = new Content();
    request.content.toType = 1;
    request.content.contentType = contentType.id;
    return request;
  }

  public static LineRequest text(String userId, String text) {
    LineRequest request = createRequest(userId, ContentType.TEXT);
    request.content.text = text;
    return request;
  }

  public static LineRequest image(String userId, String url) {
    LineRequest request = createRequest(userId, ContentType.IMAGE);
    // TODO: should we check resolution?
    request.content.originalContentUrl = url;
    // TODO: check resolution and make resized version for preview if nessessary
    request.content.previewImageUrl = url;
    return request;
  }

  public static LineRequest location(String userId, String title, double latitude, double longitude) {
    LineRequest request = createRequest(userId, ContentType.LOCATION);
    request.content.text = title == null ? "" : title; // TODO: empty location are not allowed, what to do?
    request.content.location = new Location();
    request.content.location.title = title == null ? "" : title;
    request.content.location.latitude = latitude;
    request.content.location.longitude = longitude;
    return request;
  }

  public static LineRequest video(String userId, String url, String previewImageUrl) {
    LineRequest request = createRequest(userId, ContentType.VIDEO);
    // TODO: should we check resolution?
    request.content.originalContentUrl = url;
    // TODO: check resolution, also implement generation preview from video later
    request.content.previewImageUrl = previewImageUrl;
    return request;
  }


  public String toJson() throws JsonProcessingException {
    return MarshalUtils.marshal(this);
  }

  public static LineRequest audio(String userId, String url, int duration) {
    LineRequest request = createRequest(userId, ContentType.AUDIO);
    request.content.originalContentUrl = url;
    // TODO: if duration is undefined (==0 or implement differently) - calculate duration from audio
    request.content.contentMetadata = new ContentMetadata();
    request.content.contentMetadata.AUDLEN = String.valueOf(duration);
    return request;
  }

  public ContentType type() {
    if (content == null) return null;
    return ContentType.get(content.contentType);
  }

  public static class Content {

    @JsonProperty(value = "contentType")
    Integer contentType;

    @JsonProperty(value = "toType")
    Integer toType;

    // for text, "contentType":1
    @JsonProperty(value = "text")
    String text;

    // for image ("contentType":2), video ("contentType":3)
    @JsonProperty(value = "originalContentUrl")
    String originalContentUrl;

    @JsonProperty(value = "previewImageUrl")
    String previewImageUrl;

    // for location, "contentType":7
    @JsonProperty(value = "location")
    Location location;

    @JsonProperty(value = "contentMetadata")
    ContentMetadata contentMetadata;
  }

  public static class Location {

    @JsonProperty(value = "title")
    String title;

    @JsonProperty(value = "latitude")
    Double latitude;

    @JsonProperty(value = "longitude")
    Double longitude;

  }

  public static class ContentMetadata {
    @JsonProperty(value = "AUDLEN")
    String AUDLEN;
  }

}

/*

{"to": ["u8de4fae84e4f65fe7968510011d8c096"],
  "toChannel": 1383378250,
  "eventType": "138311608800106203",
  "content": {
    "contentType": 1,
    "toType": 1,
    "text": "Please choose option\n1 Idenify\n2 Help"
  }
}

*/