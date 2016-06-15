/**
 *   Copyright 2014 Nortal AS
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.nortal.petit.beanmapper;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;

import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * @author Aleksei Lissitsin
 * 
 */
public class BeanMappingReflectionUtils {

    private static Column getAttributeOverride(Class<?> type, String name) {
        AttributeOverride ao = type.getAnnotation(AttributeOverride.class);
        if (ao != null) {
            if (ao.name().equals(name)) {
                return ao.column();
            }
        }

        AttributeOverrides aos = type.getAnnotation(AttributeOverrides.class);
        if (aos != null) {
            for (AttributeOverride a : aos.value()) {
                if (a.name().equals(name)) {
                    return a.column();
                }
            }
        }

        return null;
    }

    private static void addAll(List<Annotation> l, Annotation[] ans) {
        if (ans != null) {
            for (Annotation a : ans) {
                if (a instanceof AttributeOverrides) {
                    l.addAll(Arrays.asList(((AttributeOverrides) a).value()));
                } else {
                    l.add(a);
                }
            }
        }
    }

    private static void readAnnotations(List<Annotation> l, Class<?> type, String name) {
        Column ao = getAttributeOverride(type, name);
        if (ao != null) {
            l.add(ao);
        }
        Field field = FieldUtils.getDeclaredField(type, name, true);
        if (field != null) {
            addAll(l, field.getAnnotations());
        }
        PropertyDescriptor pd = getPropertyDescriptor(type, name);
        if (pd != null) {
            if (pd.getReadMethod() != null) {
                addAll(l, pd.getReadMethod().getAnnotations());
            }
        }
        if (type.getSuperclass() != null) {
            readAnnotations(l, type.getSuperclass(), name);
        }
    }

    static PropertyDescriptor getPropertyDescriptor(Class<?> type, String name) {
        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(type).getPropertyDescriptors();
            for (PropertyDescriptor pd : propertyDescriptors) {
                if (name.equals(pd.getName())) {
                    return pd;
                }
            }
            return null;
        } catch (IntrospectionException e) {
            throw new RuntimeException("Error introspecting type " + type, e);
        }

    }
    
    /**
     * COPIED FROM Spring's AnnotationUtils.
     * 
     * Find a single {@link Annotation} of {@code annotationType} from the supplied {@link Class},
     * traversing its interfaces and superclasses if no annotation can be found on the given class itself.
     * <p>This method explicitly handles class-level annotations which are not declared as
     * {@link java.lang.annotation.Inherited inherited} <i>as well as annotations on interfaces</i>.
     * <p>The algorithm operates as follows: Searches for an annotation on the given class and returns
     * it if found. Else searches all interfaces that the given class declares, returning the annotation
     * from the first matching candidate, if any. Else proceeds with introspection of the superclass
     * of the given class, checking the superclass itself; if no annotation found there, proceeds
     * with the interfaces that the superclass declares. Recursing up through the entire superclass
     * hierarchy if no match is found.
     * @param clazz the class to look for annotations on
     * @param annotationType the annotation type to look for
     * @return the annotation found, or {@code null} if none found
     */
    static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
        A annotation = clazz.getAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        for (Class<?> ifc : clazz.getInterfaces()) {
            annotation = findAnnotation(ifc, annotationType);
            if (annotation != null) {
                return annotation;
            }
        }
        if (!Annotation.class.isAssignableFrom(clazz)) {
            for (Annotation ann : clazz.getAnnotations()) {
                annotation = findAnnotation(ann.annotationType(), annotationType);
                if (annotation != null) {
                    return annotation;
                }
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || superclass.equals(Object.class)) {
            return null;
        }
        return findAnnotation(superclass, annotationType);
    }

    static List<Annotation> readAnnotations(Class<?> type, String name) {
        List<Annotation> res = new ArrayList<Annotation>();
        readAnnotations(res, type, name);
        return res;
    }

    @SuppressWarnings("unchecked")
    static <B extends Annotation> B getAnnotation(List<Annotation> ans, Class<B> annotationType) {
        if (ans != null) {
            for (Annotation a : ans) {
                if (a.annotationType().isAssignableFrom(annotationType)) {
                    return (B) a;
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public static <B extends Annotation> B getAnnotationRecursively(List<Annotation> ans, Class<B> annotationType, int depth) {
        if (ans != null && depth > 0) {
            for (Annotation a : ans) {
                if (a.annotationType().isAssignableFrom(annotationType)) {
                    return (B) a;
                } else {
                	B b = getAnnotationRecursively(Arrays.asList(a.annotationType().getAnnotations()), annotationType, depth - 1);
                	if (b != null) {
                		return b;
                	}
                }
            }
        }
        return null;
    }

    static Column getAttributeOverride(List<Annotation> ans, String name) {
        if (ans != null) {
            for (Annotation a : ans) {
                if (a instanceof AttributeOverride) {
                    AttributeOverride ao = (AttributeOverride) a;
                    if (ao.name().equals(name)) {
                        return ao.column();
                    }
                }
            }
        }
        return null;
    }
}
