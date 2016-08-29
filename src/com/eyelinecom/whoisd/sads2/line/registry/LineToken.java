package com.eyelinecom.whoisd.sads2.line.registry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineToken {
  private static final Pattern PATTERN = Pattern.compile("(\\d+):([a-fA-F0-9]+):([a-zA-Z0-9]+):?([@0-9a-zA-Z]+)?");
  private final String channelId;
  private final String channelSecret;
  private final String mid;
  private final String botId;

  private LineToken(String channelId, String channelSecret, String mid, String botId) {
    this.channelId = channelId;
    this.channelSecret = channelSecret;
    this.mid = mid;
    this.botId = botId;
  }

  public static LineToken get(String tokenString) {
    // https://line.me/R/ti/p/%40ubx7666l
    // 1476260241:a29868c57319c5a55cabadb2a4476535:ub6a864b06b752b403a031580d2e56b2b:@ubx7666l
    if (tokenString == null || tokenString.isEmpty()) return null;
    Matcher m = PATTERN.matcher(tokenString);
    if (!m.matches()) return null;
    return new LineToken(m.group(1), m.group(2), m.group(3), m.group(4));
  }

  public String channelId() {
    return channelId;
  }

  public String channelSecret() {
    return channelSecret;
  }

  public String mid() {
    return mid;
  }

  public String botId() {
    return botId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LineToken token = (LineToken) o;
    return !(botId != null ? !botId.equals(token.botId) : token.botId != null) &&
      channelId.equals(token.channelId) &&
      channelSecret.equals(token.channelSecret) && mid.
      equals(token.mid);
  }

  @Override
  public int hashCode() {
    int result = channelId.hashCode();
    result = 31 * result + channelSecret.hashCode();
    result = 31 * result + mid.hashCode();
    result = 31 * result + (botId != null ? botId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "LineToken{" +
      "botId='" + botId + '\'' +
      ", channelId='" + channelId + '\'' +
      ", channelSecret='" + channelSecret + '\'' +
      ", mid='" + mid + '\'' +
      '}';
  }
}
