package mofokom.slee.testfw.resource;

import mofokom.slee.testfw.SleeComponent;
import javax.slee.SbbContext;
import javax.slee.Sbb;
import javax.slee.ActivityContextInterface;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.LogManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.Map.Entry;
import javax.slee.EventTypeID;
import javax.slee.resource.InvalidConfigurationException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import javax.slee.usage.SampleStatistics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.Properties;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import javax.slee.transaction.SleeTransaction;
import javax.transaction.Transaction;
import javax.slee.resource.ConfigProperties.Property;
import javax.slee.resource.ResourceAdaptor;
import java.util.Arrays;
import javax.slee.*;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.EventLookupFacility;
import javax.slee.facilities.Tracer;
import javax.slee.resource.*;
import javax.slee.transaction.SleeTransactionManager;
import mofokom.slee.testfw.MockSlee;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public abstract class MockResourceAdaptor<RA extends ResourceAdaptor, SBB, USAGE, ACI> extends SleeComponent {

    private ConfigProperties properties;
    private RA ra;
    private SBB sbbInterface;
    private USAGE usage;
    private Tracer tracer;
    private ResourceAdaptorContext context;
    private SleeEndpoint se;
    private EventLookupFacility elf;
    private SleeTransactionManager stm;
    private SleeTransaction stxn;
    private Transaction txn;
    private Timer timer;
    private AlarmFacility af;
    private Logger log;
    private static final Logger slog = Logger.getAnonymousLogger();
    private final Class usageClass, raClass, sbbInterfaceClass;
    private String name;
    private boolean setup;
    private SbbContext sbbContext;
    private ActivityContextInterface aci;
    private final Class<ACI> aciClass;
    private final Map<Object, ActivityHandle> aciMap = new HashMap<Object, ActivityHandle>();
    private final Map<ActivityHandle, ActivityContextInterface> ahMap = new HashMap<ActivityHandle, ActivityContextInterface>();
    private ACI acif;

    public MockResourceAdaptor(Class<RA> raClass, Class<SBB> sbbInterfaceClass, Class<USAGE> usageClass, Class<ACI> aciClass) throws Exception {
        this(raClass.getSimpleName(), raClass, sbbInterfaceClass, usageClass, null);
    }

    public MockResourceAdaptor(String raEntityName, Class<RA> raClass, Class<SBB> sbbInterfaceClass, Class<USAGE> usageClass, Class<ACI> aciClass) throws Exception {
        this.name = raEntityName;

        this.raClass = raClass;
        this.usageClass = usageClass;
        this.sbbInterfaceClass = sbbInterfaceClass;
        this.aciClass = aciClass;

        init();
        setup();

    }

    public void init() {
        log = Logger.getLogger(this.getClass().getSimpleName());
        properties = new ConfigProperties() /*
                 * {
                 *
                 *
                 * @Override public Property getProperty(String name) { Property
                 * o= super.getProperty(name); if(o==null) throw new
                 * javax.slee.resource.InvalidConfigurationException(name);
                 * return o; } }
                 */;
    }

    public void addProperty(String name, Object value) {
        properties.addProperty(new Property(name, value.getClass().getName(), value));
    }
    Map<String, BigInteger> use = new HashMap<String, BigInteger>();
    Map<String, List<BigInteger>> sample = new HashMap<String, List<BigInteger>>();

    public void setup() throws Exception {
        ra = (RA) mock(raClass);
        context = mock(ResourceAdaptorContext.class);

        stm = mock(SleeTransactionManager.class);
        txn = mock(Transaction.class);
        stxn = mock(SleeTransaction.class);
        se = mock(SleeEndpoint.class);
        elf = mock(EventLookupFacility.class);
        af = mock(AlarmFacility.class);
        tracer = mock(Tracer.class);

        usage = (USAGE) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{usageClass}, new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String mn = method.getName().substring(3);
                if (method.getName().startsWith("set")) {
                    increment(mn, (Long) args[0]);
                } else if (method.getName().startsWith("get")) {
                    Object o = use.get(mn);
                    if (o instanceof BigInteger) {
                        return ((BigInteger) o).longValue();
                    }

                    List<BigInteger> l = getSample(mn);
                    return getSampleStatistics(l);

                } else if (method.getName().startsWith("sample")) {
                    sample(method.getName().substring(6), (Long) args[0]);
                }

                return null;
            }

            private void increment(String mn, Long aLong) {
                BigInteger bi = use.get(mn);
                if (bi == null) {
                    bi = BigInteger.ZERO;
                }
                bi.add(BigInteger.valueOf(aLong));
                use.put(mn, bi);
            }

            private void sample(String substring, Long aLong) {
                getSample(substring).add(BigInteger.valueOf(aLong));
            }

            private List<BigInteger> getSample(String mn) {
                List<BigInteger> bi = sample.get(mn);
                if (bi == null) {
                    bi = new ArrayList<BigInteger>();
                    sample.put(mn, bi);
                }
                return bi;
            }
        });

        timer = new Timer();

        //TODO load default Config Properties.
        // ABSTRACT RA METHODS
        Answer loggingAnswer = new LoggingAnswer();

        Answer pa2 = new Answer() {

            public Object answer(InvocationOnMock invocation) throws Throwable {
                log.info(invocation.getMethod().getName() + " " + Arrays.asList(invocation.getArguments()).toString());
                int i = 0;
                ActivityHandle handle = (ActivityHandle) invocation.getArguments()[i++];
                FireableEventType eventType = (FireableEventType) invocation.getArguments()[i++];
                Object activity = invocation.getArguments()[i++];
                Address address = (Address) invocation.getArguments()[i++];
                ReceivableService service = (ReceivableService) invocation.getArguments()[i++];
                if (activity == null) {
                    throw new NullPointerException("activity is null");
                }
                MockResourceAdaptor.this.onAnyEvent(handle, eventType, activity, address, service);
                return null;
            }
        };

        Answer<FireableEventType> pa3 = new Answer<FireableEventType>() {

            public FireableEventType answer(InvocationOnMock invocation) throws Throwable {
                System.out.println(invocation.getMethod().getName() + " " + Arrays.asList(invocation.getArguments()).toString());
                EventTypeID et = (EventTypeID) invocation.getArguments()[0];
                FireableEventType fet = mock(FireableEventType.class);
                doReturn(et).when(fet).getEventType();
                doReturn(et.toString()).when(fet).toString();

                return fet;
            }
        };

        doReturn(Boolean.TRUE).when(tracer).isInfoEnabled();
        doReturn(Boolean.TRUE).when(tracer).isFineEnabled();
        doReturn(Boolean.TRUE).when(tracer).isSevereEnabled();

        doAnswer(loggingAnswer).when(tracer).finest(anyString());
        doAnswer(loggingAnswer).when(tracer).finest(anyString(), any(Throwable.class));
        doAnswer(loggingAnswer).when(tracer).fine(anyString());
        doAnswer(loggingAnswer).when(tracer).fine(anyString(), any(Throwable.class));
        doAnswer(loggingAnswer).when(tracer).info(anyString());
        doAnswer(loggingAnswer).when(tracer).info(anyString(), any(Throwable.class));
        doAnswer(loggingAnswer).when(tracer).warning(anyString());
        doAnswer(loggingAnswer).when(tracer).warning(anyString(), (Throwable) anyObject());
        doAnswer(loggingAnswer).when(tracer).severe(anyString(), (Throwable) anyObject());
        doAnswer(loggingAnswer).when(tracer).severe(anyString());
        doAnswer(pa2).when(se).fireEvent(any(ActivityHandle.class), any(FireableEventType.class), anyObject(), any(Address.class), any(ReceivableService.class));
        doAnswer(pa2).when(se).fireEvent(any(ActivityHandle.class), any(FireableEventType.class), anyObject(), any(Address.class), any(ReceivableService.class), anyInt());
        doAnswer(new MockActivityAnswer()).when(se).startActivity(any(ActivityHandle.class), anyObject());
        doAnswer(new MockActivityAnswer()).when(se).startActivity(any(ActivityHandle.class), anyObject(), anyInt());
        doAnswer(new MockActivityAnswer()).when(se).startActivitySuspended(any(ActivityHandle.class), anyObject());
        doAnswer(new MockActivityAnswer()).when(se).startActivitySuspended(any(ActivityHandle.class), anyObject(), anyInt());
        doAnswer(new MockActivityAnswer()).when(se).startActivityTransacted(any(ActivityHandle.class), anyObject());
        doAnswer(new MockActivityAnswer()).when(se).startActivityTransacted(any(ActivityHandle.class), anyObject(), anyInt());
        doAnswer(loggingAnswer).when(se).endActivity(any(ActivityHandle.class));
        doAnswer(loggingAnswer).when(se).endActivityTransacted(any(ActivityHandle.class));
        doAnswer(loggingAnswer).when(se).suspendActivity(any(ActivityHandle.class));
        doAnswer(loggingAnswer).when(se).suspendActivity(any(ActivityHandle.class));

        doReturn(af).when(context).getAlarmFacility();
        doReturn(tracer).when(context).getTracer(anyString());
        doReturn(usage).when(context).getDefaultUsageParameterSet();
        doReturn(se).when(context).getSleeEndpoint();
        doReturn(stm).when(context).getSleeTransactionManager();
        doReturn(txn).when(stm).getTransaction();
        doReturn(timer).when(context).getTimer();
        doReturn(stxn).when(stm).getSleeTransaction();
        doReturn("Mock Entity " + name).when(context).getEntityName();
        doReturn(elf).when(context).getEventLookupFacility();
        doAnswer(pa3).when(elf).getFireableEventType(any(EventTypeID.class));
        doReturn(new ResourceAdaptorTypeID[]{new ResourceAdaptorTypeID("mockraname", "mockravendor", "mockraversion")}).when(context).getResourceAdaptorTypes();

        /*
         * doCallRealMethod().when(ra).getResourceAdaptorInterface(any(String.class));
         * doCallRealMethod().when(ra).raVerifyConfiguration(properties);
         * doCallRealMethod().when(ra).raConfigure(properties);
         * doCallRealMethod().when(ra).raActive();
         * doCallRealMethod().when(ra).setResourceAdaptorContext((ResourceAdaptorContext)
         * anyObject());
         * doCallRealMethod().when(ra).unsetResourceAdaptorContext();
         * doCallRealMethod().when(ra).raInactive();
         * doCallRealMethod().when(ra).raStopping();
         * doCallRealMethod().when(ra).raUnconfigure();
         * doCallRealMethod().when(ra).getActivity(any(javax.slee.resource.ActivityHandle.class));
         * doCallRealMethod().when(ra).getActivityHandle(any(java.lang.Object.class));
         *
         */
        // RA SBB INTERFACE METHODS
        /*
         * activityEnded(javax.slee.resource.ActivityHandle);
         * activityUnreferenced(javax.slee.resource.ActivityHandle);
         * queryLiveness(javax.slee.resource.ActivityHandle);
         * getActivity(javax.slee.resource.ActivityHandle);
         * getActivityHandle(java.lang.Object); getMarshaler();
         * administrativeRemove(javax.slee.resource.ActivityHandle);
         * eventProcessingFailed(javax.slee.resource.ActivityHandle,
         * eventProcessingSuccessful(javax.slee.resource.ActivityHandle,
         * eventUnreferenced(javax.slee.resource.ActivityHandle,
         * serviceActive(javax.slee.resource.ReceivableService);
         * serviceStopping(javax.slee.resource.ReceivableService);
         * serviceInactive(javax.slee.resource.ReceivableService);
         */
        checkDefaultUsageParameterSetIsAssignableFromContext();
        setup = true;
    }

    private void checkDefaultUsageParameterSetIsAssignableFromContext() {
        assertTrue(usageClass.isAssignableFrom(getContext().getDefaultUsageParameterSet().getClass()));
    }

    public void doCallRealMethods(Class clazz) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, Exception {
        MockSlee.doCallRealMethods(ra, clazz);
    }

    public static Object getAny(Class<? extends Object> c) {
        //log.info(c.toString());
        if (c.equals(Boolean.class)) {
            return anyBoolean();
        }
        if (c.equals(List.class)) {
            return anyList();
        }
        if (c.equals(String.class)) {
            return anyString();
        }
        if (c.equals(Integer.TYPE)) //log.info("****"  + c.toString());
        {
            return anyInt();
        }

        return any(c);
    }

    public void listRaMethods() {
        StringBuilder bob = new StringBuilder();
        for (Method m : ra.getClass().getMethods()) {
            if (filterMethod(m)) {
                continue;
            }
            //System.out.println("dc = " + Arrays.asList((Class[])m.getDeclaringClass().getInterfaces()));
            bob.append("doCallRealMethod().when(ra.getResourceAdaptor())." + m.getName() + "( ");
            for (Class c : m.getParameterTypes()) {
                bob.append(" any(" + c.getName() + ".class) ,");
            }
            bob.append(");").append('\n');
        }
        log.info(bob.toString().replaceAll(",\\);", ");"));
    }

    public void listSbbInterfaceMethods() {
        Class clazz = sbbInterfaceClass;
        StringBuilder bob = new StringBuilder();
        for (Method m : clazz.getMethods()) {
            if (filterMethod(m)) {
                continue;
            }
            //System.out.println("dc = " + Arrays.asList((Class[])m.getDeclaringClass().getInterfaces()));
            bob.append("doCallRealMethod().when(ra.getSbbInterface())." + m.getName() + "( ");
            for (Class c : m.getParameterTypes()) {
                bob.append(" any(" + c.getName() + ".class) ,");
            }
            bob.append(");").append('\n');
        }
        System.out.println(bob.toString().replaceAll(",\\);", ");"));
    }

    public void start() throws InvalidConfigurationException {
        //START
        ra.setResourceAdaptorContext(context);
        ra.raVerifyConfiguration(properties);
        ra.raConfigure(properties);
        ra.raActive();
        tracer.info(properties.toString());
    }

    public void stop() {
        tracer.info("waiting to stop");
        ra.raUnconfigure();
        pause(1000L);
        ra.raStopping();
        pause(1000L);
        ra.raInactive();
        pause(1000);
        ra.unsetResourceAdaptorContext();
    }

    public void pause(long l) {
        tracer.fine("ra mock pause " + l);
        try {
            Thread.sleep(l);
        } catch (InterruptedException ex) {
        }
    }

    public void configureFromProperties(String classpathLocation) throws IOException {
        URL url = this.getClass().getClassLoader().getResource(classpathLocation);
        if (url == null) {
            throw new NullPointerException(classpathLocation);
        }

        log.info("configuring from properties : " + classpathLocation);
        this.configureFromProperties(url.openStream());
    }

    public void configureFromProperties(InputStream propertiesInputStream) throws IOException {
        assertNotNull(propertiesInputStream);
        Properties properties = new Properties();
        properties.load(propertiesInputStream);
        this.configureFromProperties(properties);
    }

    public void configureFromProperties(java.util.Properties properties) {
        log.info("configuring from properties : " + properties.toString());
        Property p;
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            // log.info(key.toString() +  "=" + value);
            String name = key;//.substring(0, key.indexOf(':'));
            String type = value.substring(0, value.indexOf('='));
            value = value.substring(value.indexOf('=') + 1, value.length());
            p = new Property(name, type, Property.toObject(type, value));
            //log.fine(p.toString());
            this.properties.addProperty(p);
        }
    }

    public void addConfigProperty(String name, Class type, Object value) {
        this.properties.addProperty(new Property(name, type.getName(), value));
    }

    public void addConfigProperty(String name, Object value) {
        this.properties.addProperty(new Property(name, value.getClass().getName(), value));
    }

    public ConfigProperties getConfigProperties() {
        return properties;
    }

    public SBB getSbbInterface() {
        // verify(ra).raActive();
        assertNotNull(ra);
        assertNotNull(sbbInterfaceClass);

        if (sbbInterface != null) {
            return sbbInterface;
        }

        Object o = ra.getResourceAdaptorInterface(sbbInterfaceClass.getName());

        if (o != null) {
            sbbInterface = (SBB) o; // mock(o.getClass());
            log.info("non default ra sbbinterface");
            return sbbInterface;
        } else if (ra.getClass().isAssignableFrom(sbbInterfaceClass)) {
            log.info("default ra sbbinterface");
            sbbInterface = (SBB) ra;
            return sbbInterface;
        }

        throw new RuntimeException("Can't get that interface from the RA");

    }

    public RA getResourceAdaptor() {
        return (RA) ra;
    }

    public USAGE getDefaultUsageParameterSet() {
        return usage;
    }

    public void dumpDefaultUsageParameterSet() {
        System.out.println("Dumping usage parameter set");
        for (Entry<String, BigInteger> k : use.entrySet()) {
            System.out.println(k.getKey() + ", " + k.getValue());
        }
        for (Entry<String, List<BigInteger>> k : sample.entrySet()) {
            System.out.println(k.getKey() + ", " + getSampleStatistics(k.getValue()));
        }
    }

    public ResourceAdaptorContext getContext() {
        return context;
    }

    public EventLookupFacility getElf() {
        return elf;
    }

    public SleeEndpoint getSe() {
        return se;
    }

    public Tracer getTracer() {
        return tracer;
    }
    static Map<ActivityHandle, ActivityContextInterface> handleMap = new HashMap<ActivityHandle, ActivityContextInterface>();

    public abstract void onAnyEvent(ActivityHandle handle, FireableEventType eventType, Object object, Address address, ReceivableService service);

    public static Sbb createSbb(Class<? extends Sbb> sbbClass) throws Exception {
        Sbb sbb = mock(sbbClass);
        MockSlee.doCallRealMethods(sbb, Sbb.class);

        //TODO set context blah blah
        SbbContext sbbContext = mock(SbbContext.class);
        sbb.setSbbContext(sbbContext);

        return sbb;
    }

    public void callSbb(Sbb sbb, ActivityHandle handle, FireableEventType eventType, Object event, Address address, ReceivableService service) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//        Method onServiceStarted(
        //       javax.slee.serviceactivity.ServiceStartedEvent event, ActivityContextInterface aci)
        ActivityContextInterface aci = null;
        if (!handleMap.containsKey(handle)) {
            aci = mock(ActivityContextInterface.class);
            doReturn(ra.getActivity(handle)).when(aci).getActivity();
            handleMap.put(handle, aci);

        } else {
            aci = handleMap.get(handle);
        }

        EventContext ec = mock(EventContext.class);
        doReturn(address).when(ec).getAddress();

        for (Method m : sbb.getClass().getMethods()) {
            Class[] p = m.getParameterTypes();
            if (p[0].equals(event.getClass()) && p[1].equals(ActivityContextInterface.class)) {
                slog.info("invoking " + m.toGenericString());
                if (p.length == 2) {
                    m.invoke(sbb, new Object[]{event, aci});
                }
                if (p.length == 3) {
                    m.invoke(sbb, new Object[]{event, aci, ec});
                }
            }
        }
    }

    private boolean filterMethod(Method m) {
        List<Method> mf = Arrays.asList((Method[]) org.mockito.cglib.proxy.Factory.class.getMethods());
        List<Method> ra = Arrays.asList((Method[]) ResourceAdaptor.class.getMethods());

        return (m.getDeclaringClass().equals(javax.slee.resource.ResourceAdaptor.class)
                || ra.contains(m)
                || mf.contains(m)
                || m.getDeclaringClass().equals(Object.class))
                || (!Modifier.isPublic(m.getModifiers()) && !Modifier.isProtected(m.getModifiers()));
    }

    private SampleStatistics getSampleStatistics(List<BigInteger> l) {
        SampleStatistics ss = new SampleStatistics(l.size(), min(l), max(l), mean(l));
        return ss;
    }

    private long min(List<BigInteger> l) {
        BigInteger min = l.get(0);
        for (BigInteger bi : l) {
            min = bi.min(min);
        }
        return min.longValue();
    }

    private long max(List<BigInteger> l) {
        BigInteger max = l.get(0);
        for (BigInteger bi : l) {
            max = bi.max(max);
        }
        return max.longValue();
    }

    private double mean(List<BigInteger> l) {
        BigInteger mean = BigInteger.ZERO;
        for (BigInteger bi : l) {
            mean.add(bi);
        }
        return mean.doubleValue() / (double) l.size();
    }

    public static void configureLogging() throws IOException {
        configureLogging(MockResourceAdaptor.class.getClassLoader());
    }

    public static void configureLogging(ClassLoader loader) throws IOException {
        InputStream is = loader.getResourceAsStream("logging.properties");

        if (is != null) {
            LogManager.getLogManager().reset();
            LogManager.getLogManager().readConfiguration(is);
            System.err.println("Logging Initialized");
        } else {
            System.err.println("Logging NOT Initialized");
        }

        is = loader.getResourceAsStream("log4j.properties");

        if (is != null) {
            Properties properties = new Properties();
            properties.load(is);
            org.apache.log4j.PropertyConfigurator.configure(properties);
            System.err.println("Log4j Initialized");
        } else {
            System.err.println("Log4j NOT Initialized");
        }
    }

    public void setEntityName(String string) {
        this.name = string;
        doReturn("Mock Entity " + name).when(context).getEntityName();
    }

    private static Method getMethodFromTarget(Object o, Method m2) throws Exception {
        for (Method m : o.getClass().getMethods()) {
            if (m.getName().equals(m2.getName())
                    && m2.getParameterTypes().length == m.getParameterTypes().length) {
                return m;
            }
        }

        throw new Exception("can't find method " + m2 + " in " + o);
    }

    /*
    public void waitForEntityLifeCycle(ResourceAdaptorEntityLifecycle resourceAdaptorEntityLifecycle) {
        tracer.info("waiting for " + resourceAdaptorEntityLifecycle.name());

        while (((AbstractResourceAdaptor) ra).getResourceAdaptorEntityLifecycle() != resourceAdaptorEntityLifecycle) {
            pause(500l);
        }
        slog.info("ra entity " + resourceAdaptorEntityLifecycle.name());
    }
    * 
     */
    public ACI getActivityContextInterfaceFactory() {
        if (acif == null) {
            acif = (ACI) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{aciClass}, new InvocationHandler() {

                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (args == null) {
                        return null;
                    }
                    log.info("get aci " + args[0]);
                    ActivityHandle h = ra.getActivityHandle(args[0]);//aciMap.get(args[0]);
                    if (h == null) {
                        throw new UnrecognizedActivityException(args[0]);
                    }
                    ActivityContextInterface r = ahMap.get(h);
                    if (r == null) {
                        throw new UnrecognizedActivityException(args[0]);
                    }
                    return r;
                }
            });
        }
        return acif;

    }

    public void waitForEntityLifeCycle(String lifecycle) {
        //TODO:
    }

    private class LoggingAnswer implements Answer {

        public LoggingAnswer() {
        }

        public Object answer(InvocationOnMock invocation) throws Throwable {
            log.info(invocation.getMethod().getName() + " " + Arrays.asList(invocation.getArguments()).toString());
            for (Object o : invocation.getArguments()) {
                if (o instanceof Throwable) {
                    ((Throwable) o).printStackTrace();
                }
            }
            return null;
        }
    }

    private class MockActivityAnswer extends LoggingAnswer {

        public MockActivityAnswer() {
        }

        @Override
        public Object answer(final InvocationOnMock invocation) throws Throwable {
            super.answer(invocation);
            ActivityContextInterface aci = mock(ActivityContextInterface.class);
            doAnswer(new Answer() {

                public Object answer(InvocationOnMock invocation2) throws Throwable {
                    return ra.getActivity((ActivityHandle) invocation.getArguments()[0]);
                }
            }).when(aci).getActivity();
            //(ActivityContextInterface) acif.getClass().getMethod("getActivityContextInterface", invocation.getArguments()[1].getClass()).invoke(acif, invocation.getArguments()[1]);
            aciMap.put(invocation.getArguments()[1], (ActivityHandle) invocation.getArguments()[0]);
            ahMap.put((ActivityHandle) invocation.getArguments()[0], aci);
            return null;
        }
    }
}
