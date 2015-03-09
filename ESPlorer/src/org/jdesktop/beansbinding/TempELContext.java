package org.jdesktop.beansbinding;

import java.util.*;
import java.beans.*;
import org.jdesktop.beansbinding.ext.BeanAdapterFactory;
import org.jdesktop.el.BeanELResolver;
import org.jdesktop.el.CompositeELResolver;
import org.jdesktop.el.ELContext;
import org.jdesktop.el.ELResolver;
import org.jdesktop.el.FunctionMapper;
import org.jdesktop.el.MapELResolver;
import org.jdesktop.el.VariableMapper;
import org.jdesktop.el.impl.lang.FunctionMapperImpl;
import org.jdesktop.el.impl.lang.VariableMapperImpl;

/**
 * This class is temporary. Moving forward, we'll instead have a factory for
 * configuring this.
 *
 * @author Shannon Hickey
 */
class TempELContext extends ELContext {
    private final CompositeELResolver resolver;
    private final VariableMapper variableMapper = new VariableMapperImpl();
    private final FunctionMapper functionMapper = new FunctionMapperImpl();
    
    public TempELContext() {
        resolver = new CompositeELResolver();
        // PENDING(shannonh) - EL also has an ArrayELResolver. Should that be added too?
        resolver.add(new MapELResolver());
        resolver.add(new BeanDelegateELResolver());
    }
    
    public ELResolver getELResolver() {
        return resolver;
    }
    
    public FunctionMapper getFunctionMapper() {
        return functionMapper;
    }
    
    public VariableMapper getVariableMapper() {
        return variableMapper;
    }
    
    private class BeanDelegateELResolver extends BeanELResolver {
        public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
            Iterator<FeatureDescriptor> superDescriptors = super.getFeatureDescriptors(context, base);

            if (base != null) {
                List<PropertyDescriptor> pds = BeanAdapterFactory.getAdapterPropertyDescriptors(base.getClass());
                if (pds.size() > 0) {
                    Map<String, FeatureDescriptor> fdMap = new HashMap<String,FeatureDescriptor>();

                    while (superDescriptors.hasNext()) {
                        FeatureDescriptor fd = superDescriptors.next();
                        fdMap.put(fd.getName(), fd);
                    }

                    for (PropertyDescriptor pd : pds) {
                        if (pd.getPropertyType() != null) {
                            pd.setValue("type", pd.getPropertyType());
                            pd.setValue("resolvableAtDesignTime", Boolean.TRUE);
                            fdMap.put(pd.getName(), pd);
                        }
                    }

                    return fdMap.values().iterator();
                }
            }

            return superDescriptors;
        }

        private Object baseOrAdapter(Object base, Object property) {
            if (base != null && property instanceof String) {
                Object adapter = BeanAdapterFactory.getAdapter(base, (String) property);
                if (adapter != null) {
                    return adapter;
                }
            }

            return base;
        }

        public void setValue(ELContext context, Object base, Object property, Object val) {
            super.setValue(context, baseOrAdapter(base, property), property, val);
        }

        public boolean isReadOnly(ELContext context, Object base, Object property) {
            return super.isReadOnly(context, baseOrAdapter(base, property), 
                    property);
        }

        public Object getValue(ELContext context, Object base, Object property) {
            return super.getValue(context, baseOrAdapter(base, property), 
                    property);
        }

        public Class<?> getType(ELContext context, Object base, Object property) {
            return super.getType(context, baseOrAdapter(base, property), 
                    property);
        }
    }

}
