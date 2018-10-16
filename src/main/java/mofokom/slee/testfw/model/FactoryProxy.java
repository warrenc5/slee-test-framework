package mofokom.slee.testfw.model;

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
public class FactoryProxy<T> implements Serializable{

    private final Class<T> clazz;
    private Map<String, Object> map;
    private Class[] additionalProxyInterfaces;

    public FactoryProxy(Class<T> clazz, Map<String, Object> initial, Class... additionalProxyInterfaces) {
        this(clazz, additionalProxyInterfaces);
        this.map = initial;
    }

    public FactoryProxy(Class<T> clazz, Class... additionalProxyInterfaces) {
        this(clazz);
        this.additionalProxyInterfaces = additionalProxyInterfaces;
    }

    public FactoryProxy(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static Object getInnerMock(Object proxy) {
        if (Proxy.isProxyClass(proxy.getClass())) {
            return ((ext) proxy).getInnerMock();
        } else {
            throw new IllegalArgumentException("not a proxy");
        }
    }

    public T createProxy() {
        List<Class> cList = new ArrayList();
        cList.addAll(Arrays.asList(new Class[]{clazz, ext.class}));
        if (additionalProxyInterfaces != null) {
            cList.addAll(Arrays.asList(additionalProxyInterfaces));
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = this.getClass().getClassLoader();
        }
        return (T) Proxy.newProxyInstance(cl, cList.toArray(new Class[cList.size()]), new BeanInvocationHandler(clazz, map));
    }

    public interface ext {

        Object getInnerMock();

    }

    public class BeanInvocationHandler<T> implements InvocationHandler, Serializable {

        Map<String, Object> state;
        Map<Class, Object> defaultMap;
        private final Class<T> clazz;
        private transient final T mock;
        private Object o;

        public BeanInvocationHandler(Class<T> c, Map<String, Object> initial) {
            this(c);
            if (initial != null) {
                state.putAll(initial);
            }
        }

        public BeanInvocationHandler(Class<T> c) {
            this.o = new Object();
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
            if (method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(o, args);
            } else if (method.getName().equals("getInnerMock")) {
                return mock;
            } else if (method.getName().startsWith("create")) {
                log.debug(method.getDeclaringClass().getSimpleName() + "." + method.getName());
                return new FactoryProxy(method.getReturnType()).createProxy();
            } else if (method.getName().startsWith("set")) {
                log.debug(method.getDeclaringClass().getSimpleName() + "." + method.getName() + "==" + Arrays.asList(args).toString());
                this.state.put(method.getName().substring(3), args[0]);
                //Object o = Mockito.doReturn(args[0]).when(mock);
                //Method getter = clazz.getMethod("g" + method.getName().substring(1));
                //getter.invoke(o);
            } else if (method.getName().startsWith("get")) {
                log.debug(method.getDeclaringClass().getSimpleName() + "." + method.getName());
                //if (method.getDeclaringClass().equals(ext.class)) {
                Object o = this.state.get(method.getName().substring(3));
                if (o == null && method.getReturnType().isPrimitive()) {
                    return defaultMap.get(method.getReturnType());
                }
                if (o == null) {
                    o = this.state.get(method.getName().substring(3) + "s");
                }

                return o;
                //}
                //return method.invoke(mock, args);
            } else if (method.getName().startsWith("has")) {
                return this.state.containsKey(method.getName().substring(3));
            }
            return Void.TYPE;
        }

        @Override
        public String toString() {
            return "BeanInvocationHandler{" + "state=" + state + ", defaultMap=" + defaultMap + ", clazz=" + clazz + ", mock=" + mock + '}';
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
