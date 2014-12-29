/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding.ext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.beans.*;

public final class BeanAdapterFactory {
    private static final BeanAdapterFactory INSTANCE =  new BeanAdapterFactory();
    private final Map<Object, List<VendedAdapter>> vendedAdapters;
    private final List<BeanAdapterProvider> providers;
    private final Set<ClassLoader> classLoaders;
    private final Set<URL> serviceURLs;

    public static Object getAdapter(Object source, String property) {
        return INSTANCE.getAdapter0(source, property);
    }

    public static List<PropertyDescriptor> getAdapterPropertyDescriptors(Class<?> type) {
        return INSTANCE.getAdapterPropertyDescriptors0(type);
    }

    public BeanAdapterFactory() {
        this.providers = new ArrayList<BeanAdapterProvider>();
        classLoaders = new HashSet<ClassLoader>();
        serviceURLs = new HashSet<URL>();
        vendedAdapters = new WeakHashMap<Object, List<VendedAdapter>>();
    }

    private void loadProvidersIfNecessary() {
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        if (!classLoaders.contains(currentLoader)) {
            classLoaders.add(currentLoader);
            loadProviders(currentLoader);
        }
    }
    
    private void loadProviders(ClassLoader classLoader) {
        // PENDING: this needs to be rewriten in terms of ServiceLoader
        String serviceName = "META-INF/services/" + 
                BeanAdapterProvider.class.getName();
        try {
            Enumeration<URL> urls = classLoader.getResources(serviceName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (!serviceURLs.contains(url)) {
                    serviceURLs.add(url);
                    addProviders(url);
                }
            }
        } catch (IOException ex) {
        }
    }
    
    private void addProviders(URL url) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = url.openStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    providers.add((BeanAdapterProvider)Class.forName(line).newInstance());
                } catch (IllegalAccessException ex) {
                } catch (InstantiationException ex) {
                } catch (ClassNotFoundException ex) {
                }
            }
        } catch (UnsupportedEncodingException ex) {
        } catch (IOException ex) {
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
            }
        }
    }

    public Object getAdapter0(Object source, String property) {
        if (source == null || property == null) {
            throw new IllegalArgumentException();
        }
        loadProvidersIfNecessary();
        property = property.intern();
        BeanAdapterProvider provider = getProvider(source, property);
        if (provider != null) {
            List<VendedAdapter> adapters = vendedAdapters.get(source);
            if (adapters != null) {
                for (int i = adapters.size() - 1; i >= 0; i--) {
                    VendedAdapter vendedAdapter = adapters.get(i);
                    Object adapter = vendedAdapter.getAdapter();
                    if (adapter == null) {
                        vendedAdapters.remove(i);
                    } else if (vendedAdapter.getProvider() == provider && vendedAdapter.getProperty() == property) {
                        return adapter;
                    }
                }
            } else {
                adapters = new ArrayList<VendedAdapter>(1);
                vendedAdapters.put(source, adapters);
            }
            Object adapter = provider.createAdapter(source, property);
            adapters.add(new VendedAdapter(property, provider, adapter));
            return adapter;
        }
        return null;
    }
    
    private BeanAdapterProvider getProvider(Object source, String property) {
        Class<?> type = source.getClass();
        for (BeanAdapterProvider provider : providers) {
            if (provider.providesAdapter(type, property)) {
                return provider;
            }
        }
        return null;
    }

        private List<FeatureDescriptor> getDescriptors(Class<?> type) {
            BeanInfo info = null;
            try {
                info = Introspector.getBeanInfo(type);
            } catch (Exception ex) {
            }
            if (info == null) {
                return Collections.emptyList();
            }
            ArrayList<FeatureDescriptor> list = new ArrayList<FeatureDescriptor>(
                    info.getPropertyDescriptors().length);
            for (PropertyDescriptor pd: info.getPropertyDescriptors()) {
                // PENDING: The following properties come from EL, are they
                // needed?
                if (pd.getPropertyType() != null) {
                    pd.setValue("type", pd.getPropertyType());
                }
                pd.setValue("resolvableAtDesignTime", Boolean.TRUE);
                list.add(pd);
            }
            return list;
        }

    private static BeanInfo getBeanInfo(Class<?> type) {
        try {
            return Introspector.getBeanInfo(type);
        } catch (IntrospectionException ie) {
            return null;
        }
    }

    private List<PropertyDescriptor> getAdapterPropertyDescriptors0(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must be non-null");
        }

        loadProvidersIfNecessary();
        
        ArrayList<PropertyDescriptor> des = new ArrayList<PropertyDescriptor>();

        for (BeanAdapterProvider provider : providers) {
            Class<?> pdType = provider.getAdapterClass(type);
            if (pdType != null) {
                BeanInfo info = getBeanInfo(pdType);
                if (info != null) {
                    PropertyDescriptor[] pds = info.getPropertyDescriptors();
                    if (pds != null) {
                        for (PropertyDescriptor pd : pds) {
                            if (provider.providesAdapter(type, pd.getName())) {
                                des.add(pd);
                            }
                        }
                    }
                }
            }
        }
        
        return des;
    }
    
    private static final class VendedAdapter {
        private final BeanAdapterProvider provider;
        private final String property;
        private final WeakReference<Object> adapter;

        public VendedAdapter(String property, BeanAdapterProvider provider, Object adapter) {
            this.property = property;
            this.adapter = new WeakReference<Object>(adapter);
            this.provider = provider;
        }

        public Object getAdapter() {
            return adapter.get();
        }

        public String getProperty() {
            return property;
        }
        
        public BeanAdapterProvider getProvider() {
            return provider;
        }
    }

}
