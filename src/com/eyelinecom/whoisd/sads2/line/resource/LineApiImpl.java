package com.eyelinecom.whoisd.sads2.line.resource;

import com.eyelinecom.whoisd.sads2.common.HttpDataLoader;
import com.eyelinecom.whoisd.sads2.common.HttpLoader;
import com.eyelinecom.whoisd.sads2.common.Loader;
import com.eyelinecom.whoisd.sads2.common.SADSInitUtils;
import com.eyelinecom.whoisd.sads2.executors.connector.SADSInitializer;
import com.eyelinecom.whoisd.sads2.line.api.types.LineContacts;
import com.eyelinecom.whoisd.sads2.line.api.types.LineRequest;
import com.eyelinecom.whoisd.sads2.line.registry.LineToken;
import com.eyelinecom.whoisd.sads2.line.util.MarshalUtils;
import com.eyelinecom.whoisd.sads2.resource.ResourceFactory;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LineApiImpl implements LineApi {

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LineApiImpl.class);

  private final HttpDataLoader loader;
  private final String connectorUrl;
  public static final String VIDEO_PREVIEW_IMAGE = "videoPreviewImage.jpg";

  public LineApiImpl(HttpDataLoader loader, Properties properties) {
    this.loader = loader;
    this.connectorUrl = properties.getProperty("connector.url");
  }

  @Override
  public String connectorUrl() {
    return connectorUrl;
  }

  @Override
  public void send(String text, String userId, LineToken token) {
    sendEvent(LineRequest.text(userId, text), token);
  }

  private void sendEvent(LineRequest request, LineToken token) {
    try {
      String json = request.toJson();
      log.debug("Line send " + request.type() + " request: " + json);
      Loader.Entity entity = loader.load("https://trialbot-api.line.me/v1/events", json, "application/json", "UTF-8", createHeaders(token), HttpLoader.METHOD_POST);
      log.debug("Line received " + request.type() + " response: " + new String(entity.getBuffer()));
    } catch (Exception e) {
      log.error("Line request failed", e);
    }
  }

  @Override
  public void sendPhoto(String url, String userId, LineToken token) {
    sendEvent(LineRequest.image(userId, url), token);
  }

  @Override
  public void sendVideo(String url, String userId, LineToken token) {
    sendEvent(LineRequest.video(userId, url, videoPreviewImageUrl()), token);
  }

  private String videoPreviewImageUrl() {
    // This is just black square, check LineMessageConnector.getVideoPreviewImageData() for generation
    return getRootUri() + "/line/" + VIDEO_PREVIEW_IMAGE;
  }

  protected String getRootUri() {
    try {
      final Properties mainProperties = SADSInitializer.getMainProperties();
      return mainProperties.getProperty("root.uri");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendAudio(String url, int duration, String userId, LineToken token) {
    sendEvent(LineRequest.audio(userId, url, duration), token);
  }

  @Override
  public void sendLocation(String title, double latitude, double longitude, String userId, LineToken token) {
    if (title == null || title.isEmpty()) title = "Location";
    sendEvent(LineRequest.location(userId, title, latitude, longitude), token);
  }

  @Override
  public byte[] getContent(LineToken token, String fileId) {
    try {
      log.debug("Sending file request, id: " + fileId);
      Loader.Entity entity = loader.load("https://trialbot-api.line.me/v1/bot/message/" + fileId + "/content", createHeaders(token));
      return entity.getBuffer();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public LineContacts getProfile(String mid, LineToken token) {
    try {
      Loader.Entity entity = loader.load("https://trialbot-api.line.me/v1/profiles?mids=" + mid, createHeaders(token));
      LineContacts contacts = MarshalUtils.unmarshal(new String(entity.getBuffer()), LineContacts.class);
      if (contacts.isError()) throw new Exception(contacts.getStatusMessage());
      return contacts;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, String> createHeaders(LineToken token) {
    HashMap<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("X-Line-ChannelID", token.channelId());
    headers.put("X-Line-ChannelSecret", token.channelSecret());
    headers.put("X-Line-Trusted-User-With-ACL", token.mid());
    return headers;
  }

  public static class Factory implements ResourceFactory {

    @Override
    public Object build(String id, Properties properties, HierarchicalConfiguration config) throws Exception {
      final HttpDataLoader loader = SADSInitUtils.getResource("loader", properties);
      return new LineApiImpl(loader, properties);
    }

    @Override
    public boolean isHeavyResource() {
      return false;
    }
  }

}
