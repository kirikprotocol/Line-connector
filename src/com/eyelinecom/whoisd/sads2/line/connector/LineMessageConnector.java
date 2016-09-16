package com.eyelinecom.whoisd.sads2.line.connector;

import com.eyelinecom.whoisd.sads2.Protocol;
import com.eyelinecom.whoisd.sads2.common.InitUtils;
import com.eyelinecom.whoisd.sads2.common.SADSUrlUtils;
import com.eyelinecom.whoisd.sads2.common.UrlUtils;
import com.eyelinecom.whoisd.sads2.connector.SADSRequest;
import com.eyelinecom.whoisd.sads2.connector.SADSResponse;
import com.eyelinecom.whoisd.sads2.connector.Session;
import com.eyelinecom.whoisd.sads2.events.Event;
import com.eyelinecom.whoisd.sads2.events.LinkEvent;
import com.eyelinecom.whoisd.sads2.events.MessageEvent;
import com.eyelinecom.whoisd.sads2.executors.connector.AbstractHTTPPushConnector;
import com.eyelinecom.whoisd.sads2.executors.connector.ProfileEnabledMessageConnector;
import com.eyelinecom.whoisd.sads2.executors.connector.SADSExecutor;
import com.eyelinecom.whoisd.sads2.input.AbstractInputType;
import com.eyelinecom.whoisd.sads2.input.InputFile;
import com.eyelinecom.whoisd.sads2.input.InputLocation;
import com.eyelinecom.whoisd.sads2.line.api.types.LineCallback;
import com.eyelinecom.whoisd.sads2.line.resource.LineApiImpl;
import com.eyelinecom.whoisd.sads2.line.util.MarshalUtils;
import com.eyelinecom.whoisd.sads2.profile.Profile;
import com.eyelinecom.whoisd.sads2.registry.ServiceConfig;
import com.eyelinecom.whoisd.sads2.session.ServiceSessionManager;
import com.eyelinecom.whoisd.sads2.session.SessionManager;
import com.eyelinecom.whoisd.sads2.utils.ConnectorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Log4JLogger;
import org.dom4j.Document;
import org.dom4j.Element;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.eyelinecom.whoisd.sads2.Protocol.LINE;
import static com.eyelinecom.whoisd.sads2.wstorage.profile.QueryRestrictions.property;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

public class LineMessageConnector extends HttpServlet {

  private final static Log log = new Log4JLogger(org.apache.log4j.Logger.getLogger(LineMessageConnector.class));

  private LineMessageConnectorImpl connector;
  private volatile byte[] videoPreviewImageData;

  @Override
  public void destroy() {
    super.destroy();
    connector.destroy();
  }

  @Override
  public void init(ServletConfig servletConfig) throws ServletException {
    connector = new LineMessageConnectorImpl();

    try {
      final Properties properties = AbstractHTTPPushConnector.buildProperties(servletConfig);
      connector.init(properties);

    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void service(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

    if (req.getRequestURI().endsWith(LineApiImpl.VIDEO_PREVIEW_IMAGE)) {
      resp.setStatus(200);
      resp.setContentType("image/jpeg");
      resp.getOutputStream().write(getVideoPreviewImageData());
      return;
    }
    final LineCallbackMultipleRequest request = new LineCallbackMultipleRequest(req);
    for (LineCallback.Request result : request.getCallback().getRequest()) {
      if (result.isVerifyRequest()) continue;
      connector.process(result);
    }
    final SADSResponse response = connector.buildCallbackResponse(200, "");
    ConnectorUtils.fillHttpResponse(resp, response);
  }

  private byte[] getVideoPreviewImageData() {
    if (videoPreviewImageData == null) {
      BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      try {
        ImageIO.write(image, "jpg", bout);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      videoPreviewImageData = bout.toByteArray();
    }
    return videoPreviewImageData;
  }

  private class LineMessageConnectorImpl
    extends ProfileEnabledMessageConnector<LineCallback.Request> {

    @Override
    protected SADSResponse buildQueuedResponse(LineCallback.Request request, SADSRequest sadsRequest) {
      return buildCallbackResponse(200, "");
    }

    @Override
    protected SADSResponse buildQueueErrorResponse(Exception e, LineCallback.Request request, SADSRequest sadsRequest) {
      return buildCallbackResponse(500, "");
    }

    @Override
    protected Log getLogger() {
      return log;
    }

    @Override
    protected String getSubscriberId(LineCallback.Request req) throws Exception {

      if (req.getProfile() != null) {
        return req.getProfile().getWnumber();
      }
      String userId = String.valueOf(req.getUserId());
      Profile profile = getProfileStorage()
        .query()
        .where(property("line", "id").eq(userId))
        .getOrCreate();

      req.setProfile(profile);
      return profile.getWnumber();
    }

    @Override
    protected String getServiceId(LineCallback.Request req) throws Exception {
      return req.getServiceId();
    }

    @Override
    protected String getGateway() {
      return "Line";
    }

    @Override
    protected String getGatewayRequestDescription(LineCallback.Request lineCallbackRequest) {
      return "Line";
    }

    @Override
    protected Protocol getRequestProtocol(ServiceConfig config, String subscriberId, LineCallback.Request request) {
      return LINE;
    }

    @Override
    protected String getRequestUri(ServiceConfig config, String wnumber, LineCallback.Request message) throws Exception {

      final String serviceId = config.getId();
      String incoming = message.getText();

      Session session = getSessionManager(serviceId).getSession(wnumber);
      final String prevUri = (String) session.getAttribute(ATTR_SESSION_PREVIOUS_PAGE_URI);
      if (prevUri == null) {
        // No previous page means this is an initial request, thus serve the start page.
        message.setEvent(new MessageEvent.TextMessageEvent(incoming));
        return super.getRequestUri(config, wnumber, message);
      } else {
        final Document prevPage =
          (Document) session.getAttribute(SADSExecutor.ATTR_SESSION_PREVIOUS_PAGE);

        String href = null;
        String inputName = null;

        // Look for a button with a corresponding label.
        //noinspection unchecked
        for (Element e : (List<Element>) prevPage.getRootElement().elements("button")) {
          final String btnLabel = e.getTextTrim();
          final String btnIndex = e.attributeValue("index");

          if (equalsIgnoreCase(btnLabel, incoming) || equalsIgnoreCase(btnIndex, incoming)) {
            final String btnHref = e.attributeValue("href");
            href = btnHref != null ? btnHref : e.attributeValue("target");

            message.setEvent(new LinkEvent(btnLabel, prevUri));
          }
        }

        // Look for input field if any.
        if (href == null) {
          final Element input = prevPage.getRootElement().element("input");
          if (input != null) {
            href = input.attributeValue("href");
            inputName = input.attributeValue("name");
          }
        }

        // Nothing suitable to handle user input found, consider it a bad command.
        if (href == null) {
          final String badCommandPage =
            InitUtils.getString("bad-command-page", "", config.getAttributes());
          href = UrlUtils.merge(prevUri, badCommandPage);
          href = UrlUtils.addParameter(href, "bad_command", incoming);
        }

        if (message.getEvent() == null) {
          message.setEvent(new MessageEvent.TextMessageEvent(incoming));
        }

        href = SADSUrlUtils.processUssdForm(href, StringUtils.trim(incoming));
        if (inputName != null) {
          href = UrlUtils.addParameter(href, inputName, incoming);
        }

        return UrlUtils.merge(prevUri, href);
      }
    }

    @Override
    protected SADSResponse getOuterResponse(LineCallback.Request result, SADSRequest request, SADSResponse response) {
      return buildCallbackResponse(200, "");
    }

    private SessionManager getSessionManager(String serviceId) throws Exception {
      final ServiceSessionManager serviceSessionManager = getResource("session-manager");
      return serviceSessionManager.getSessionManager(LINE, serviceId);
    }

    @Override
    protected Profile getCachedProfile(LineCallback.Request req) {
      return req.getProfile();
    }

    @Override
    protected Event getEvent(LineCallback.Request req) {
      return req.getEvent();
    }

    @Override
    protected void fillSADSRequest(SADSRequest sadsRequest, LineCallback.Request request) {
      super.fillSADSRequest(sadsRequest, request);
      try {
        handleFileUpload(sadsRequest, request);
      } catch (Exception e) {
        getLog(request).error(e.getMessage(), e);
      }

      super.fillSADSRequest(sadsRequest, request);
    }

    private void handleFileUpload(SADSRequest sadsRequest, LineCallback.Request req) throws Exception {
      final List<? extends AbstractInputType> mediaList = extractMedia(sadsRequest, req);
      if (isEmpty(mediaList)) return;

      req.setEvent(mediaList.iterator().next().asEvent());

      Session session = sadsRequest.getSession();
      Document prevPage = (Document) session.getAttribute(SADSExecutor.ATTR_SESSION_PREVIOUS_PAGE);
      Element input = prevPage == null ? null : prevPage.getRootElement().element("input");
      String inputName = input != null ? input.attributeValue("name") : "bad_command";

      final String mediaParameter = MarshalUtils.marshal(mediaList);
      sadsRequest.getParameters().put(inputName, mediaParameter);
      sadsRequest.getParameters().put("input_type", "json");
    }

    private List<? extends AbstractInputType> extractMedia(SADSRequest sadsRequest, LineCallback.Request req) {
      final String serviceId = sadsRequest.getServiceId();
      final List<AbstractInputType> mediaList = new ArrayList<>();

      switch (req.getContentType()) {
        case IMAGE: {
          final InputFile file = new InputFile();
          file.setMediaType("photo");
          file.setUrl(getFilePath(serviceId, req.getId()));
          mediaList.add(file);
        }
        break;
        case VIDEO: {
          final InputFile file = new InputFile();
          file.setMediaType("video");
          file.setUrl(getFilePath(serviceId, req.getId()));
          mediaList.add(file);
        }
        case AUDIO: {
          final InputFile file = new InputFile();
          file.setMediaType("audio");
          file.setUrl(getFilePath(serviceId, req.getId()));
          mediaList.add(file);
        }
        case LOCATION: {
          final InputLocation location = new InputLocation();
          location.setLatitude(req.getLocation().latitude);
          location.setLongitude(req.getLocation().longitude);
          mediaList.add(location);
        }
        break;
      }

      return mediaList;
    }

    private String getFilePath(String serviceId, String fileId) {
      return getRootUri() + "/files/" + serviceId + "/line/" + fileId;
    }

    private SADSResponse buildCallbackResponse(int statusCode, String body) {
      final SADSResponse rc = new SADSResponse();
      rc.setStatus(statusCode);
      rc.setHeaders(Collections.<String, String>emptyMap());
      rc.setMimeType("text/plain");
      rc.setData(body.getBytes());
      return rc;
    }

  }

}
