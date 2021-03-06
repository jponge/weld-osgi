/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.cdi.impl.extension;

import org.osgi.cdi.api.extension.annotation.Filter;
import org.osgi.cdi.api.extension.annotation.OSGiService;
import org.osgi.cdi.impl.extension.beans.DynamicServiceHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;

/**
 * This the bean class for all beans generated from a {@link org.osgi.cdi.api.extension.annotation.OSGiService} annotated {@link InjectionPoint}.
 *
 * @author Mathieu ANCELIN - SERLI (mathieu.ancelin@serli.com)
 * @author Matthieu CLOCHARD - SERLI (matthieu.clochard@serli.com)
 */
public class OSGiServiceBean implements Bean {

    private final static Logger logger = LoggerFactory.getLogger(OSGiServiceBean.class);

    private final Map<Object, DynamicServiceHandler> handlers = new HashMap<Object, DynamicServiceHandler>();
    private final InjectionPoint injectionPoint;
    private Filter filter;
    private Set<Annotation> qualifiers;
    private Type type;
    private long timeout;

    protected OSGiServiceBean(InjectionPoint injectionPoint) {
        logger.debug("Creation of a new OSGiServiceBean for injection point: {}",injectionPoint);
        this.injectionPoint = injectionPoint;
        type = injectionPoint.getType();
        qualifiers = injectionPoint.getQualifiers();
        filter = FilterGenerator.makeFilter(injectionPoint);
        for (Annotation annotation : injectionPoint.getQualifiers()) {
            if (annotation.annotationType().equals(OSGiService.class)) {
                timeout = ((OSGiService) annotation).value();
                break;
            }
        }
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> s = new HashSet<Type>();
        s.add(injectionPoint.getType());
        s.add(Object.class);
        return s;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> result = new HashSet<Annotation>();
        result.addAll(qualifiers);
        result.add(new AnnotationLiteral<Any>() {});
        return result;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Class getBeanClass() {
        return (Class)type;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public Object create(CreationalContext ctx) {
        logger.debug("Instantiation of an {}", this);
        try {
            Bundle bundle = FrameworkUtil.getBundle(injectionPoint.getMember().getDeclaringClass());
            DynamicServiceHandler handler =
                    new DynamicServiceHandler(bundle, ((Class)type).getName(), filter, qualifiers, timeout);
            Object proxy = Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[]{getBeanClass()},
                    handler);
            if(handlers.containsKey(proxy)) {
                handler.setStored(true);
            } else {
                handlers.put(proxy, handler);
                handler.setStored(true);
            }
            return proxy;
        } catch (Exception e) {
            logger.error("Unable to instantiate {} due to {}",this,e);
            throw new CreationException(e);
        }
    }

    @Override
    public void destroy(Object instance, CreationalContext creationalContext) {
        // Nothing to do, services are unget after each call.
        DynamicServiceHandler handler = handlers.get(instance);
        if (handler != null) {
            handler.closeHandler();
            handlers.remove(instance);
        } else {
            logger.info("Can't close tracker for bean {}", this.toString());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OSGiServiceBean)) return false;

        OSGiServiceBean that = (OSGiServiceBean) o;

        if (!filter.value().equals(that.filter.value())) return false;
        if (!getTypes().equals(that.getTypes())) return false;
        if (!getQualifiers().equals(that.getQualifiers())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getTypes().hashCode();
        result = 31 * result + filter.value().hashCode();
        result = 31 * result + getQualifiers().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "OSGiServiceBean [" +
                ((Class)type).getSimpleName() +
                "] with qualifiers [" +
                printQualifiers() +
                "]";
    }

    public String printQualifiers() {
        String result = "";
        for(Annotation qualifier : getQualifiers()) {
            if(!result.equals("")) {
                result += " ";
            }
            result += "@" + qualifier.annotationType().getSimpleName();
            result += printValues(qualifier);
        }
        return result;
    }

    private String printValues(Annotation qualifier) {
        String result = "(";
        for (Method m : qualifier.annotationType().getDeclaredMethods()) {
            if (!m.isAnnotationPresent(Nonbinding.class)) {
                try {
                    Object value = m.invoke(qualifier);
                    if (value == null) {
                        value = m.getDefaultValue();
                    }
                    if(value != null) {
                        result += m.getName() + "=" + value.toString();
                    }
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
        result += ")";
        return result.equals("()") ? "" : result;
    }
}
