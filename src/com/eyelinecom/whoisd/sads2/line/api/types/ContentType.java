package com.eyelinecom.whoisd.sads2.line.api.types;

import java.util.HashMap;
import java.util.Map;

public enum ContentType {
  TEXT(1),
  IMAGE(2),
  VIDEO(3),
  AUDIO(4),
  LOCATION(7),
  STICKER(8),
  CONTACT(10),
  UNKOWN(-1)
  ;
  public final int id;

  private ContentType(int id) {
    this.id = id;
    MapHolder.map.put(id, this);
  }

  public static ContentType get(int id) {
    ContentType contentType = MapHolder.map.get(id);
    if (contentType != null) return contentType;
    return UNKOWN;
  }

  private static class MapHolder {
    public static final Map<Integer, ContentType> map = new HashMap<>();
  }

}
