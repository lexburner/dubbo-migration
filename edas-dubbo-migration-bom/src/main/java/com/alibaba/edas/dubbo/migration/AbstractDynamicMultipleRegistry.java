package com.alibaba.edas.dubbo.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import com.alibaba.edas.dubbo.migration.MultipleRegistry.MultipleNotifyListenerWrapper;
import com.alibaba.edas.dubbo.migration.MultipleRegistry.SingleNotifyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractDynamicMultipleRegistry extends MultipleRegistry {
    private static final Logger logger = LoggerFactory.getLogger(AbstractDynamicMultipleRegistry.class);

    public AbstractDynamicMultipleRegistry(URL url) {
        super(url);
    }

    protected synchronized void refreshServiceRegistry(List<String> serviceRegistryURLs) {
        this.effectServiceRegistryURLs = serviceRegistryURLs;
        if (serviceRegistryURLs != null && !serviceRegistryURLs.isEmpty() && !this.getServiceRegistries().isEmpty()) {
            Set<URL> registerOrSubscriberData = this.getRegisteredProviderURLs();
            if (CollectionUtils.isEmpty(registerOrSubscriberData)) {
                logger.info("can't fetch registered URL for service.");
            } else {
                Map<String, Registry> newRegistryMap = new HashMap(4);
                Iterator var4 = serviceRegistryURLs.iterator();

                while(true) {
                    String serviceRegistryURL;
                    Registry removedRegistry;
                    Iterator var7;
                    URL url;
                    do {
                        if (!var4.hasNext()) {
                            List<Registry> removedRegistries = new ArrayList();
                            Iterator var10 = this.getServiceRegistries().entrySet().iterator();

                            while(var10.hasNext()) {
                                Entry<String, Registry> origRegistryEntry = (Entry)var10.next();
                                if (serviceRegistryURLs.contains(origRegistryEntry.getKey())) {
                                    newRegistryMap.put(origRegistryEntry.getKey(), origRegistryEntry.getValue());
                                } else {
                                    removedRegistries.add(origRegistryEntry.getValue());
                                }
                            }

                            var10 = removedRegistries.iterator();

                            while(var10.hasNext()) {
                                removedRegistry = (Registry)var10.next();
                                logger.info("Remove original registry for service:" + removedRegistry.getUrl());
                                var7 = registerOrSubscriberData.iterator();

                                while(var7.hasNext()) {
                                    url = (URL)var7.next();
                                    removedRegistry.unregister(url);
                                }
                            }

                            this.getServiceRegistries().clear();
                            this.getServiceRegistries().putAll(newRegistryMap);
                            return;
                        }

                        serviceRegistryURL = (String)var4.next();
                        removedRegistry = (Registry)this.getServiceRegistries().get(serviceRegistryURL);
                    } while(removedRegistry != null);

                    removedRegistry = this.registryFactory.getRegistry(URL.valueOf(serviceRegistryURL));
                    newRegistryMap.put(serviceRegistryURL, removedRegistry);
                    logger.info("add new registry for service:" + serviceRegistryURL);
                    var7 = registerOrSubscriberData.iterator();

                    while(var7.hasNext()) {
                        url = (URL)var7.next();
                        logger.info("Register new url " + url + " into reigstry:" + removedRegistry.getClass().getName() + " for provider");
                        removedRegistry.register(url);
                    }
                }
            }
        }
    }

    protected synchronized void refreshReferenceRegistry(List<String> referenceRegistryURLs) {
        this.effectReferenceRegistryURLs = referenceRegistryURLs;
        if (referenceRegistryURLs != null && !referenceRegistryURLs.isEmpty() && !this.getReferenceRegistries().isEmpty()) {
            Map<URL, Set<NotifyListener>> registerOrSubscriberData = this.getSubscribedURLMap();
            Set<URL> consumerURLs = this.getRegisteredConsumerURLs();
            if (registerOrSubscriberData == null) {
                logger.info("can't fetch registered URL for reference.");
            } else {
                Map<String, Registry> newRegistryMap = new HashMap<>(4);
                Iterator var5 = referenceRegistryURLs.iterator();

                while(true) {
                    String serviceRegistryURL;
                    Registry removedRegistry;
                    Iterator var8;
                    Entry urlNotifyListenerMap;
                    Iterator var10;
                    NotifyListener notifyListener;
                    URL url;
                    do {
                        if (!var5.hasNext()) {
                            List<Registry> removedRegistries = new ArrayList();
                            Iterator var14 = this.getReferenceRegistries().entrySet().iterator();

                            while(var14.hasNext()) {
                                urlNotifyListenerMap = (Entry)var14.next();
                                if (referenceRegistryURLs.contains(urlNotifyListenerMap.getKey())) {
                                    newRegistryMap.put((String)urlNotifyListenerMap.getKey(), (Registry)urlNotifyListenerMap.getValue());
                                } else {
                                    removedRegistries.add((Registry)urlNotifyListenerMap.getValue());
                                }
                            }

                            var14 = removedRegistries.iterator();

                            while(var14.hasNext()) {
                                removedRegistry = (Registry)var14.next();
                                logger.info("Remove original registry for reference:" + removedRegistry.getUrl());
                                var8 = consumerURLs.iterator();

                                while(var8.hasNext()) {
                                    url = (URL)var8.next();
                                    removedRegistry.unregister(url);
                                }

                                var8 = registerOrSubscriberData.entrySet().iterator();

                                while(var8.hasNext()) {
                                    urlNotifyListenerMap = (Entry)var8.next();
                                    var10 = ((Set)urlNotifyListenerMap.getValue()).iterator();

                                    while(var10.hasNext()) {
                                        notifyListener = (NotifyListener)var10.next();
                                        this.doRemoveMultipleSubscribed(notifyListener, removedRegistry);
                                        removedRegistry.unsubscribe((URL)urlNotifyListenerMap.getKey(), notifyListener);
                                    }
                                }
                            }

                            this.getReferenceRegistries().clear();
                            this.getReferenceRegistries().putAll(newRegistryMap);
                            var14 = registerOrSubscriberData.entrySet().iterator();

                            while(var14.hasNext()) {
                                urlNotifyListenerMap = (Entry)var14.next();
                                var8 = ((Set)urlNotifyListenerMap.getValue()).iterator();

                                while(var8.hasNext()) {
                                    NotifyListener notifyListener1 = (NotifyListener)var8.next();
                                    if (notifyListener1 instanceof MultipleNotifyListenerWrapper) {
                                        ((MultipleNotifyListenerWrapper)notifyListener1).notifySourceListener();
                                    }
                                }
                            }

                            return;
                        }

                        serviceRegistryURL = (String)var5.next();
                        removedRegistry = (Registry)this.getReferenceRegistries().get(serviceRegistryURL);
                    } while(removedRegistry != null);

                    removedRegistry = this.registryFactory.getRegistry(URL.valueOf(serviceRegistryURL));
                    newRegistryMap.put(serviceRegistryURL, removedRegistry);
                    logger.info("add new registry for reference:" + serviceRegistryURL);
                    var8 = registerOrSubscriberData.entrySet().iterator();

                    while(var8.hasNext()) {
                        urlNotifyListenerMap = (Entry)var8.next();
                        var10 = ((Set)urlNotifyListenerMap.getValue()).iterator();

                        while(var10.hasNext()) {
                            notifyListener = (NotifyListener)var10.next();
                            NotifyListener singleNotifyListener = this.fetchNotifyListenerAndAddMultipleSubscribed(notifyListener, removedRegistry);
                            removedRegistry.subscribe((URL)urlNotifyListenerMap.getKey(), singleNotifyListener);
                        }
                    }

                    var8 = consumerURLs.iterator();

                    while(var8.hasNext()) {
                        url = (URL)var8.next();
                        logger.info("register new url " + url + " into reigstry:" + removedRegistry.getClass().getName() + " for consumer");
                        removedRegistry.register(url);
                    }
                }
            }
        }
    }

    private Set<URL> getRegisteredProviderURLs() {
        Set<URL> urlSet = new HashSet();
        Iterator var2 = super.getRegistered().iterator();

        while(var2.hasNext()) {
            URL url = (URL)var2.next();
            if (url != null && this.isProvider(url)) {
                urlSet.add(url);
            }
        }

        return urlSet;
    }

    private Set<URL> getRegisteredConsumerURLs() {
        Set<URL> urlSet = new HashSet();
        Iterator var2 = super.getRegistered().iterator();

        while(var2.hasNext()) {
            URL url = (URL)var2.next();
            if (url != null && this.isConsumer(url)) {
                urlSet.add(url);
            }
        }

        return urlSet;
    }

    private Map<URL, Set<NotifyListener>> getSubscribedURLMap() {
        return super.getSubscribed();
    }

    private void doRemoveMultipleSubscribed(NotifyListener notifyListener, Registry removedRegistry) {
        if (notifyListener instanceof MultipleNotifyListenerWrapper) {
            ((MultipleNotifyListenerWrapper)notifyListener).getRegistryMap().remove(removedRegistry.getUrl());
        }

    }

    private NotifyListener fetchNotifyListenerAndAddMultipleSubscribed(NotifyListener notifyListener, Registry newRegistry) {
        if (notifyListener instanceof MultipleNotifyListenerWrapper) {
            MultipleNotifyListenerWrapper multipleNotifyListenerWrapper = (MultipleNotifyListenerWrapper)notifyListener;
            SingleNotifyListener singleNotifyListener = new SingleNotifyListener(multipleNotifyListenerWrapper, newRegistry);
            ((MultipleNotifyListenerWrapper)notifyListener).getRegistryMap().put(newRegistry.getUrl(), singleNotifyListener);
            return singleNotifyListener;
        } else {
            throw new IllegalArgumentException("fetch notify listener but not return any MultipleNotifyListenerWrapper." + notifyListener);
        }
    }
}
