package com.alibaba.edas.dubbo.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

public class EdasMultipleRegistryFactory extends AbstractRegistryFactory {
    public EdasMultipleRegistryFactory() {
    }

    @Override
    protected Registry createRegistry(URL url) {
        return new EdasMultipleRegistry(url);
    }
}