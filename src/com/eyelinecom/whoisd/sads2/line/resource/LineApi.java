package com.eyelinecom.whoisd.sads2.line.resource;

import com.eyelinecom.whoisd.sads2.line.api.types.LineContacts;
import com.eyelinecom.whoisd.sads2.line.registry.LineToken;

public interface LineApi {
  String connectorUrl();

  void send(String text, String userId, LineToken token);

  void sendPhoto(String url, String userId, LineToken token);

  void sendVideo(String url, String userId, LineToken token);

  void sendAudio(String url, int duration, String userId, LineToken token);

  void sendLocation(String title, double latitude, double longitude, String userId, LineToken token);

  byte[] getContent(LineToken lineToken, String fileId);

  LineContacts getProfile(String mid, LineToken token);
}
