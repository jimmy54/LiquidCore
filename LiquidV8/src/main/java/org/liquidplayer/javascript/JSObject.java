/*
 * Copyright (c) 2014 - 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/LiquidCore for terms and conditions.
 */
package org.liquidplayer.javascript;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A JavaScript object.
 * @since 0.1.0
 *
 */
public class JSObject extends JSValue {

    @Retention(RetentionPolicy.RUNTIME)
    /*
     * Exports a property or function to JS environment
     */
    public @interface jsexport {
        Class type() default Object.class;

        int attributes() default JSPropertyAttributeNone;
    }

    /**
     * A JavaScript object property.  A convenience class for declaring properties using
     * the @jsexport attribute.
     *
     * @param <T> the type of the property
     * @since 0.1.0
     */
    public class Property<T> {
        private T temp = null;
        private Class pT;
        private Integer attributes = null;

        private Property() {
        }

        /**
         * Sets the property
         *
         * @param v value to set
         */
        public void set(T v) {
            temp = v;
            if (temp != null)
                pT = temp.getClass();
            if (name != null) {
                if (attributes != null) {
                    property(name, v, attributes);
                    attributes = null;
                } else {
                    property(name, v);
                }
            }
        }

        /**
         * Gets the property
         *
         * @return the value of the property
         */
        @SuppressWarnings("unchecked")
        public T get() {
            if (temp == null && pT == Object.class) {
                context.throwJSException(new JSException(context, "object has no defined type"));
                return null;
            }
            if (name != null) {
                return (T) property(name).toJavaObject(pT);
            } else {
                return this.temp;
            }
        }

        private String name = null;

        private void setName(String n, Class cls, int attributes) {
            name = n;
            this.attributes = attributes;
            if (temp != null) {
                property(name, temp, this.attributes);
                this.attributes = null;
            } else {
                pT = cls;
                property(name, new JSValue(context));
            }
        }
    }

    /**
     * Specifies that a property has no special attributes.
     */
    public static final int JSPropertyAttributeNone = 0;
    /**
     * Specifies that a property is read-only.
     */
    public static final int JSPropertyAttributeReadOnly = 1 << 1;
    /**
     * Specifies that a property should not be enumerated by
     * JSPropertyEnumerators and JavaScript for...in loops.
     */
    public static final int JSPropertyAttributeDontEnum = 1 << 2;
    /**
     * Specifies that the delete operation should fail on a property.
     */
    public static final int JSPropertyAttributeDontDelete = 1 << 3;

    /**
     * Creates a new, empty JavaScript object.  In JS:
     * <pre>
     * {@code
     * var obj = {}; // OR
     * var obj = new Object();
     * }
     * </pre>
     *
     * @param ctx The JSContext to create the object in
     * @since 0.1.0
     */
    public JSObject(JSContext ctx) {
        context = ctx;
        valueRef = context.ctxRef().make();
        addJSExports();
        context.persistObject(this);
    }

    void addJSExports() {
        try {
            for (Field f : getClass().getDeclaredFields()) {
                if (f.isAnnotationPresent(jsexport.class)) {
                    f.setAccessible(true);
                    if (Property.class.isAssignableFrom(f.getType())) {
                        Property prop = (Property) f.get(this);
                        if (prop == null) {
                            Constructor ctor =
                                    f.getType().getDeclaredConstructor(JSObject.class);
                            ctor.setAccessible(true);
                            prop = (Property) ctor.newInstance(this);
                            f.set(this, prop);
                        }
                        prop.setName(f.getName(),
                                f.getAnnotation(jsexport.class).type(),
                                f.getAnnotation(jsexport.class).attributes());
                    }
                }
            }
            Method[] methods = getClass().getDeclaredMethods();
            for (Method m : methods) {
                if (m.isAnnotationPresent(jsexport.class)) {
                    m.setAccessible(true);
                    JSFunction f = new JSFunction(context, m,
                            JSObject.class, JSObject.this);
                    property(m.getName(), f, m.getAnnotation(jsexport.class).attributes());
                }
            }
        } catch (Exception e) {
            context.throwJSException(new JSException(context, e.toString()));
        }
    }

    /**
     * Called only by convenience subclasses.  If you use
     * this, you must set context and valueRef yourself.
     *
     * @since 0.1.0
     */
    public JSObject() {
    }

    /**
     * Wraps an existing object from JavaScript
     *
     * @param objRef The JavaScriptCore object reference
     * @param ctx    The JSContext of the reference
     * @since 0.1.0
     */
    protected JSObject(final JNIJSObject objRef, JSContext ctx) {
        super(objRef, ctx);
        context.persistObject(this);
    }

    /**
     * Creates a new object with function properties set for each method
     * in the defined interface.
     * In JS:
     * <pre>
     * {@code
     * var obj = {
     *     func1: function(a)   { alert(a); },
     *     func2: function(b,c) { alert(b+c); }
     * };
     * }
     * </pre>
     * Where func1, func2, etc. are defined in interface 'iface'.  This JSObject
     * must implement 'iface'.
     *
     * @param ctx   The JSContext to create the object in
     * @param iface The Java Interface defining the methods to expose to JavaScript
     * @since 0.1.0
     */
    public JSObject(JSContext ctx, final Class<?> iface) {
        context = ctx;
        valueRef = context.ctxRef().make();
        addJSExports();
        Method[] methods = iface.getDeclaredMethods();
        for (Method m : methods) {
            JSFunction f = new JSFunction(context, m,
                    JSObject.class, this);
            property(m.getName(), f);
        }
        context.persistObject(this);
    }

    /**
     * Creates a new object with the entries in 'map' set as properties.
     *
     * @param ctx The JSContext to create object in
     * @param map The map containing the properties
     * @since 0.1.0
     */
    @SuppressWarnings("unchecked")
    public JSObject(JSContext ctx, final Map map) {
        this(ctx);
        new JSObjectPropertiesMap<>(this, Object.class).putAll(map);
        addJSExports();
    }

    /**
     * Determines if the object contains a given property
     *
     * @param prop The property to test the existence of
     * @return true if the property exists on the object, false otherwise
     * @since 0.1.0
     */
    public boolean hasProperty(final String prop) {
        return JNI().hasProperty(prop);
    }

    /**
     * Gets the property named 'prop'
     *
     * @param prop The name of the property to fetch
     * @return The JSValue of the property, or null if it does not exist
     * @since 0.1.0
     */
    public JSValue property(final String prop) {
        try {
            return new JSValue(JNI().getProperty(prop), context);
        } catch (JNIJSException excp) {
            context.throwJSException(new JSException(new JSValue(excp.exception, context)));
            return new JSValue(context);
        }
    }

    /**
     * Sets the value of property 'prop'
     *
     * @param prop       The name of the property to set
     * @param val        The Java object to set.  The Java object will be converted to a JavaScript object
     *                   automatically.
     * @param attributes And OR'd list of JSProperty constants
     * @since 0.1.0
     */
    public void property(final String prop, final Object val, final int attributes) {
        JNIJSValue ref = (val instanceof JSValue) ?
                ((JSValue) val).valueRef() : new JSValue(context, val).valueRef();
        try {
            JNI().setProperty(
                    prop,
                    ref,
                    attributes);
        } catch (JNIJSException excp) {
            context.throwJSException(new JSException(new JSValue(excp.exception, context)));
        }
    }

    /**
     * Sets the value of property 'prop'.  No JSProperty attributes are set.
     *
     * @param prop  The name of the property to set
     * @param value The Java object to set.  The Java object will be converted to a JavaScript object
     *              automatically.
     * @since 0.1.0
     */
    public void property(String prop, Object value) {
        property(prop, value, JSPropertyAttributeNone);
    }

    /**
     * Deletes a property from the object
     *
     * @param prop The name of the property to delete
     * @return true if the property was deleted, false otherwise
     * @since 0.1.0
     */
    public boolean deleteProperty(final String prop) {
        try {
            return JNI().deleteProperty(prop);
        } catch (JNIJSException excp) {
            context.throwJSException(new JSException(new JSValue(excp.exception, context)));
            return false;
        }
    }

    /**
     * Returns the property at index 'index'.  Used for arrays.
     *
     * @param index The index of the property
     * @return The JSValue of the property at index 'index'
     * @since 0.1.0
     */
    public JSValue propertyAtIndex(final int index) {
        try {
            return new JSValue(JNI().getPropertyAtIndex(index), context);
        } catch (JNIJSException excp) {
            context.throwJSException(new JSException(new JSValue(excp.exception, context)));
            return new JSValue(context);
        }
    }

    /**
     * Sets the property at index 'index'.  Used for arrays.
     *
     * @param index The index of the property to set
     * @param val The Java object to set, will be automatically converted to a JavaScript value
     * @since 0.1.0
     */
    public void propertyAtIndex(final int index, final Object val) {
        try {
            JNI().setPropertyAtIndex(index,
                    (val instanceof JSValue) ? ((JSValue) val).valueRef()
                            : new JSValue(context, val).valueRef());
        } catch (JNIJSException excp) {
            context.throwJSException(
                    new JSException(new JSValue(excp.exception, context)));
        }
    }

    /**
     * Gets the list of set property names on the object
     *
     * @return A string array containing the property names
     * @since 0.1.0
     */
    public String[] propertyNames() {
        return JNI().copyPropertyNames();
    }

    /**
     * Determines if the object is a function
     *
     * @return true if the object is a function, false otherwise
     * @since 0.1.0
     */
    public boolean isFunction() {
        return JNI().isFunction();
    }

    /**
     * Determines if the object is a constructor
     *
     * @return true if the object is a constructor, false otherwise
     * @since 0.1.0
     */
    public boolean isConstructor() {
        return JNI().isConstructor();
    }

    @Override
    public int hashCode() {
        return (int) valueRef().reference;
    }

    /**
     * Gets the prototype object, if it exists
     *
     * @return A JSValue referencing the prototype object, or null if none
     * @since 0.1.0
     */
    public JSValue prototype() {
        return new JSValue(JNI().getPrototype(), context);
    }

    /**
     * Sets the prototype object
     *
     * @param proto The object defining the function prototypes
     * @since 0.1.0
     */
    public void prototype(final JSValue proto) {
        JNI().setPrototype(proto.valueRef());
    }

    final List<JSObject> zombies = new ArrayList<>();

    @Override
    protected void finalize() throws Throwable {
        if (context != null && persisted) {
            context.finalizeObject(this);
        }
        super.finalize();
    }

    protected void setThis(JSObject thiz) {
        this.thiz = thiz;
    }

    public JSObject getThis() {
        return thiz;
    }

    @SuppressWarnings("unused")
    public JSValue __nullFunc() {
        return new JSValue(context);
    }

    private JSObject thiz = null;

    protected JNIJSObject JNI() {
        return (JNIJSObject) valueRef();
    }

    private long canon = 0;
    boolean persisted = false;
    long canonical() {
        if (canon == 0) {
            canon = valueRef().canonicalReference();
        }
        return canon;
    }

}