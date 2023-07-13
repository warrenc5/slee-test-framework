package mofokom.slee.testfw.mocks;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import javax.slee.EventTypeID;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.ServiceID;
import javax.slee.SbbLocalObject;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.AlarmLevel;
import javax.slee.facilities.TimerFacility;
import javax.slee.management.Alarm;
import javax.slee.management.NotificationSource;
import javax.slee.nullactivity.NullActivity;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileTableActivityContextInterfaceFactory;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;
import javax.slee.serviceactivity.ServiceStartedEvent;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import lombok.extern.slf4j.Slf4j;
import mobi.mofokom.javax.slee.annotation.Service;
import mobi.mofokom.javax.slee.annotation.event.ServiceStartedEventHandler;
import mofokom.slee.testfw.NameVendorVersion;
import mofokom.slee.testfw.NewInitialContextFactory;
import mofokom.slee.testfw.SleeLifecycle;
import static mofokom.slee.testfw.mocks.MockResourceAdaptor.getAny;
import mofokom.slee.testfw.resource.AnyEventHandler;
import mofokom.slee.testfw.service.ServiceLifecycle;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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

    public ExecutorService sleeExecutor;

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

    public static MockSlee getNewInstance() {
        try {
            instance = new MockSlee();
        } catch (NamingException ex) {
            log.error(ex.getMessage(), ex);
        }
        return instance;
    }

    private Set<MockSbb> sbbs = new HashSet<>();
    public Map<String, NameVendorVersion> sbbAlias = new HashMap<>();
    private Set<MockResourceAdaptor> ras = new HashSet<>();
    private Set<MockEvent> events = new HashSet<>();
    private Set<MockProfile> profiles = new HashSet<>();
    private Map<Object, ActivityContextInterface> acis = new HashMap<>();
    public Map<ActivityHandle, ActivityContextInterface> activities = new HashMap<>();
    public Map<NameVendorVersion, MockSleeComponent> components = new HashMap<>();

    private static Logger slog;
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
    private SleeLifecycle state;

    public Map<ActivityHandle, Object> activityMap = new HashMap();
    public Map<ActivityHandle, Set<SbbLocalObject>> sbbsA = new HashMap();

    MockSleeTransactionManager txMgr = new MockSleeTransactionManager();

    static {

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MockSlee.class);
        try {
            configureLogging();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);

        }

        slog = Logger.getLogger(MockSlee.class.getName());

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
    private HashMap<String, ? super ResourceAdaptor> links;
    private HashMap<String, Map<String, Alarm>> alarms;
    private Map<ServiceID, Class<Sbb>> services = new HashMap<>();
    private Map<ServiceID, ServiceLifecycle> serviceState = new HashMap<>();

    private MockSlee() throws NamingException {
        System.setProperty("java.naming.factory.initial", NewInitialContextFactory.class.getName());
        this.sleeExecutor = Executors.newSingleThreadExecutor(); //SLEE Executor
        ic = new InitialContext();
        this.links = new HashMap<>();
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
            org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger();
            System.err.println("Log4j Initialized");
        } else {
            System.err.println("Log4j NOT Initialized");
        }
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

        alarms = new HashMap<>();
        doAnswer(ic -> {

            Alarm alarm = createAlarm(ic);
            Map<String, Alarm> alarmType = this.alarms.getOrDefault(ic.getArgumentAt(1, String.class), new HashMap<>());
            alarmType.merge(ic.getArgumentAt(1, String.class), alarm, (k, v) -> {
                return alarm;
            });
            return alarm.getAlarmID();
        }).when(this.alarmFacility).raiseAlarm(anyString(), anyString(), any(AlarmLevel.class), anyString());

        doAnswer(ic -> {
            this.alarms.clear();
            return null;
        }).when(this.alarmFacility).clearAlarms();

        doAnswer(ic -> {
            Map<String, Alarm> alarmType = this.alarms.remove(ic.getArgumentAt(0, String.class));
            return null;
        }).when(this.alarmFacility).clearAlarms(anyString());

        doAnswer(ic -> {
            this.alarms.values().forEach(a -> {
                a.remove(ic.getArgumentAt(0, String.class));
            });
            return null;
        }).when(this.alarmFacility).clearAlarm(anyString());
    }

    private static void doAnswerCmp(Answer answer, Object cmpIface, Class<?> cmpInterface) {
        //WTF?
    }

    //TODO: sbb handler methods
    public static void doCallRealMethods(MockSleeComponent target) throws Exception {

        if (target == null) {
            throw new NullPointerException("target cannot be null");
        }

        if (target instanceof MockSbb) {
            doCallRealMethods(((MockSbb) target).getSbb(), ((MockSbb) target).getSbbClass(), ((MockSbb) target).getSbbLocalObjectClass());
        } else {
            throw new UnsupportedOperationException(target.getClass().toString());
        }
    }

    public static void doCallRealMethods(Object target, Class... clazzz) throws Exception {
        doCallRealMethods(target, new Pattern[]{}, clazzz);
    }

    public static void doCallRealMethods(Object target, Pattern[] patterns, Class... clazzz) throws Exception {

        final List<String> mNames = new ArrayList<>();

        Arrays.asList(clazzz).forEach((clazz) -> {
            if (clazz == null) {
                return;
            }

            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().startsWith("ajc$")) {
                    continue;
                }
                if (Arrays.stream(patterns).anyMatch(p -> p.matcher(m.getName()).matches())) {

                } else if (!clazz.equals(m.getDeclaringClass())) {
                    slog.log(Level.WARNING, m.getDeclaringClass() + " " + m.toString() + " not assignable from class " + clazz.getName());

                    continue;
                } else if (!clazz.isInterface() && Modifier.isAbstract(m.getModifiers())) {
                    log.warn("abstract interface method " + m.toString() + " not being called");
                    continue;

                } else if (m.getName().contains("access$") || Modifier.isPrivate(m.getModifiers()) || Modifier.isFinal(m.getModifiers()) || m.getModifiers() == 0) {
                    log.warn("modifier " + m.toString() + " not being called");
                    continue;
                }

                if (Modifier.isProtected(m.getModifiers())) {
                    continue;
                }

                try {
                    doCallRealMethod(target, m);
                    mNames.add(m.getName());
                } catch (org.mockito.exceptions.misusing.UnfinishedStubbingException x) {
                    log.warn(target.getClass() + "matcher failed " + m.getDeclaringClass().getName() + ":" + m.getName());
                } catch (InvocationTargetException | IllegalAccessException ex) {
                    log.warn(m.getDeclaringClass().getName() + ":" + m.getName() + " " + ex.getMessage());
                }
            }
        });
        log.info(String.format("clazzz %1$s, methods %2$s", Arrays.asList(clazzz).toString(), mNames.toString()));

    }

    public static void doCallRealMethod(Object target, Method m) throws InvocationTargetException, IllegalAccessException {

        Object o = null;

        m.setAccessible(true);
        try {
            if (o == null) {
                o = Mockito.doCallRealMethod().when(target);
            }

            doDangling(o, target, m);
        } catch (org.mockito.exceptions.misusing.InvalidUseOfMatchersException xI) {
            log.warn(target + " " + m.toString() + " " + xI.getMessage());
        }
    }

    public static void doDangling(Object o, Object target, Method m) throws IllegalAccessException, InvocationTargetException {

        Object[] args = new Object[m.getParameterTypes().length];
        int p = 0;

        for (Class c : m.getParameterTypes()) {
            args[p++] = getAny(c);
        }

        slog.fine("completed dangling " + m.toString());
        try {
            m.setAccessible(true);
            m.invoke(o, args);
            o = null;
        } catch (RuntimeException x) {
            slog.log(Level.WARNING, target.getClass() + " " + o.getClass() + " " + m.getDeclaringClass() + " " + m.toString() + " " + x.getClass() + " " + x.getMessage());
            throw x;
        } catch (IllegalAccessException | InvocationTargetException x) {
            slog.log(Level.WARNING, target.getClass() + " " + o.getClass() + " " + m.getDeclaringClass() + " " + m.toString() + " " + x.getClass() + " " + x.getMessage());
            throw x;
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

    //TODO do this for each Sbb RAType binding
    public <RA extends ResourceAdaptor, PROV, UP, ACIF> void add(MockResourceAdaptor<RA, PROV, UP, ACIF> ra, String aciFactoryName, String raEntityLink, String sbbRaInterface) {

        log.info(sbbRaInterface + " " + ra.getSbbInterface());
        try {
            this.ic.bind(aciFactoryName, ra.getActivityContextInterfaceFactory());
            this.ic.bind(sbbRaInterface, ra.getSbbInterface());
        } catch (NamingException ex) {
            log.error(ex.getMessage(), ex);
        }
        this.links.put(raEntityLink, ra.getResourceAdaptor());
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
            ).map(e -> this.activities.get(e.getKey())
            ).collect(Collectors.toSet());

            slog.info(sbbHome.getSbbLocalObject() + " bound to " + set.toString());
            return set.toArray(new ActivityContextInterface[set.size()]);
        }).when(sbbHome.sbbContext).getActivities();

        Class clazz = sbbHome.getSbbClass();

        if (clazz.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.Service.class)) {
            Service a = ((Service) clazz.getAnnotation(mobi.mofokom.javax.slee.annotation.Service.class));
            services.put(toServiceID(a), clazz);
        }

        return sbbHome.getSbb();
    }

    public MockSleeComponent add(Class clazz) {
        return this.add(clazz, null);
    }

    public MockSleeComponent add(Class clazz, AnyEventHandler handler) {
        Annotation[] annotations = clazz.getAnnotations();
        List<Annotation> ann = Arrays.asList(annotations);

        for (Annotation a : ann) {

            if (a.annotationType().equals(mobi.mofokom.javax.slee.annotation.ResourceAdaptor.class)) {
                mobi.mofokom.javax.slee.annotation.ResourceAdaptor raA = (mobi.mofokom.javax.slee.annotation.ResourceAdaptor) a;
                NameVendorVersion nvv = NameVendorVersion.builder().name(raA.name()).vendor(raA.vendor()).version(raA.version()).build();
                return ann.stream().filter(aa -> aa.annotationType().equals(mobi.mofokom.javax.slee.annotation.ResourceAdaptorType.class)).map(aa -> {
                    mobi.mofokom.javax.slee.annotation.ResourceAdaptorType raTA = (mobi.mofokom.javax.slee.annotation.ResourceAdaptorType) aa;
                    MockResourceAdaptor mRa = null;
                    try {

                        mRa = this.createResourceAdaptorEntity(nvv, clazz, raTA.raInterface(), raA.usageParametersInterface(), raTA.aciFactory(), handler);
                        this.add(mRa);
                        return mRa;
                    } catch (Exception ex) {
                        Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    throw new IllegalCallerException(clazz.getName());
                }).findFirst().get();
            } else if (a.annotationType().equals(mobi.mofokom.javax.slee.annotation.event.EventType.class)) {

                mobi.mofokom.javax.slee.annotation.event.EventType eA = (mobi.mofokom.javax.slee.annotation.event.EventType) a;
                NameVendorVersion nvv = NameVendorVersion.builder().name(eA.name()).vendor(eA.vendor()).version(eA.version()).build();
                MockEvent mE = new MockEvent(nvv, clazz);
                this.events.add(mE);
                return mE;
            } else if (a.annotationType().equals(mobi.mofokom.javax.slee.annotation.Sbb.class)) {
                mobi.mofokom.javax.slee.annotation.Sbb sbbA = (mobi.mofokom.javax.slee.annotation.Sbb) a;
                NameVendorVersion nvv = NameVendorVersion.builder().name(sbbA.name()).vendor(sbbA.vendor()).version(sbbA.version()).build();
                try {
                    final MockSbb mSbb = new MockSbb(nvv, clazz, sbbA.localInterface(), sbbA.usageParametersInterface());
                    MockSlee.getInstance().sbbAlias.put(sbbA.alias() == null ? nvv.toString() : sbbA.alias(), nvv);

                    this.add(mSbb);
                    return mSbb;
                } catch (Exception ex) {
                    Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (a.annotationType().equals(mobi.mofokom.javax.slee.annotation.ProfileSpec.class)) {

                mobi.mofokom.javax.slee.annotation.ProfileSpec pA = (mobi.mofokom.javax.slee.annotation.ProfileSpec) a;
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
            } else if (a.annotationType().equals(mobi.mofokom.javax.slee.annotation.UsageParameter.class)) {
            }
        }
        throw new IllegalCallerException(clazz.getName());
    }

    public void start() throws NamingException {
        log.info("starting slee");
        this.state = SleeLifecycle.STARTING;
        this.setupJNDI();
        setupFactories();
        for (MockResourceAdaptor ra : ras) {
            try {
                log.info("starting " + ra.nvv.toString());
                //TODO store component state
                ra.start();
                log.info("started " + ra.nvv.toString());
            } catch (InvalidConfigurationException ex) {
                Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        this.state = SleeLifecycle.STARTED;
        log.info("started slee");
        EventTypeID eventType = new EventTypeID(
                ServiceStartedEventHandler.SERVICE_STARTED_EVENT_1_1_NAME,
                ServiceStartedEventHandler.SERVICE_STARTED_EVENT_1_1_VENDOR,
                ServiceStartedEventHandler.SERVICE_STARTED_EVENT_1_1_VERSION
        );

        log.info("root sbbs: " + services.values().stream().map(r -> r.toString()).collect(Collectors.toList()).toString());
        for (MockSbb sbb : sbbs) {
            //TODO store component state
            log.info(sbb.getSbb().getClass() + " " + services.values().stream().map(r -> r.toString()).collect(Collectors.toList()).toString());
            services.entrySet().stream().filter(r -> r.getValue().isAssignableFrom(sbb.getSbb().getClass())).forEach(r2 -> {
                log.info("starting " + sbb.nvv.toString());

                ServiceStartedEvent event = mock(ServiceStartedEvent.class);
                ServiceID serviceId = null; //reverseKey lookup
                Mockito.doReturn(serviceId).when(event).getService();
                ServiceActivity activity = mock(ServiceActivity.class);
                doReturn(r2.getKey()).when(activity).getService();
                ActivityContextInterface aci = mock(ActivityContextInterface.class);
                doReturn(activity).when(aci).getActivity();
                EventContext ec = new MockEventContext(event, aci, null, serviceId);
                FireableEventType fet = new MockFireableEventType(eventType, event);

                try {
                    deliverEvent(sbb, fet, aci, ec);
                    serviceState.put(serviceId, ServiceLifecycle.ACTIVE);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    serviceState.put(serviceId, ServiceLifecycle.STOPPED);
                    log.error(ex.getMessage(), ex);
                }

                log.info("started " + sbb.nvv.toString());
            });
        }
    }

    public boolean isStarted() {
        return this.state == SleeLifecycle.STARTED;
    }

    public void stop() {
        this.state = SleeLifecycle.STOPPING;

        for (MockResourceAdaptor ra : ras) {
            ra.stop();
        }

        for (MockSbb sbb : sbbs) {
            sbb.getSbb().unsetSbbContext();
        }
        log.info("------------------- STOPPED ------------------------");
        this.state = SleeLifecycle.STOPPED;
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

    public SleeLifecycle getState() {
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

        /**
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
        */
    }

    public void deliverEvent(FireableEventType eventType, ActivityContextInterface aci, EventContext ec) throws MultiException, UnhandledEventException {
        final List<Throwable> e = new ArrayList<>();
        final List<Boolean> h = new ArrayList<>();

        this.getSbbs().forEach(sbb -> {
            //TODO: check root sbb for receivable service if specified
            try {
                boolean handled = deliverEvent(sbb, eventType, aci, ec);
                h.add(handled);

                //TODO slee transaction 
                //TODO sort by priority
                sbb.childRelation.values().stream().forEach(cr -> {
                    final CompletableFuture cf = CompletableFuture.runAsync(() -> {
                        ((MockChildRelation) cr).mock.forEach(childSbb -> {

                            Future<?> future = this.sleeExecutor.submit(() -> {
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
                    });
                    cf.join();
                    //wait for all futures;
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
        try {
            txMgr.beginSleeTransaction();
        } catch (NotSupportedException | SystemException ex) {
            Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
        }
        Sbb sbb = mockSbb.getSbb();
        Class clazz = mockSbb.getSbbClass();
        Annotation[] annotations = clazz.getAnnotations();

        NameVendorVersion tNvv = NameVendorVersion.from(eventType.getEventType());

        log.info(tNvv.toString());
        Optional<Method> hm = Arrays.asList(clazz.getMethods()).stream()
                .filter(m -> m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.EventHandler.class))
                .filter(m -> {
                    Annotation a = m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.EventHandler.class);
                    mobi.mofokom.javax.slee.annotation.event.EventHandler eA = (mobi.mofokom.javax.slee.annotation.event.EventHandler) a;

                    NameVendorVersion nvv = NameVendorVersion.builder().name(eA.eventType().name()).vendor(eA.eventType().vendor()).version(eA.eventType().version()).build();

                    return nvv.equals(tNvv);
                })
                .findFirst();

        if (hm.isEmpty()) {
            hm = Arrays.asList(clazz.getMethods()).stream()
                    .filter(m -> m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.ServiceStartedEventHandler.class))
                    .filter(m -> {
                        Annotation a = m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.ServiceStartedEventHandler.class);
                        ServiceStartedEventHandler eA = (ServiceStartedEventHandler) a;

                        NameVendorVersion nvv = NameVendorVersion.builder().name(eA.eventType().name()).vendor(eA.eventType().vendor()).version(eA.eventType().version()).build();

                        return nvv.equals(tNvv);
                    })
                    .findFirst();
        }

        //TODO: add standard profiles
        if (hm.isPresent()) {
            log.info("handled " + tNvv + " on " + clazz);
            //TODO: create //load //passivate

            RolledBackContext rbc = new RolledBackContext() {
                @Override
                public Object getEvent() {
                    return ec.getEvent();
                }

                @Override
                public ActivityContextInterface getActivityContextInterface() {
                    return aci;
                }

                @Override
                public boolean isRemoveRolledBack() {
                    return true;
                }
            };

            try {
                this.callSbb(sbb, hm.get(), eventType, aci, ec);

                if (mockSbb.sbbContext.getRollbackOnly()) {
                    txMgr.rollback();
                    throw new RollbackException("rollback only flag set");
                }

                txMgr.commit();

            } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException | SystemException x) {
                log.error(x.getMessage(), x);
                sbb.sbbRolledBack(rbc);
            } catch (Exception x) {
                log.error(x.getMessage(), x);
                try {
                    sbb.sbbRolledBack(rbc);
                    sbb.sbbExceptionThrown(x, ec.getEvent(), aci);
                } catch (Exception x2) {
                    log.error("rollback failed with " + x.getMessage(), x);
                    sbb.sbbExceptionThrown(x2, ec.getEvent(), aci);
                }
            }

            try {
                log.info("slee transaction status " + StatusEnum.from(txMgr.getStatus()).name());
            } catch (SystemException ex) {
                Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
            }

            return true;
        } else {
            log.info("no handler for " + tNvv + " on " + clazz);
            return false;
        }

    }

    public void callSbb(Sbb sbb, Method m, FireableEventType eventType, ActivityContextInterface aci, EventContext ec) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Object event = ec.getEvent();
        assertNotNull(event, "event can't be null");
        assertNotNull(aci, "aci can't be null");
        assertNotNull(sbb);
        Class[] p = m.getParameterTypes();
        log.info("calling " + m.toString());
        if (p[0].isAssignableFrom(event.getClass()) && p[1].equals(ActivityContextInterface.class)) {
            log.info("invoking " + m.toGenericString());
            m.setAccessible(true);
            if (p.length == 2) {
                m.invoke(sbb, new Object[]{event, aci});
            } else if (p.length == 3) {
                m.invoke(sbb, new Object[]{event, aci, ec});
            } else {
                throw new IllegalArgumentException(sbb.getClass().getSimpleName() + " " + m.getName() + " should have 2 or 3 parameters it has" + p.length);
            }
        } else {
            throw new IllegalArgumentException(m.toString() + " is not assignable from " + event);
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

    public Alarm createAlarm(String alarmType, String instanceId, AlarmLevel alarmLevel, String message) {
        NotificationSource notificationSource = null;
        Throwable cause = null;
        return new Alarm(alarmType, notificationSource, alarmType, instanceId, alarmLevel, message, cause, 0);
    }

    private Alarm createAlarm(InvocationOnMock ic) {
        return createAlarm(ic.getArgumentAt(0, String.class),
                ic.getArgumentAt(1, String.class),
                ic.getArgumentAt(2, AlarmLevel.class),
                ic.getArgumentAt(3, String.class));
    }

    private Optional<NameVendorVersion> getComponentId(Sbb sbb) {
        return this.components.entrySet().stream().filter(k -> k.getValue().equals(sbb)).map(k -> k.getKey()).findFirst();
    }

    private void setupFactories() {
        doReturn(mock(NullActivity.class)).when(this.nullActivityFactory).createNullActivity();
        doReturn(mock(ActivityContextInterface.class)).when(this.nullAciFactory).getActivityContextInterface(any(NullActivity.class));
    }

    private ServiceID toServiceID(Service a) {
        return new ServiceID(a.name(), a.vendor(), a.version());
    }
}
