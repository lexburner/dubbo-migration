package com.alibaba.edas.dubbo.migration;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdasMultipleRegistry extends AbstractDynamicMultipleRegistry {
    private static final Logger logger = LoggerFactory.getLogger(EdasMultipleRegistry.class);
    private static final String CONFIG_ADDRESS_KEY = "config-address";
    private static final String REGISTRY_CONFIGURATION_DATA_ID = "dubbo.registry.config";
    private static final String REGISTRY_DEFAULT_GROUP = "_GLOBAL_REGISTRY_";
    private static final String DUBBO_SERVICE_REGISTER_KEY = "dubbo.service.registry";
    private static final String DUBBO_SERVICE_SUBSCRIBER_KEY = "dubbo.reference.registry";
    private static final String STATIC_REGISTRY_MAP_ORIG_SERVICE = "dubbo.orig.service.registry";
    private static final String STATIC_REGISTRY_MAP_ORIG_REFERENCE = "dubbo.orig.reference.registry";
    private static final String STATIC_REGISTRY_MAP_SERVICE = "dubbo.effective.service.registry";
    private static final String STATIC_REGISTRY_MAP_REFERENCE = "dubbo.effective.reference.registry";
    public static final Map<String, List<String>> staticRegistryInfos = new HashMap();
    private ConfigService configService;
    private Properties properties;
    private String localHost;

    public EdasMultipleRegistry(URL url) {
        super(url);

        try {
            this.fillRegistryInfo();
            this.addListenerDynamicConfig();
        } catch (NacosException var4) {
            String errorMsg = "Error happened when adding listener for nacos of url: " + super.getUrl();
            logger.error(errorMsg, var4);
            throw new RuntimeException(errorMsg, var4);
        }
    }

    @Override
    protected void init() {
        String errorMsg;
        try {
            this.localHost = NetUtils.getLocalHost();
            this.configService = this.buildConfigServer(super.getUrl());
            this.fetchDynamicConfig();
            logger.info("invoke init to constructor configService and dynamic fetch data for ip: " + this.localHost);
        } catch (NacosException var3) {
            errorMsg = "Can't create nacos server for url: " + super.getUrl();
            logger.error(errorMsg, var3);
            throw new RuntimeException(errorMsg, var3);
        } catch (IOException var4) {
            errorMsg = "Can't create config server for url: " + super.getUrl();
            logger.error(errorMsg, var4);
            throw new RuntimeException(errorMsg, var4);
        }
    }

    @Override
    protected List<String> filterServiceRegistry(List<String> serviceRegistryURLs) {
        String[] strArray;
        if (this.properties != null && !StringUtils.isEmpty(this.properties.getProperty(this.localHost + "." + "dubbo.service.registry"))) {
            logger.info("get service registry info for ip: " + this.localHost + " , value: " + this.properties.getProperty(this.localHost + "." + "dubbo.service.registry"));
            strArray = Constants.COMMA_SPLIT_PATTERN.split(this.properties.getProperty(this.localHost + "." + "dubbo.service.registry"));
            return Arrays.asList(strArray);
        } else if (this.properties != null && !StringUtils.isEmpty(this.properties.getProperty("dubbo.service.registry"))) {
            logger.info("get service registry info. value: " + this.properties.getProperty("dubbo.service.registry"));
            strArray = Constants.COMMA_SPLIT_PATTERN.split(this.properties.getProperty("dubbo.service.registry"));
            return Arrays.asList(strArray);
        } else {
            return serviceRegistryURLs;
        }
    }

    @Override
    protected List<String> filterReferenceRegistry(List<String> referenceRegistryURLs) {
        String[] strArray;
        if (this.properties != null && !StringUtils.isEmpty(this.properties.getProperty(this.localHost + "." + "dubbo.reference.registry"))) {
            logger.info("get reference registry info for ip: " + this.localHost + " , value: " + this.properties.getProperty(this.localHost + "." + "dubbo.reference.registry"));
            strArray = Constants.COMMA_SPLIT_PATTERN.split(this.properties.getProperty(this.localHost + "." + "dubbo.reference.registry"));
            return Arrays.asList(strArray);
        } else if (this.properties != null && !StringUtils.isEmpty(this.properties.getProperty("dubbo.reference.registry"))) {
            logger.info("get reference registry info. value: " + this.properties.getProperty("dubbo.reference.registry"));
            strArray = Constants.COMMA_SPLIT_PATTERN.split(this.properties.getProperty("dubbo.reference.registry"));
            return Arrays.asList(strArray);
        } else {
            return referenceRegistryURLs;
        }
    }

    private ConfigService buildConfigServer(URL url) throws NacosException {
        String serverAddr = url.getParameter("config-address");
        if (serverAddr == null) {
            return null;
        } else {
            Properties properties = new Properties();
            properties.put("serverAddr", serverAddr);
            return NacosFactory.createConfigService(properties);
        }
    }

    private void fetchDynamicConfig() throws NacosException, IOException {
        Properties p = new Properties();
        if (this.configService == null) {
            this.properties = p;
        } else {
            String value = this.configService.getConfig("dubbo.registry.config", super.getApplicationName(), 3000L);
            if (StringUtils.isEmpty(value)) {
                value = this.configService.getConfig("dubbo.registry.config", "_GLOBAL_REGISTRY_", 3000L);
            }

            if (!StringUtils.isEmpty(value)) {
                p.load(new StringReader(value));
                this.properties = p;
            }
        }
    }

    private void addListenerDynamicConfig() throws NacosException {
        if (this.configService != null) {
            Listener listener = new EdasMultipleRegistry.RegistryDynamicDataListener();
            this.configService.addListener("dubbo.registry.config", super.getApplicationName(), listener);
            this.configService.addListener("dubbo.registry.config", "_GLOBAL_REGISTRY_", listener);
        }
    }

    private void fillRegistryInfo() {
        staticRegistryInfos.put("dubbo.orig.service.registry", this.getOrigServiceRegistryURLs());
        staticRegistryInfos.put("dubbo.orig.reference.registry", this.getOrigReferenceRegistryURLs());
        staticRegistryInfos.put("dubbo.effective.service.registry", this.getEffectServiceRegistryURLs());
        staticRegistryInfos.put("dubbo.effective.reference.registry", this.getEffectReferenceRegistryURLs());
    }

    class RegistryDynamicDataListener extends AbstractListener {
        RegistryDynamicDataListener() {
        }

        @Override
        public void receiveConfigInfo(String configInfo) {
            try {
                if (EdasMultipleRegistry.logger.isDebugEnabled()) {
                    EdasMultipleRegistry.logger.debug("Config data was updated." + configInfo);
                }

                EdasMultipleRegistry.this.fetchDynamicConfig();
                if (!CollectionUtils.isEmpty(EdasMultipleRegistry.this.getEffectServiceRegistryURLs())) {
                    EdasMultipleRegistry.this.refreshServiceRegistry(EdasMultipleRegistry.this.filterServiceRegistry(EdasMultipleRegistry.this.getEffectServiceRegistryURLs()));
                }

                if (!CollectionUtils.isEmpty(EdasMultipleRegistry.this.getEffectReferenceRegistryURLs())) {
                    EdasMultipleRegistry.this.refreshReferenceRegistry(EdasMultipleRegistry.this.filterReferenceRegistry(EdasMultipleRegistry.this.getEffectReferenceRegistryURLs()));
                }

                EdasMultipleRegistry.this.fillRegistryInfo();
            } catch (NacosException var3) {
                EdasMultipleRegistry.logger.error("error happened when getting dynamic configuration from nacos. ", var3);
            } catch (IOException var4) {
                EdasMultipleRegistry.logger.error("error happened when getting dynamic configuration from nacos. ", var4);
            }

        }
    }
}