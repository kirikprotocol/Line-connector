package com.eyelinecom.whoisd.sads2.line.registry;

import com.eyelinecom.whoisd.sads2.common.SADSInitUtils;
import com.eyelinecom.whoisd.sads2.exception.ConfigurationException;
import com.eyelinecom.whoisd.sads2.line.resource.LineApi;
import com.eyelinecom.whoisd.sads2.registry.Config;
import com.eyelinecom.whoisd.sads2.registry.ServiceConfig;
import com.eyelinecom.whoisd.sads2.registry.ServiceConfigListener;
import com.eyelinecom.whoisd.sads2.resource.ResourceFactory;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class LineServiceRegistry extends ServiceConfigListener {

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LineServiceRegistry.class);

  public static final String CONF_TOKEN = "line.token";

  private final Map<String, ServiceEntry> serviceMap = new ConcurrentHashMap<>();
  private final LineApi api;

  public LineServiceRegistry(LineApi api) {
    this.api = api;
  }

  @Override
  protected void process(Config config) throws ConfigurationException {
    final String serviceId = config.getId();
    if (config.isEmpty()) {
      unregister(serviceId);
    } else if (config instanceof ServiceConfig) {
      final ServiceConfig serviceConfig = (ServiceConfig) config;
      LineToken token = getToken(serviceConfig.getAttributes());
      if (token == null) {
        unregister(serviceId);
      } else {
        register(serviceId, token);
      }
    }
  }

  private void register(String serviceId, LineToken token) {
    ServiceEntry serviceEntry = serviceMap.get(serviceId);
    if (serviceEntry != null && token.equals(serviceEntry.token)) {
      log.debug("Already registered service " + serviceId + " already registered in line api, token: " + token + "...");
      return;
    }
    String url = api.connectorUrl() + "/" + serviceId;
    log.debug("registered for service" + serviceId + " token " + token);
    serviceMap.put(serviceId, new ServiceEntry(serviceId, token));
  }

  private void unregister(String serviceId) {
    log.debug("unregistering " + serviceId);
    serviceMap.remove(serviceId);
  }

  public static LineToken getToken(Properties properties) {
    return LineToken.get(properties.getProperty(CONF_TOKEN));
  }

  public LineToken getToken(String serviceId) {
    ServiceEntry entry = serviceMap.get(serviceId);
    if (entry == null) return null;
    return entry.token;
  }

  public static class Factory implements ResourceFactory {

    @Override
    public Object build(String id, Properties properties, HierarchicalConfiguration config) throws Exception {
      LineApi api = SADSInitUtils.getResource("line-api", properties);
      return new LineServiceRegistry(api);
    }

    @Override
    public boolean isHeavyResource() {
      return false;
    }
  }

  private static class ServiceEntry {

    private final String serviceId;
    private final LineToken token;

    public ServiceEntry(String serviceId, LineToken token) {
      this.serviceId = serviceId;
      this.token = token;
    }
  }
}
