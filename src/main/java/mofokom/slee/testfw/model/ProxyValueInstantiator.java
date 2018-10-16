package mofokom.slee.testfw.model;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;

/**
 *
 * @author wozza
 */
@Slf4j
public class ProxyValueInstantiator<T> extends ValueInstantiator implements Serializable {

    private final Class<T> clazz;
    private Map<String, Object> map;
    private Class[] additionalProxyInterfaces;

    public ProxyValueInstantiator(Class<T> clazz, Map<String, Object> initial, Class[] additionalProxyInterfaces) {
        this(clazz, additionalProxyInterfaces);
        this.map = initial;
    }

    public ProxyValueInstantiator(Class<T> clazz, Class[] additionalProxyInterfaces) {
        this(clazz);
        this.additionalProxyInterfaces = additionalProxyInterfaces;
    }

    public ProxyValueInstantiator(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
        return createProxy();
    }

    @Override
    public boolean canCreateUsingDefault() {
        return true;
    }

    public static Object getInnerMock(Object proxy) {
        if (Proxy.isProxyClass(proxy.getClass())) {
            return ((ext) proxy).getInnerMock();
        } else {
            throw new IllegalArgumentException("not a proxy ");
        }
    }

    public T createProxy() {
        List<Class> cList = new ArrayList();
        cList.addAll(Arrays.asList(new Class[]{clazz, ext.class, Serializable.class}));
        if (additionalProxyInterfaces != null) {
            cList.addAll(Arrays.asList(additionalProxyInterfaces));
        }

        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), cList.toArray(new Class[cList.size()]), new BeanInvocationHandler(clazz, map));
    }

    public interface ext {

        Object getInnerMock();

    }

    public class MockBeanInvocationHandler<T> implements InvocationHandler, Serializable {

        Map<String, Object> state;
        Map<Class, Object> defaultMap;
        private final Class<T> clazz;
        transient private final T mock;

        public MockBeanInvocationHandler(Class<T> c, Map<String, Object> initial) {
            this(c);
            if (initial != null) {
                state.putAll(initial);
            }
        }

        public MockBeanInvocationHandler(Class<T> c) {
            this.clazz = c;
            this.mock = (T) Mockito.mock(c);
            state = new HashMap<>();
            defaultMap = new HashMap<>();
            defaultMap.put(int.class, 0);
            defaultMap.put(long.class, 0l);
            defaultMap.put(short.class, 0);
            defaultMap.put(boolean.class, false);
            defaultMap.put(char.class, (char) 0);
            defaultMap.put(double.class, 0d);
            defaultMap.put(float.class, 0f);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            /**
             * System.out.println("**" +
             * method.getDeclaringClass().getSimpleName() + "." +
             * method.getName());
             *
             *
             */
            if (method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(this, args);
            } else if (method.getName().equals("getInnerMock")) {
                return mock;
            } else if (method.getName().startsWith("create")) {
                return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{method.getReturnType()}, new BeanInvocationHandler(clazz, map));
            } else if (method.getName().startsWith("set")) {
                if (args != null) {
                    System.out.println("**"
                            + method.getDeclaringClass().getSimpleName() + "."
                            + method.getName() + "==" + Arrays.asList(args).toString());
                }
                this.state.put(method.getName().substring(3), args[0]);
                Object o = Mockito.doReturn(args[0]).when(mock);
                Method getter = clazz.getMethod("g" + method.getName().substring(1));
                getter.invoke(o);
            } else if (method.getName().startsWith("get")) {
                if (method.getDeclaringClass().equals(ext.class)) {
                    Object o = this.state.get(method.getName().substring(3));
                    if (o == null && method.getReturnType().isPrimitive()) {
                        return defaultMap.get(method.getReturnType());
                    }
                    return o;
                }
                return method.invoke(mock, args);
            } else if (method.getName().startsWith("has")) {
                return this.state.containsKey(method.getName().substring(3));
            }
            return Void.TYPE;
        }
    }

    public class BeanInvocationHandler<T> implements InvocationHandler, Serializable {

        Map<String, Object> state;
        Map<Class, Object> defaultMap;
        private final Class<T> clazz;

        public BeanInvocationHandler(Class<T> c, Map<String, Object> initial) {
            this(c);
            if (initial != null) {
                state.putAll(initial);
            }
        }

        public BeanInvocationHandler(Class<T> c) {
            this.clazz = c;
            state = new HashMap<>();
            defaultMap = new HashMap<>();
            defaultMap.put(int.class, 0);
            defaultMap.put(long.class, 0l);
            defaultMap.put(short.class, 0);
            defaultMap.put(boolean.class, false);
            defaultMap.put(char.class, (char) 0);
            defaultMap.put(double.class, 0d);
            defaultMap.put(float.class, 0f);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(this, args);
            } else if (method.getName().startsWith("create")) {
                return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{method.getReturnType()}, new BeanInvocationHandler(clazz, map));
            } else if (method.getName().startsWith("set")) {
                if (args != null) {
                    log.debug("**"
                            + method.getDeclaringClass().getSimpleName() + "."
                            + method.getName() + "==" + Arrays.asList(args).toString());
                }
                this.state.put(method.getName().substring(3), args[0]);
            } else if (method.getName().startsWith("get")) {
                Object o = this.state.get(method.getName().substring(3));
                if (o == null && method.getReturnType().isPrimitive()) {
                    return defaultMap.get(method.getReturnType());
                }
                return o;
            } else if (method.getName().startsWith("has")) {
                return this.state.containsKey(method.getName().substring(3));
            }
            return Void.TYPE;
        }

    }

}
/**
 * SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
 * resolver.addMapping(CreditControlRequest.class,
 * GenericProxyBeanFactory.class);
 *
 * module.setAbstractTypes(resolver);
 *
 * SimpleKeyDeserializers simpleKeyDeserializers = new SimpleKeyDeserializers();
 * simpleKeyDeserializers.addDeserializer(CreditControlRequest.class, new
 * KeyDeserializer() {
 *
 * @Override public Object deserializeKey(String key, DeserializationContext
 * ctxt) throws IOException { throw new UnsupportedOperationException("Not
 * supported yet."); //To change body of generated methods, choose Tools |
 * Templates. } }); super.setKeyDeserializers(simpleKeyDeserializers);
 */
/**
 * public class DiameterMockSerializer<T> extends StdSerializer<T> {
 *
 * public DiameterMockSerializer(Class<T> t) { super(t); }
 *
 * @Override public void serialize(T value, JsonGenerator gen,
 * SerializerProvider provider) throws IOException {
 * System.out.println(value.getClass()); gen.writeStartObject(); if (value
 * instanceof CreditControlRequest) { provider.defaultSerializeValue(value,
 * gen); } gen.writeEndObject(); } }
 *
 */
