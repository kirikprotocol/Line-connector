package com.eyelinecom.whoisd.sads2.line.interceptors;

import com.eyelinecom.whoisd.sads2.RequestDispatcher;
import com.eyelinecom.whoisd.sads2.common.HttpDataLoader;
import com.eyelinecom.whoisd.sads2.common.Initable;
import com.eyelinecom.whoisd.sads2.common.PageBuilder;
import com.eyelinecom.whoisd.sads2.common.SADSInitUtils;
import com.eyelinecom.whoisd.sads2.common.SADSLogger;
import com.eyelinecom.whoisd.sads2.common.UrlUtils;
import com.eyelinecom.whoisd.sads2.connector.SADSRequest;
import com.eyelinecom.whoisd.sads2.connector.SADSResponse;
import com.eyelinecom.whoisd.sads2.content.ContentResponse;
import com.eyelinecom.whoisd.sads2.content.attachments.Attachment;
import com.eyelinecom.whoisd.sads2.exception.InterceptionException;
import com.eyelinecom.whoisd.sads2.interceptor.BlankInterceptor;
import com.eyelinecom.whoisd.sads2.line.registry.LineServiceRegistry;
import com.eyelinecom.whoisd.sads2.line.registry.LineToken;
import com.eyelinecom.whoisd.sads2.line.resource.LineApi;
import com.eyelinecom.whoisd.sads2.session.ServiceSessionManager;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.apache.commons.logging.Log;
import org.dom4j.Document;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.eyelinecom.whoisd.sads2.content.attachments.Attachment.Type.fromString;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class LineAttachmentPushInterceptor extends BlankInterceptor implements Initable {

  private static final org.apache.log4j.Logger globalLog = org.apache.log4j.Logger.getLogger(LineAttachmentPushInterceptor.class);

  private LineApi client;
  private ServiceSessionManager sessionManager;
  private HttpDataLoader loader;


  public void afterResponseRender(SADSRequest request,
                                  ContentResponse content,
                                  SADSResponse response,
                                  RequestDispatcher dispatcher) throws InterceptionException {
    try {
      if (isNotBlank(request.getParameters().get("sadsSmsMessage"))) {
        sendAttachment(request, content, response);
      } else {
        // ?
        sendAttachment(request, content, response);
      }
    } catch (Exception e) {
      throw new InterceptionException(e);
    }
  }

  private void sendAttachment(SADSRequest request, ContentResponse content, SADSResponse response) {
    final String serviceId = request.getServiceId();
    final Document doc = (Document) response.getAttributes().get(PageBuilder.VALUE_DOCUMENT);
    Log log = SADSLogger.getLogger(request, this.getClass());

    final Collection<Attachment> attachments = Attachment.extract(globalLog, doc);
    if (attachments.isEmpty()) return;

    LineToken token = LineServiceRegistry.getToken(request.getServiceScenario().getAttributes());
    String userId = request.getProfile().property("line", "id").getValue();

    // final String url = UrlUtils.merge(resourceBaseUrl, getSrc());
    try {
      for (Attachment attachment : attachments) {
        Attachment.Type type = fromString(attachment.getType());
        String url = attachment.getSrc() != null ? UrlUtils.merge(request.getResourceURI(), attachment.getSrc()) : null;
        switch (type) {
          case PHOTO:
            client.sendPhoto(url, userId, token);
            break;
          case VIDEO:
            client.sendVideo(url, userId, token);
            break;
          case AUDIO:
            int duration = attachment.getDuration() == null ? 0 : attachment.getDuration();
            client.sendAudio(url, duration, userId, token);
            break;
          case DOCUMENT:
            break;
          case LOCATION:
            try {
              double latitude = Double.parseDouble(attachment.getLatitude());
              double longitude = Double.parseDouble(attachment.getLongitude());
              client.sendLocation(attachment.getCaption(), latitude, longitude, userId, token);
            } catch (NumberFormatException e) {
              log.debug("Failed to parse location", e);
            }
            break;
        }

      }
    } catch (Throwable e) {
      log.error("", e);
    }

  }


  @Override
  public void init(Properties config) throws Exception {
    client = (LineApi) SADSInitUtils.getResource("client", config);
    sessionManager = (ServiceSessionManager) SADSInitUtils.getResource("session-manager", config);
    loader = SADSInitUtils.getResource("loader", config);
  }

  @Override
  public void destroy() {
  }
}
