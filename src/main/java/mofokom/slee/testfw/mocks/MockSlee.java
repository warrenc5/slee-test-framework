package mofokom.slee.testfw.mocks;

import static java.lang.Boolean.TRUE;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.EventContext;
import javax.slee.Sbb;
import javax.slee.SbbLocalObject;
import javax.slee.annotation.event.EventHandler;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.TimerFacility;
import javax.slee.nullactivity.NullActivity;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileTableActivityContextInterfaceFactory;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.ReceivableService;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;
import lombok.extern.slf4j.Slf4j;
import mofokom.slee.testfw.NameVendorVersion;
import mofokom.slee.testfw.NewInitialContextFactory;
import static mofokom.slee.testfw.mocks.MockResourceAdaptor.getAny;
import mofokom.slee.testfw.resource.AnyEventHandler;
import mofokom.slee.testfw.service.ServiceLifecycle;
import org.apache.log4j.ConsoleAppender;
import org.mockito.Mockito;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@Slf4j
public class MockSlee {

    private static MockSlee instance;

    private static Level commonsLevel(String level) {
        String newLevel = level;
        switch (level) {
            case "TRACE":
                newLevel = "FINEST";
                break;
            case "DEBUG":
                newLevel = "FINE";
                break;
            case "WARNING":
                newLevel = "SEVERE";
                break;
            default:
        }
        return Level.parse(newLevel);
    }

    private Set<MockSbb> sbbs = new HashSet<>();
    public Map<String, NameVendorVersion> sbbAlias = new HashMap<>();
    private Set<MockResourceAdaptor> ras = new HashSet<>();
    private Set<MockEvent> events = new HashSet<>();
    private Set<MockProfile> profiles = new HashSet<>();
    private Map<Object, ActivityContextInterface> acis = new HashMap<>();
    public Map<ActivityHandle, ActivityContextInterface> activities = new HashMap<>();
    public Map<NameVendorVersion, MockSleeComponent> components = new HashMap<>();

    private static final Logger slog = Logger.getAnonymousLogger();
    private AlarmFacility alarmFacility;
    private TimerFacility timerFacility;
    private ServiceActivityFactory serviceActivityFactory;
    private ServiceActivityContextInterfaceFactory serviceActivityACIFactory;
    private ProfileTableActivityContextInterfaceFactory profileTableACIFactory;
    private ProfileFacility profileFacility;
    private NullActivityFactory nullActivityFactory;
    private ActivityContextNamingFacility acNamingFacility;
    private NullActivityContextInterfaceFactory nullAciFactory;

    private InitialContext ic;
    private ServiceLifecycle state;

    public Map<ActivityHandle, Object> activityMap = new HashMap();
    public Map<ActivityHandle, Set<SbbLocalObject>> sbbsA = new HashMap();

    static {
        if (instance == null) {
            try {
                instance = new MockSlee();
            } catch (NamingException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    public static MockSlee getInstance() {
        return instance;
    }

    private MockSlee() throws NamingException {
        System.setProperty("java.naming.factory.initial", NewInitialContextFactory.class.getName());
        ic = new InitialContext();
    }

    public void setupJNDI() throws NamingException {

        this.alarmFacility = mock(AlarmFacility.class);
        this.nullAciFactory = mock(NullActivityContextInterfaceFactory.class);
        this.acNamingFacility = mock(ActivityContextNamingFacility.class);
        this.nullActivityFactory = mock(NullActivityFactory.class);
        this.profileFacility = mock(ProfileFacility.class);
        this.profileTableACIFactory = mock(ProfileTableActivityContextInterfaceFactory.class);
        this.serviceActivityACIFactory = mock(ServiceActivityContextInterfaceFactory.class);
        this.serviceActivityFactory = mock(ServiceActivityFactory.class);
        this.timerFacility = mock(TimerFacility.class);

        ic.bind(AlarmFacility.JNDI_NAME, this.alarmFacility);
        ic.bind(NullActivityContextInterfaceFactory.JNDI_NAME, this.nullAciFactory);
        ic.bind(ActivityContextNamingFacility.JNDI_NAME, this.acNamingFacility);
        ic.bind(NullActivityFactory.JNDI_NAME, this.nullActivityFactory);
        ic.bind(ProfileFacility.JNDI_NAME, this.profileFacility);
        ic.bind(ProfileTableActivityContextInterfaceFactory.JNDI_NAME, this.profileTableACIFactory);
        ic.bind(ServiceActivityContextInterfaceFactory.JNDI_NAME, this.serviceActivityACIFactory);
        ic.bind(ServiceActivityFactory.JNDI_NAME, this.serviceActivityFactory);
        ic.bind(TimerFacility.JNDI_NAME, this.timerFacility);

        doAnswer((iom) -> createNullActivityContextInterface()).when(this.nullActivityFactory).createNullActivity();

    }

    private static void doAnswerCmp(Answer answer, Object cmpIface, Class<?> cmpInterface) {
    }

    //TODO: sbb handler methods
    public static void doCallRealMethods(Object target, Class... clazzz) throws Exception {
        doCallRealMethods(target, new Pattern[]{}, clazzz);
    }

    public static void doCallRealMethods(Object target, Pattern[] patterns, Class... clazzz) throws Exception {

        Arrays.asList(clazzz).forEach(clazz -> {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().startsWith("ajc$")) {
                    continue;
                }
                if (Arrays.stream(patterns).anyMatch(p -> p.matcher(m.getName()).matches())) {

                } else if (!clazz.equals(m.getDeclaringClass())) {
                    slog.log(Level.WARNING, m.getDeclaringClass() + " " + m.toString() + " not assignable from class " + clazz.getName());

                    continue;
                } else if (m.getName().contains("access$") || (!clazz.isInterface() && Modifier.isAbstract(m.getModifiers())) || Modifier.isPrivate(m.getModifiers()) || Modifier.isFinal(m.getModifiers()) || m.getModifiers() == 0) {
                    continue;
                }
                if (Modifier.isProtected(m.getModifiers())) {
                    continue;
                }
                doCallRealMethod(target, m);
            }
        });

    }

    public static void doCallRealMethod(Object target, Method m) {

        Object o = null;

        if (o == null) {
            o = Mockito.doCallRealMethod().when(target);
        }

        doDangling(o, target, m);
    }

    public static void doDangling(Object o, Object target, Method m) {

        Object[] args = new Object[m.getParameterTypes().length];
        int p = 0;

        for (Class c : m.getParameterTypes()) {
            args[p++] = getAny(c);
        }
        slog.info("calling real " + m.toString());
        try {
            m.invoke(o, args);
            o = null;
        } catch (RuntimeException x) {
            slog.log(Level.WARNING, target.getClass() + " " + o.getClass() + " " + m.getDeclaringClass() + " " + m.toString() + " " + x.getClass() + " " + x.getMessage());
        } catch (Exception x) {
            slog.log(Level.WARNING, target.getClass() + " " + o.getClass() + " " + m.getDeclaringClass() + " " + m.toString() + " " + x.getClass() + " " + x.getMessage());
        }

    }

    public Object mockEvent(NameVendorVersion nvv, Class aClass) {
        MockEvent me = null;
        this.events.add(me = new MockEvent(nvv, aClass));
        this.components.put(me.getNvv(), me);
        return me;
    }

    public static void resolveResourceAdaptorTypeRef() {
        //for each sbb ratr 
        //find the ra
        //bind the sbb aci name to the entity link
        /**
         *
         *
         * mockSbb.bind("MEGACO_ACI_FACTORY_NAME","slee/resources/megaco/activitycontextinterfacefactory");
         * mockSbb.bind("MEGACO_SBB_RA_INTERFACE_NAME","slee/resources/megaco/sbbrainterface");
         * mockSbb.bind("slee/resources/megaco/activitycontextinterfacefactory",
         * mockRa.getActivityContextInterfaceFactory());
         * mockSbb.bind("slee/resources/megaco/sbbrainterface",mockRa.getSbbInterface());
         */
    }

    public void add(MockResourceAdaptor ra) throws Exception {
        this.ras.add(ra);
        this.components.put(ra.getNvv(), ra);
    }

    public <A extends Sbb, B extends SbbLocalObject, C> A add(MockSbb<A, B, C> sbbHome) throws Exception {
        MockitoAnnotations.initMocks(sbbHome.getSbb());
        this.sbbs.add(sbbHome);
        this.components.put(sbbHome.getNvv(), sbbHome);
        sbbHome.init();
        doAnswer(ic -> {
            slog.info("live activties " + this.activities.toString());
            slog.info("bound activties " + this.sbbsA.toString());
            Set<ActivityContextInterface> set = this.sbbsA.entrySet().stream().filter(
                    e -> e.getValue().contains(sbbHome.getSbbLocalObject())
            ).map(
                    e -> this.activities.get(e.getKey())
            ).collect(Collectors.toSet());

            slog.info(sbbHome.getSbbLocalObject() + " bound to " + set.toString());
            return set.toArray(new ActivityContextInterface[set.size()]);
        }).when(sbbHome.sbbContext).getActivities();

        Class clazz = sbbHome.getSbbClass();
        Arrays.asList(clazz.getMethods()).stream()
                .filter(m
                        -> m.isAnnotationPresent(javax.slee.annotation.event.EventHandler.class))
                .forEach(m -> {
                    EventHandler a = m.getAnnotation(javax.slee.annotation.event.EventHandler.class);
            sbbHome.eventHandlers.put(NameVendorVersion.from(a.eventType()), m);
                });

        return sbbHome.getSbb();
    }

    public MockSleeComponent add(Class clazz) {
        return this.add(clazz, null);
    }

    public MockSleeComponent add(Class clazz, AnyEventHandler handler) {
        Annotation[] annotations = clazz.getAnnotations();
        List<Annotation> ann = Arrays.asList(annotations);

        for (Annotation a : ann) {

            if (a.annotationType().equals(javax.slee.annotation.ResourceAdaptor.class)) {
                javax.slee.annotation.ResourceAdaptor raA = (javax.slee.annotation.ResourceAdaptor) a;
                NameVendorVersion nvv = NameVendorVersion.builder().name(raA.name()).vendor(raA.vendor()).version(raA.version()).build();
                return ann.stream().filter(aa -> aa.annotationType().equals(javax.slee.annotation.ResourceAdaptorType.class)).map(aa -> {
                    javax.slee.annotation.ResourceAdaptorType raTA = (javax.slee.annotation.ResourceAdaptorType) aa;
                    MockResourceAdaptor mRa = null;
                    try {

                        mRa = this.createResourceAdaptorEntity(nvv, clazz, raTA.raInterface(), raA.usageParametersInterface(), raTA.aciFactory(), handler);
                        this.add(mRa);
                        return mRa;
                    } catch (Exception ex) {
                        Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return null;
                }).findFirst().get();
            } else if (a.annotationType().equals(javax.slee.annotation.event.EventType.class)) {

                javax.slee.annotation.event.EventType eA = (javax.slee.annotation.event.EventType) a;
                NameVendorVersion nvv = NameVendorVersion.builder().name(eA.name()).vendor(eA.vendor()).version(eA.version()).build();
                MockEvent mE = new MockEvent(nvv, clazz);
                this.events.add(mE);
                return mE;
            } else if (a.annotationType().equals(javax.slee.annotation.Sbb.class)) {
                javax.slee.annotation.Sbb sbbA = (javax.slee.annotation.Sbb) a;
                NameVendorVersion nvv = NameVendorVersion.builder().name(sbbA.name()).vendor(sbbA.vendor()).version(sbbA.version()).build();
                try {
                    final MockSbb mSbb = new MockSbb(nvv, clazz, sbbA.localInterface(), sbbA.usageParametersInterface());
                    MockSlee.getInstance().sbbAlias.put(sbbA.alias() == null ? nvv.toString() : sbbA.alias(), nvv);
                    Arrays.stream(clazz.getFields()).forEach(f -> {
                        javax.slee.annotation.CMPField cmp = f.getAnnotation(javax.slee.annotation.CMPField.class);
                        if (cmp != null) {
                            mSbb.addCmp(f);
                        }
                    });

                    Arrays.stream(clazz.getMethods()).forEach(m -> {
                        javax.slee.annotation.CMPField cmp = m.getAnnotation(javax.slee.annotation.CMPField.class);
                        if (cmp != null) {
                            mSbb.addCmp(m);
                        }
                    });

                    Arrays.stream(clazz.getFields()).forEach(f -> {
                        javax.slee.annotation.ChildRelation cr = f.getAnnotation(javax.slee.annotation.ChildRelation.class);
                        if (cr != null) {
                            try {
                                mSbb.addChildRelation(cr.sbbAliasRef(), cr.defaultPriority(), f);
                            } catch (Exception ex) {
                                Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });

                    Arrays.stream(clazz.getMethods()).forEach(m -> {
                        javax.slee.annotation.ChildRelation cr = m.getAnnotation(javax.slee.annotation.ChildRelation.class);
                        if (cr != null) {
                            try {
                                mSbb.addChildRelation(cr.sbbAliasRef(), cr.defaultPriority(), m);
                            } catch (Exception ex) {
                                Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });

                    this.add(mSbb);
                    return mSbb;
                } catch (Exception ex) {
                    Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (a.annotationType().equals(javax.slee.annotation.ProfileSpec.class)) {

                javax.slee.annotation.ProfileSpec pA = (javax.slee.annotation.ProfileSpec) a;
                NameVendorVersion nvv = NameVendorVersion.builder().name(pA.name()).vendor(pA.vendor()).version(pA.version()).build();
                MockProfile mP = new MockProfile(nvv, pA.abstractClass(), pA.cmpInterface(), pA.localInterface(), pA.tableInterface(), pA.managementInterface());
                this.profiles.add(mP);
                MockSlee.doAnswerCmp(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                }, mP.getCmpIface(), pA.cmpInterface());

                return mP;
            } else if (a.annotationType().equals(javax.slee.annotation.UsageParameter.class)) {
            }
        }
        return null;
    }

    public void start() throws NamingException {
        log.info("starting slee");
        this.state = ServiceLifecycle.STARTING;
        this.setupJNDI();
        for (MockResourceAdaptor ra : ras) {
            try {
                log.info("starting " + ra.nvv.toString());
                ra.start();
                log.info("started " + ra.nvv.toString());
            } catch (InvalidConfigurationException ex) {
                Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.state = ServiceLifecycle.ACTIVE;
        log.info("started slee");
    }

    public boolean isStarted() {
        return this.state == ServiceLifecycle.ACTIVE;
    }

    public void stop() {
        this.state = ServiceLifecycle.INACTIVE;
        for (MockResourceAdaptor ra : ras) {
            ra.stop();
        }
    }

    public ActivityContextInterface createNullActivityContextInterface() {
        NullActivity mock = mock(NullActivity.class);
        return mock(ActivityContextInterface.class);
    }

    public ActivityContextInterface mockACI() {
        return mockACI(null); // TODO use NullAci
    }

    public ActivityContextInterface mockACI(Object activity) {
        ActivityHandle handle = new ActivityHandle() {
        };
        return mockACI(handle, activity);
    }

    public ActivityContextInterface mockACI(ActivityHandle handle, Object activity) {

        sbbsA.putIfAbsent(handle, new HashSet());
        if (activity != null) {
            this.activityMap.put(handle, activity);
        }
        MockActivityContextInterface mockACI = spy(new MockActivityContextInterface(this, handle));
        activities.put(handle, mockACI);

        return mockACI;
    }

    public Set<MockSbb> getSbbs() {
        return sbbs;
    }

    public Set<MockResourceAdaptor> getRas() {
        return ras;
    }

    public Set<MockEvent> getEvents() {
        return events;
    }

    public Set<MockProfile> getProfiles() {
        return profiles;
    }

    public Map<Object, ActivityContextInterface> getAcis() {
        return acis;
    }

    public Map<ActivityHandle, ActivityContextInterface> getActivities() {
        return activities;
    }

    public AlarmFacility getAlarmFacility() {
        return alarmFacility;
    }

    public TimerFacility getTimerFacility() {
        return timerFacility;
    }

    public ServiceActivityFactory getServiceActivityFactory() {
        return serviceActivityFactory;
    }

    public ServiceActivityContextInterfaceFactory getServiceActivityACIFactory() {
        return serviceActivityACIFactory;
    }

    public ProfileTableActivityContextInterfaceFactory getProfileTableACIFactory() {
        return profileTableACIFactory;
    }

    public ProfileFacility getProfileFacility() {
        return profileFacility;
    }

    public NullActivityFactory getNullActivityFactory() {
        return nullActivityFactory;
    }

    public ActivityContextNamingFacility getAcNamingFacility() {
        return acNamingFacility;
    }

    public NullActivityContextInterfaceFactory getNullAciFactory() {
        return nullAciFactory;
    }

    public InitialContext getIc() {
        return ic;
    }

    public ServiceLifecycle getState() {
        return state;
    }

    public Map<ActivityHandle, Object> getActivityMap() {
        return activityMap;
    }

    public Map<ActivityHandle, Set<SbbLocalObject>> getSbbsA() {
        return sbbsA;
    }

    public MockResourceAdaptor createResourceAdaptorEntity(NameVendorVersion nvv, Class clazz, Class<?> raInterface, Class<?> usageParametersInterface, Class<?> aciFactory) throws Exception {
        return this.createResourceAdaptorEntity(nvv, clazz, raInterface, usageParametersInterface, aciFactory, null);
    }

    public MockResourceAdaptor createResourceAdaptorEntity(NameVendorVersion nvv, Class clazz, Class<?> raInterface, Class<?> usageParametersInterface, Class<?> aciFactory, AnyEventHandler handler) throws Exception {
        return new MockResourceAdaptor(nvv, clazz, raInterface, usageParametersInterface, aciFactory) {
            @Override
            public void onAnyEvent(ActivityHandle handle, FireableEventType eventType, ActivityContextInterface aci, EventContext ec, MockResourceAdaptor ra) {
                if (handler != null) {
                    handler.onAnyEvent(handle, eventType, aci, ec, ra);
                } else {
                    try {
                        MockSlee.this.deliverEvent(eventType, aci, ec);
                    } catch (MultiException ex) {
                        Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (UnhandledEventException ex) {
                        Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
    }

    public static Object getField(Object o, String name) {

        if (o instanceof org.mockito.cglib.proxy.Factory) {
            try {
                return Whitebox.getInternalState(o, name);
            } catch (Exception ex) {
                log.info(o.getClass() + " " + name + " " + ex.getMessage(), ex);
            }
        } else {
            try {
                Field f = o.getClass().getField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (Exception ex) {
                log.info(o.getClass() + " " + name + " " + ex.getMessage(), ex);

            }
        }
        return null;
    }

    public static void setField(Object o, String name, Object value) {

        try {
            Whitebox.setInternalState(o, name, value);
        } catch (Exception ex) {
            log.info(o.getClass() + " " + name + " " + ex.getMessage(), ex);
        }
    }

    public static void setLogger(String name, String level) {
        Logger logger = LogManager.getLogManager().getLogger(name);
        if (logger != null) {
            logger.setLevel(commonsLevel(level));
        }

        org.apache.log4j.Logger logger2 = org.apache.log4j.Logger.getLogger(name);
        if (logger2 != null) {
            logger2.setLevel(org.apache.log4j.Level.toLevel(level));

            for (Enumeration e = org.apache.log4j.Logger.getRootLogger().getAllAppenders(); e.hasMoreElements();) {
                Object a = e.nextElement();
                if (a instanceof ConsoleAppender) {
                    ConsoleAppender appender = (org.apache.log4j.ConsoleAppender) a;
                    if (appender != null) {
                        appender.setThreshold(org.apache.log4j.Level.toLevel(level));
                    }
                }
            }

        }
    }

    public void deliverEvent(FireableEventType eventType, ActivityContextInterface aci, EventContext ec) throws MultiException, UnhandledEventException {
        final List<Throwable> e = new ArrayList<>();
        final List<Boolean> h = new ArrayList<>();

        this.getSbbs().forEach(sbb -> {
            try {
                boolean handled = deliverEvent(sbb, eventType, aci, ec);
                h.add(handled);

                //TODO sort by priority
                sbb.childRelation.values().stream().forEach(cr -> {
                    ((MockChildRelation) cr).mock.forEach(childSbb -> {
                        try {
                            boolean handled2 = deliverEvent(((MockChildRelation) childSbb).sbb, eventType, aci, ec);
                            h.add(handled2);
                        } catch (IllegalAccessException ex) {
                            e.add(ex.getCause());
                        } catch (InvocationTargetException ex) {
                            e.add(ex.getCause());
                        }
                    });
                });
            } catch (IllegalAccessException ex) {
                e.add(ex.getCause());
            } catch (InvocationTargetException ex) {
                e.add(ex.getCause());
            }
        });

        for (Throwable t : e) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
        if (!e.isEmpty()) {
            throw new MultiException(e);
        }

        if (!h.contains(TRUE)) {
            throw new UnhandledEventException(eventType);
        }

    }

    public boolean deliverEvent(MockSbb mockSbb, FireableEventType eventType, ActivityContextInterface aci, EventContext ec) throws IllegalAccessException, InvocationTargetException {

        Sbb sbb = mockSbb.getSbb();
        Class clazz = mockSbb.getSbbClass();
        Annotation[] annotations = clazz.getAnnotations();

        NameVendorVersion tNvv = NameVendorVersion.from(eventType.getEventType());

        Optional<Method> hm = Arrays.asList(clazz.getMethods()).stream()
                .filter(m -> m.isAnnotationPresent(javax.slee.annotation.event.EventHandler.class))
                .filter(m -> {
                    Annotation a = m.getAnnotation(javax.slee.annotation.event.EventHandler.class);
                    javax.slee.annotation.event.EventHandler eA = (javax.slee.annotation.event.EventHandler) a;

                    NameVendorVersion nvv = NameVendorVersion.builder().name(eA.eventType().name()).vendor(eA.eventType().vendor()).version(eA.eventType().version()).build();

                    return nvv.equals(tNvv);
                })
                .findFirst();

        if (hm.isPresent()) {
            log.debug("handled " + tNvv + " on " + clazz);
            this.callSbb(sbb, hm.get(), eventType, aci, ec);
            return true;
        } else {
            log.debug("no handler for " + tNvv + " on " + clazz);
            return false;
        }
    }

    public void callSbb(Sbb sbb, Method m, FireableEventType eventType, ActivityContextInterface aci, EventContext ec) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Object event = ec.getEvent();
        Class[] p = m.getParameterTypes();
        if (p[0].isAssignableFrom(event.getClass()) && p[1].equals(ActivityContextInterface.class)) {
            slog.info("invoking " + m.toGenericString());
            if (p.length == 2) {
                m.invoke(sbb, new Object[]{event, aci});
            } else if (p.length == 3) {
                m.invoke(sbb, new Object[]{event, aci, ec});
            } else {
                throw new IllegalArgumentException(sbb.getClass().getSimpleName() + " " + m.getName() + " should have 2 or 3 parameters it has" + p.length);
            }
        }
    }

//        Method onServiceStarted(
    //       javax.slee.serviceactivity.ServiceStartedEvent event, ActivityContextInterface aci)
    public static EventContext createEventContext(ActivityContextInterface aci, Object event, Address address, ReceivableService service) {

        EventContext ec = mock(EventContext.class);
        doReturn(aci).when(ec).getActivityContextInterface();
        doReturn(event).when(ec).getEvent();
        doReturn(address).when(ec).getAddress();
        if (service != null) {
            doReturn(service.getService()).when(ec).getService();
        }

        return ec;
    }

    public void callSbb(Sbb sbb, ActivityHandle handle, FireableEventType eventType, Object event, Address address, ReceivableService service, ActivityContextInterface aci, EventContext ec) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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

}
