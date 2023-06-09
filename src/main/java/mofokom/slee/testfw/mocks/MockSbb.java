//TODO validate eventFiring method signatures
package mofokom.slee.testfw.mocks;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import static java.util.logging.Level.*;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.*;
import javax.slee.facilities.*;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileTableActivityContextInterfaceFactory;
import javax.slee.resource.ReceivableService;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;
import javax.slee.serviceactivity.ServiceStartedEvent;
import mobi.mofokom.javax.slee.annotation.event.EventTypeRef;
import mofokom.slee.testfw.NameVendorVersion;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 * @author wozza
 */
public class MockSbb<SBB extends Sbb, SBBLOCAL extends SbbLocalObject, USAGE> extends MockSleeComponent {

    public final SbbContext sbbContext;
    private final Tracer tracer;
    private Logger log;
    private final SBB sbb;
    private final Class<? extends SbbLocalObject> local;
    private final Class<? extends Sbb> sbbClass;
    private final Class usage;
    private SBBLOCAL sbbLocalObject; //TODO: support cascading deletes
    private InitialContext ic;
    private Context env;
    private AlarmFacility alarmFacility;
    private TimerFacility timerFacility;
    private ServiceActivityFactory serviceActivityFactory;
    private ServiceActivityContextInterfaceFactory serviceActivityACIFactory;
    private ProfileTableActivityContextInterfaceFactory profileTableACIFactory;
    private ProfileFacility profileFacility;
    private NullActivityFactory nullActivityFactory;
    private ActivityContextNamingFacility acNamingFacility;
    private NullActivityContextInterfaceFactory nullAciFactory;

    private Map<String, Object> cmpMap = new HashMap<>();
    private SBB cmpProxy;

    public Map<NameVendorVersion, MockChildRelation> childRelation = new HashMap<>();

    private Map<String, NameVendorVersion> childRelationMethods = new HashMap<>();

    public Map<NameVendorVersion, Method> eventHandlers = new HashMap();
    public Map<NameVendorVersion, Method> eventFires = new HashMap();

    public MockSbb(NameVendorVersion nvv, Class<? extends Sbb> sbbClass) throws InstantiationException, IllegalAccessException {
        this(nvv, sbbClass, MockSbbLocal.class, MockUsageParameters.class);
    }

    public MockSbb(NameVendorVersion nvv, Class<? extends Sbb> sbbClass, Class<? extends SbbLocalObject> local) throws InstantiationException, IllegalAccessException {
        this(nvv, sbbClass, local, MockUsageParameters.class);
    }

    public MockSbb(NameVendorVersion nvv, Class<? extends Sbb> sbbClass, Class<? extends SbbLocalObject> local, Class usage) throws InstantiationException, IllegalAccessException {
        super(nvv);
        this.local = local;
        this.usage = usage;
        this.sbbClass = sbbClass;
        log = Logger.getLogger(this.getClass().getSimpleName());

        sbb = (SBB) mock(sbbClass); //TODO use proxy for abstract?


        /*
        cmpProxy = (SBB) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{sbbClass}, new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if(Modifier.isAbstract(method.getModifiers())){

                    if(method.getName().startsWith("get"))
                        return cmpMap.get(method.getName().substring(3));
                    
                    else if(method.getName().startsWith("set"))
                        return cmpMap.put(method.getName().substring(3),args[0]);

                }
                else return method.invoke(sbb, args);

                return null;
            }
        });
        * 
         */
        sbbContext = mock(SbbContext.class);
        tracer = mock(Tracer.class);
    }

    Answer loggingAnswer = new Answer() {

        public Object answer(InvocationOnMock invocation) throws Throwable {
            log.info(invocation.getMethod().getName() + " " + Arrays.asList(invocation.getArguments()).toString());
            for (Object o : invocation.getArguments()) {
                if (o instanceof Throwable) {
                    log.log(java.util.logging.Level.WARNING, ((Throwable) o).getMessage(), (Throwable) o);
                }
            }
            return null;
        }
    };

    private void setupStubs() {

        doReturn(Boolean.TRUE).when(tracer).isInfoEnabled();
        doReturn(Boolean.TRUE).when(tracer).isFinestEnabled();
        doReturn(Boolean.TRUE).when(tracer).isFinerEnabled();
        doReturn(Boolean.TRUE).when(tracer).isFineEnabled();
        doReturn(Boolean.TRUE).when(tracer).isSevereEnabled();

        doAnswer(loggingAnswer).when(tracer).finest(any());
        doAnswer(loggingAnswer).when(tracer).finest(any(), any(Throwable.class));
        doAnswer(loggingAnswer).when(tracer).fine(any());
        doAnswer(loggingAnswer).when(tracer).fine(any(), any(Throwable.class));
        doAnswer(loggingAnswer).when(tracer).info(any());
        doAnswer(loggingAnswer).when(tracer).info(any(), any(Throwable.class));
        doAnswer(loggingAnswer).when(tracer).warning(any());
        doAnswer(loggingAnswer).when(tracer).warning(any(), any(Throwable.class));
        doAnswer(loggingAnswer).when(tracer).severe(any(), any(Throwable.class));
        doAnswer(loggingAnswer).when(tracer).severe(any());
        doReturn(tracer).when(sbbContext).getTracer(any());
        doReturn(new SbbID(sbbClass.getName(), "mockVendor", "mockVersion")).when(sbbContext).getSbb();
        doReturn(getSbbLocalObject()).when(sbbContext).getSbbLocalObject();

    }

    public static void doCallRealMethods(Object target, Class clazz) throws Exception {
        MockSlee.doCallRealMethods(target, clazz);
    }

    public void doCallRealMethodEventHandlers() {
        this.eventHandlers.entrySet().forEach(e -> {
            log.info("calling " + e.getValue().getName() + " for " + e.getKey().toString());
            MockSlee.doCallRealMethod(sbb, e.getValue());
        });
    }

    public void init() throws Exception {
        log.info("init sbb" + sbb.getClass().getName());

        doCallRealMethods(sbb, javax.slee.Sbb.class);

        Arrays.stream(this.sbbClass.getDeclaredFields()).forEach(f -> {
            mobi.mofokom.javax.slee.annotation.CMPField cmp = f.getAnnotation(mobi.mofokom.javax.slee.annotation.CMPField.class);
            if (cmp != null) {
                log.info("detected CMP " + f);
                addCmp(f);
            }
        });
        /**

        Arrays.stream(this.sbbClass.getMethods()).forEach(m -> {
            mobi.mofokom.javax.slee.annotation.CMPField cmp = m.getAnnotation(mobi.mofokom.javax.slee.annotation.CMPField.class);
            if (cmp != null) {
                addCmp(m);
            }
        });
         **/

        Arrays.stream(this.sbbClass.getFields()).forEach(f -> {
            mobi.mofokom.javax.slee.annotation.ChildRelation cr = f.getAnnotation(mobi.mofokom.javax.slee.annotation.ChildRelation.class);
            if (cr != null) {
                try {
                    addChildRelation(cr.sbbAliasRef(), cr.defaultPriority(), f);
                } catch (Exception ex) {
                    log.log(SEVERE, ex.getMessage(), ex);
                }
            }
        });

        Arrays.stream(this.sbbClass.getMethods()).forEach(m -> {
            mobi.mofokom.javax.slee.annotation.ChildRelation cr = m.getAnnotation(mobi.mofokom.javax.slee.annotation.ChildRelation.class);
            if (cr != null) {
                try {
                    this.addChildRelation(cr.sbbAliasRef(), cr.defaultPriority(), m);
                } catch (Exception ex) {
                    log.log(SEVERE, ex.getMessage(), ex);
                }
            }
        });

        Arrays.stream(this.sbbClass.getMethods())
                .filter(m
                        -> m.getReturnType().isAssignableFrom(ChildRelation.class) && m.getName().startsWith("get"))
                .forEach(m -> {

                    Object o = doAnswer(ic -> {
                        NameVendorVersion childNvv = MockSbb.this.childRelationMethods.get(ic.getMethod().getName());
                        log.info("returning child relation for " + childNvv + " child of " + this.nvv);
                        MockChildRelation mcr = MockSbb.this.childRelation.get(childNvv);
                        if (mcr == null) {
                            throw new Exception("can't find child relation for " + childNvv + " child of " + this.nvv + " did you add it before?");
                        }

                        return mcr.mock;
                    }).when(sbb);
                    MockSlee.doDangling(o, sbb, m);
                });

        Arrays.asList(this.sbbClass.getMethods()).stream()
                .filter(m
                        -> m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.EventHandler.class))
                .forEach(m -> {
                    mobi.mofokom.javax.slee.annotation.event.EventHandler a = m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.EventHandler.class);
                    eventHandlers.put(NameVendorVersion.from(a.eventType()), m);
                });

        Arrays.asList(this.sbbClass.getMethods()).stream()
                .filter(m
                        -> m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.ServiceStartedEventHandler.class)
                || m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.ActivityEndEventHandler.class)
                || m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.ProfileAddedEventHandler.class)
                || m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.ProfileRemovedEventHandler.class)
                || m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.ProfileUpdatedEventHandler.class)
                || m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.ServiceStartedEventHandler.class)
                || m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.TimerEventHandler.class)
                )
                .forEach(m -> {
                    Stream.of(
                            m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.ServiceStartedEventHandler.class),
                            m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.ActivityEndEventHandler.class),
                            m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.ProfileAddedEventHandler.class),
                            m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.ProfileRemovedEventHandler.class),
                            m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.ProfileUpdatedEventHandler.class),
                            m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.ServiceStartedEventHandler.class),
                            m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.TimerEventHandler.class)
                    ).filter(Objects::nonNull).forEach(a -> {
                        try {
                            eventHandlers.put(NameVendorVersion.from((EventTypeRef) a.getClass().getMethod("eventType").invoke(a)), m);
                        } catch (NoSuchMethodException ex) {
                            Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                        } catch (SecurityException ex) {
                            Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                        } catch (IllegalAccessException ex) {
                            Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                        } catch (InvocationTargetException ex) {
                            Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                        }
                    });

                });

        log.info("handles events " + eventHandlers.keySet().toString());
        Arrays.asList(this.sbbClass.getMethods()).stream()
                .filter(m
                        -> m.isAnnotationPresent(mobi.mofokom.javax.slee.annotation.event.EventFiring.class))
                .forEach(m -> {
                    mobi.mofokom.javax.slee.annotation.event.EventFiring a = m.getAnnotation(mobi.mofokom.javax.slee.annotation.event.EventFiring.class);
                    Object o = doAnswer(ic -> {
                        log.info("firing event " + ic.toString());
                        ActivityContextInterface aci = ic.getArgumentAt(1, ActivityContextInterface.class);
                        EventTypeID eventTypeId = new EventTypeID(a.value().name(), a.value().vendor(), a.value().version());
                        Object event = ic.getArgumentAt(0, Object.class);
                        EventContext eventContext = MockSlee.createEventContext(aci, event, ic.getArgumentAt(2, Address.class), mock(ReceivableService.class));
                        MockSlee.getInstance().deliverEvent(new MockFireableEventType(eventTypeId, event), aci, eventContext);
                        return null;
                    }).when(sbb);

                    MockSlee.doDangling(o, sbb, m);
                    eventFires.put(NameVendorVersion.from(a.value()), m);
                });

        log.info("fire events " + eventFires.keySet().toString());
        Arrays.stream(this.sbbClass.getMethods())
                .filter(m -> m.isAnnotationPresent(PostConstruct.class))
                .forEach(m -> {
                    try {
                        MockSlee.doCallRealMethod(sbb, m);
                        m.invoke(sbb, new Object[0]);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                    }
                });

        log.info("has cmp fields " + this.cmpMap.keySet().toString());
        log.info("Sbb Mock starting");
        this.setupStubs();
        try {
            sbb.setSbbContext(sbbContext);
        } catch (Throwable t) {
            log.warning("abstract sbb class");
        }
    }

    public void deinit() {
        sbb.unsetSbbContext();
    }

    public SBB getSbb() {
        return sbb;
    }

    public Class<? extends Sbb> getSbbClass() {
        return this.sbbClass;
    }

    public SBBLOCAL getSbbLocalObject() {
        if (sbbLocalObject == null) {

            Set<Class> classes = new HashSet<>();
            classes.add(local);
            classes.add(MockSbbLocal.class);
            Class<?>[] interfaces = this.getClass().getInterfaces();
            classes.addAll(Arrays.asList(interfaces));

            sbbLocalObject = (SBBLOCAL) Proxy.newProxyInstance(this.getClass().getClassLoader(), classes.toArray(new Class[classes.size()]), new InvocationHandler() {

                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                    log.info(method.toString());
                    Object target = sbb;

                    /**
                     * if
                     * (method.getDeclaringClass().equals(MockSbbLocal.class)) {
                     * return sbb; }
                     */
                    if (method.getDeclaringClass().isAssignableFrom(target.getClass())) {
                        Optional<Method> m = MockSbb.this.findSameMethodOn(method, target);
                        if (!m.isPresent()) {
                            throw new Exception("local method not found on Sbb" + m.toString());
                        }
                        method = m.get();
                        log.info(method.toString());
                    }

                    return method.invoke(target, args); //TODO dynamically replace target negation implements local interface.
                }
            });
        }
        return sbbLocalObject;
    }

    public ServiceStartedEvent getServiceStartedEvent() {
        return new ServiceStartedEvent() {

            @Override
            public ServiceID getService() {
                return null;
            }
        };
    }

    public TimerEvent createTimerEvent() {
        TimerEvent mock = mock(TimerEvent.class);
        doReturn(mock(TimerID.class)).when(mock).getTimerID();
        return mock;
    }

    public void bindACI(String name, Class aci) throws NamingException {
        ic.bind(name, mock(aci));
    }

    public void bind(String name, Object o) throws NamingException {
        ic.bind(name, o);
    }

    public SbbContext getSbbContext() {
        return this.sbbContext;
    }

    public void addCmp(Field f) {
        if (!f.getType().isPrimitive() || Serializable.class.isAssignableFrom(f.getType())) {
            log.warning(f.toString() + " is not serializable");
        }

        Class<?> clazz = f.getDeclaringClass();
        StringBuilder name = new StringBuilder(f.getName());
        name.setCharAt(0, Character.toUpperCase(name.charAt(0)));
        try {
            addCmp(clazz.getMethod("set" + name.toString(), f.getType()));
            addCmp(clazz.getMethod("get" + name.toString()));
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    public void addCmp(Method m) {
        final String fieldName = m.getName().substring(3).toLowerCase();
        log.info("adding cmp " + fieldName + " " + m.toString());

        this.cmpMap.put(fieldName, null);
        Object o = doAnswer(ic -> {
            String fieldName2 = m.getName().substring(3).toLowerCase();
            //log.info("!!!!!!!!!!!!!!access cmp " + fieldName2 + " " + ic.getMethod().toString() + " " + Arrays.asList(Thread.currentThread().getStackTrace()).stream().filter(st->st.getClassName().contains("test")).collect(toList()).toString());
            Method m2 = ic.getMethod();
            if (m2.getName().startsWith("get")) {
                log.fine("getting cmp " + fieldName2);
                Object v = MockSbb.this.cmpMap.get(fieldName2);
                log.fine("got cmp " + fieldName2 + " " + v);
                //log.log(INFO, "" , new Exception());
                return v;
            } else if (m2.getName().startsWith("set")) {
                MockSbb.this.cmpMap.put(fieldName, ic.getArguments()[0]);
                log.fine("setting cmp " + fieldName2 + " " + ic.getArguments()[0]);
                return Void.TYPE;
            } else {
                throw new IllegalAccessError(m.toString());
            }
        }).when(sbb);

        MockSlee.doDangling(o, sbb, m);
    }

    public MockChildRelation mockChildRelation(MockSbb parent, MockSbb sbb, NameVendorVersion nvv) {
        MockChildRelation mockCR = new MockChildRelation(parent, sbb);
        parent.childRelation.put(nvv, mockCR);
        return mockCR;
    }

    public void addChildRelation(String alias, int priority, Method m) throws Exception {
        NameVendorVersion nvv = MockSlee.getInstance().sbbAlias.get(alias);
        if (nvv == null) {
            throw new Exception("no sbb found for alias " + alias);
        }

        MockSbb.this.childRelationMethods.put(m.getName(), nvv);
        MockSleeComponent mockSbb = MockSlee.getInstance().components.get(nvv);
        MockChildRelation mockCR = mockChildRelation(this, (MockSbb) mockSbb, nvv);
        mockCR.setPriority(priority);
        mockCR.setAlias(alias);
    }

    public void addChildRelation(String alias, int priority, Field f) throws Exception {
        Class<?> clazz = f.getDeclaringClass();
        StringBuilder name = new StringBuilder(f.getName());
        name.setCharAt(0, Character.toUpperCase(name.charAt(0)));
        try {
            addChildRelation(alias, priority, clazz.getMethod("get" + name.toString() + "ChildRelation"));
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(MockSbb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    private Optional<Method> findSameMethodOn(Method method, Object target) {
        return Arrays.stream(target.getClass().getMethods()).filter(m -> m.getName().equals(method.getName())).findFirst();
    }

}
