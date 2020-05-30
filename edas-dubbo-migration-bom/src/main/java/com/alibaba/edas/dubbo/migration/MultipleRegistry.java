package com.alibaba.edas.dubbo.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.common.Constants;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.support.AbstractRegistry;

public class MultipleRegistry extends AbstractRegistry {
    public static final String REGISTRY_FOR_SERVICE = "service-registry";
    public static final String REGISTRY_FOR_REFERENCE = "reference-registry";
    protected RegistryFactory registryFactory = (RegistryFactory)ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
    private final Map<String, Registry> serviceRegistries = new ConcurrentHashMap(4);
    private final Map<String, Registry> referenceRegistries = new ConcurrentHashMap(4);
    private final Map<NotifyListener, MultipleRegistry.MultipleNotifyListenerWrapper> multipleNotifyListenerMap = new ConcurrentHashMap(32);
    protected List<String> origServiceRegistryURLs;
    protected List<String> origReferenceRegistryURLs;
    protected List<String> effectServiceRegistryURLs;
    protected List<String> effectReferenceRegistryURLs;
    private URL registryUrl;
    private String applicationName;

    public MultipleRegistry(URL url) {
        super(url);
        this.registryUrl = url;
        this.applicationName = url.getParameter("application");
        this.init();
        this.checkApplicationName(this.applicationName);
        this.origServiceRegistryURLs = this.getParameter(url, "service-registry", new ArrayList());
        this.origReferenceRegistryURLs = this.getParameter(url, "reference-registry", new ArrayList());
        this.effectServiceRegistryURLs = this.filterServiceRegistry(this.origServiceRegistryURLs);
        this.effectReferenceRegistryURLs = this.filterReferenceRegistry(this.origReferenceRegistryURLs);
        boolean defaultRegistry = url.getParameter("default", true);
        if (defaultRegistry && this.effectServiceRegistryURLs.isEmpty() && this.effectReferenceRegistryURLs.isEmpty()) {
            throw new IllegalArgumentException("Illegal registry url. You need to configure parameter service-registry or reference-registry");
        } else {
            Set<String> allURLs = new HashSet(this.effectServiceRegistryURLs);
            allURLs.addAll(this.effectReferenceRegistryURLs);
            Map<String, Registry> tmpMap = new HashMap(4);
            Iterator var5 = allURLs.iterator();

            String referenceReigstyURL;
            while(var5.hasNext()) {
                referenceReigstyURL = (String)var5.next();
                tmpMap.put(referenceReigstyURL, this.registryFactory.getRegistry(URL.valueOf(referenceReigstyURL)));
            }

            var5 = this.effectServiceRegistryURLs.iterator();

            while(var5.hasNext()) {
                referenceReigstyURL = (String)var5.next();
                this.serviceRegistries.put(referenceReigstyURL, tmpMap.get(referenceReigstyURL));
            }

            var5 = this.effectReferenceRegistryURLs.iterator();

            while(var5.hasNext()) {
                referenceReigstyURL = (String)var5.next();
                this.referenceRegistries.put(referenceReigstyURL, tmpMap.get(referenceReigstyURL));
            }

        }
    }

    @Override
    public URL getUrl() {
        return this.registryUrl;
    }

    @Override
    public boolean isAvailable() {
        boolean available = this.serviceRegistries.isEmpty();
        Iterator var2 = this.serviceRegistries.values().iterator();

        Registry referenceRegistry;
        while(var2.hasNext()) {
            referenceRegistry = (Registry)var2.next();
            if (referenceRegistry.isAvailable()) {
                available = true;
            }
        }

        if (!available) {
            return false;
        } else {
            available = this.referenceRegistries.isEmpty();
            var2 = this.referenceRegistries.values().iterator();

            while(var2.hasNext()) {
                referenceRegistry = (Registry)var2.next();
                if (referenceRegistry.isAvailable()) {
                    available = true;
                }
            }

            if (!available) {
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public void destroy() {
        Set<Registry> registries = new HashSet(this.serviceRegistries.values());
        registries.addAll(this.referenceRegistries.values());
        Iterator var2 = registries.iterator();

        while(var2.hasNext()) {
            Registry registry = (Registry)var2.next();
            registry.destroy();
        }

    }

    @Override
    public void register(URL url) {
        super.register(url);
        Iterator var2;
        Registry registry;
        if (this.isConsumer(url)) {
            var2 = this.referenceRegistries.values().iterator();

            while(var2.hasNext()) {
                registry = (Registry)var2.next();
                registry.register(url);
            }

        } else {
            var2 = this.serviceRegistries.values().iterator();

            while(var2.hasNext()) {
                registry = (Registry)var2.next();
                registry.register(url);
            }

        }
    }

    @Override
    public void unregister(URL url) {
        super.unregister(url);
        Iterator var2;
        Registry registry;
        if (this.isConsumer(url)) {
            var2 = this.referenceRegistries.values().iterator();

            while(var2.hasNext()) {
                registry = (Registry)var2.next();
                registry.unregister(url);
            }

        } else {
            var2 = this.serviceRegistries.values().iterator();

            while(var2.hasNext()) {
                registry = (Registry)var2.next();
                registry.unregister(url);
            }

        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        MultipleRegistry.MultipleNotifyListenerWrapper multipleNotifyListenerWrapper = new MultipleRegistry.MultipleNotifyListenerWrapper(listener);
        this.multipleNotifyListenerMap.put(listener, multipleNotifyListenerWrapper);
        Iterator var4 = this.referenceRegistries.values().iterator();

        while(var4.hasNext()) {
            Registry registry = (Registry)var4.next();
            MultipleRegistry.SingleNotifyListener singleNotifyListener = new MultipleRegistry.SingleNotifyListener(multipleNotifyListenerWrapper, registry);
            multipleNotifyListenerWrapper.putRegistryMap(registry.getUrl(), singleNotifyListener);
            registry.subscribe(url, singleNotifyListener);
        }

        super.subscribe(url, multipleNotifyListenerWrapper);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        MultipleRegistry.MultipleNotifyListenerWrapper notifyListener = (MultipleRegistry.MultipleNotifyListenerWrapper)this.multipleNotifyListenerMap.remove(listener);
        Iterator var4 = this.referenceRegistries.values().iterator();

        while(var4.hasNext()) {
            Registry registry = (Registry)var4.next();
            MultipleRegistry.SingleNotifyListener singleNotifyListener = (MultipleRegistry.SingleNotifyListener)notifyListener.registryMap.get(registry.getUrl());
            registry.unsubscribe(url, singleNotifyListener);
        }

        if (notifyListener != null) {
            super.unsubscribe(url, notifyListener);
            notifyListener.destroy();
        }

    }

    @Override
    public List<URL> lookup(URL url) {
        List<URL> urls = new ArrayList();
        Iterator var3 = this.referenceRegistries.values().iterator();

        while(var3.hasNext()) {
            Registry registry = (Registry)var3.next();
            List<URL> tmpUrls = registry.lookup(url);
            if (!CollectionUtils.isEmpty(tmpUrls)) {
                urls.addAll(tmpUrls);
            }
        }

        return urls;
    }

    protected boolean isProvider(URL url) {
        return "provider".equals(url.getProtocol()) || "provider".equals(url.getParameter("side")) || "providers".equals(url.getParameter("category"));
    }

    protected boolean isConsumer(URL url) {
        return "consumer".equals(url.getProtocol()) || "consumer".equals(url.getParameter("side")) || "consumers".equals(url.getParameter("category"));
    }

    protected void init() {
    }

    protected List<String> filterServiceRegistry(List<String> serviceRegistryURLs) {
        return serviceRegistryURLs;
    }

    protected List<String> filterReferenceRegistry(List<String> referenceRegistryURLs) {
        return referenceRegistryURLs;
    }

    protected void checkApplicationName(String applicationName) {
    }

    protected String getApplicationName() {
        return this.applicationName;
    }

    public Map<String, Registry> getServiceRegistries() {
        return this.serviceRegistries;
    }

    public Map<String, Registry> getReferenceRegistries() {
        return this.referenceRegistries;
    }

    public List<String> getOrigServiceRegistryURLs() {
        return this.origServiceRegistryURLs;
    }

    public List<String> getOrigReferenceRegistryURLs() {
        return this.origReferenceRegistryURLs;
    }

    public List<String> getEffectServiceRegistryURLs() {
        return this.effectServiceRegistryURLs;
    }

    public List<String> getEffectReferenceRegistryURLs() {
        return this.effectReferenceRegistryURLs;
    }

    private List<String> getParameter(URL url, String key, List<String> defaultValue) {
        String value = url.getParameter(key);
        if (value != null && value.length() != 0) {
            String[] strArray = Constants.COMMA_SPLIT_PATTERN.split(value);
            return Arrays.asList(strArray);
        } else {
            return defaultValue;
        }
    }

    protected static class SingleNotifyListener implements NotifyListener {
        MultipleRegistry.MultipleNotifyListenerWrapper multipleNotifyListenerWrapper;
        Registry registry;
        volatile List<URL> urlList;

        public SingleNotifyListener(MultipleRegistry.MultipleNotifyListenerWrapper multipleNotifyListenerWrapper, Registry registry) {
            this.registry = registry;
            this.multipleNotifyListenerWrapper = multipleNotifyListenerWrapper;
        }

        @Override
        public synchronized void notify(List<URL> urls) {
            this.urlList = urls;
            if (this.multipleNotifyListenerWrapper != null) {
                this.multipleNotifyListenerWrapper.notifySourceListener();
            }

        }

        public void destroy() {
            this.multipleNotifyListenerWrapper = null;
            this.registry = null;
        }

        public List<URL> getUrlList() {
            return this.urlList;
        }
    }

    protected static class MultipleNotifyListenerWrapper implements NotifyListener {
        Map<URL, MultipleRegistry.SingleNotifyListener> registryMap = new ConcurrentHashMap(4);
        NotifyListener sourceNotifyListener;

        public MultipleNotifyListenerWrapper(NotifyListener sourceNotifyListener) {
            this.sourceNotifyListener = sourceNotifyListener;
        }

        public void putRegistryMap(URL registryURL, MultipleRegistry.SingleNotifyListener singleNotifyListener) {
            this.registryMap.put(registryURL, singleNotifyListener);
        }

        public void destroy() {
            Iterator var1 = this.registryMap.values().iterator();

            while(var1.hasNext()) {
                MultipleRegistry.SingleNotifyListener singleNotifyListener = (MultipleRegistry.SingleNotifyListener)var1.next();
                if (singleNotifyListener != null) {
                    singleNotifyListener.destroy();
                }
            }

            this.registryMap.clear();
            this.sourceNotifyListener = null;
        }

        public synchronized void notifySourceListener() {
            List<URL> notifyURLs = new ArrayList();
            URL emptyURL = null;
            Iterator var3 = this.registryMap.values().iterator();

            while(true) {
                while(true) {
                    List tmpUrls;
                    do {
                        if (!var3.hasNext()) {
                            if (emptyURL != null && notifyURLs.isEmpty()) {
                                notifyURLs.add(emptyURL);
                            }

                            this.notify(notifyURLs);
                            return;
                        }

                        MultipleRegistry.SingleNotifyListener singleNotifyListener = (MultipleRegistry.SingleNotifyListener)var3.next();
                        tmpUrls = singleNotifyListener.getUrlList();
                    } while(CollectionUtils.isEmpty(tmpUrls));

                    if (tmpUrls.size() == 1 && tmpUrls.get(0) != null && "empty".equals(((URL)tmpUrls.get(0)).getProtocol())) {
                        if (emptyURL == null) {
                            emptyURL = (URL)tmpUrls.get(0);
                        }
                    } else {
                        notifyURLs.addAll(tmpUrls);
                    }
                }
            }
        }

        @Override
        public void notify(List<URL> urls) {
            this.sourceNotifyListener.notify(urls);
        }

        public Map<URL, MultipleRegistry.SingleNotifyListener> getRegistryMap() {
            return this.registryMap;
        }
    }
}