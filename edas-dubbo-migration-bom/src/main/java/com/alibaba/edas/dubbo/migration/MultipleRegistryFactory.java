package com.alibaba.edas.dubbo.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

public class MultipleRegistryFactory extends AbstractRegistryFactory {
    public MultipleRegistryFactory() {
    }

    @Override
    protected Registry createRegistry(URL url) {
        return new MultipleRegistry(url);
    }
}