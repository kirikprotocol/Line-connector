package com.eyelinecom.whoisd.sads2.line.connector;

import com.eyelinecom.whoisd.sads2.common.StoredHttpRequest;
import com.eyelinecom.whoisd.sads2.line.api.types.LineCallback;
import com.eyelinecom.whoisd.sads2.line.util.MarshalUtils;
import com.eyelinecom.whoisd.sads2.profile.Profile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class LineCallbackMultipleRequest extends StoredHttpRequest {

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LineCallbackMultipleRequest.class);

  private final LineCallback callback;

  public LineCallbackMultipleRequest(HttpServletRequest request) throws IOException {
    super(request);
    final String[] parts = getRequestURI().split("/");
    String serviceId = parts[parts.length - 1];
    String content = getContent();
    log.debug("Line content: " + content);
    callback = MarshalUtils.unmarshal(content, LineCallback.class);
    callback.setServiceId(serviceId);
  }
  public LineCallback getCallback() {
    return callback;
  }
}
