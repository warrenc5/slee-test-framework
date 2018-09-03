package mofokom.slee.testfw;

import java.lang.reflect.Method;
import mofokom.slee.testfw.service.ServiceLifecycle;
import mofokom.slee.testfw.service.MockSbb;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.ChildRelation;
import javax.slee.Sbb;
import javax.slee.SbbLocalObject;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.TimerFacility;
import javax.slee.nullactivity.NullActivity;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileTableActivityContextInterfaceFactory;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;
import lombok.extern.slf4j.Slf4j;
import mofokom.slee.testfw.resource.MockActivityContextInterface;
import mofokom.slee.testfw.resource.MockEvent;
import mofokom.slee.testfw.resource.MockResourceAdaptor;
import static mofokom.slee.testfw.resource.MockResourceAdaptor.getAny;
import mofokom.slee.testfw.service.MockChildRelation;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@Slf4j
public class MockSlee {

    private Set<MockSbb> sbbs = new HashSet<>();
    private Set<MockResourceAdaptor> ras = new HashSet<>();
    private Set<MockEvent> events = new HashSet<>();
    private Set<MockProfile> profiles = new HashSet<>();
    private Map<Object, ActivityContextInterface> acis = new HashMap<>();
    public Map<ActivityHandle, ActivityContextInterface> activities = new HashMap<>();

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

        System.setProperty("java.naming.factory.initial", NewInitialContextFactory.class.getName());

        ic = new InitialContext();

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

    public static void doCallRealMethods(Object target, Class... clazzz) throws Exception {

        Arrays.asList(clazzz).forEach(clazz -> {
            Object o = null;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().startsWith("ajc$")) {
                    continue;
                }

                /*
             * if (Modifier.isAbstract(m.getModifiers())){ slog.fine("abstract
             * method"); continue; }
             *
                 */
                //    continue;
                if (!clazz.equals(m.getDeclaringClass())) {
                    slog.log(Level.WARNING, m.getDeclaringClass() + " " + m.toString() + " not assignable from class " + clazz.getName());

                    continue;
                }
                if (m.getName().contains("access$") || (!clazz.isInterface() && Modifier.isAbstract(m.getModifiers())) || Modifier.isPrivate(m.getModifiers()) || Modifier.isFinal(m.getModifiers()) || m.getModifiers() == 0) {
                    continue;
                }

                //slog.fine("getting " + m.toString());
                //Method 
                //if (m2 == null)
                //continue;
                if (Modifier.isProtected(m.getModifiers())) {
                    continue;
                }
                /*
                m = getMethodFromTarget(target, m);
                * 
                 */

                Object[] args = new Object[m.getParameterTypes().length];

                int p = 0;
                if (o == null) {
                    o = doCallRealMethod().when(target);
                }

                for (Class c : m.getParameterTypes()) {
                    args[p++] = getAny(c);
                }
                slog.fine("calling real " + m.toString());
                try {
                    m.invoke(o, args);
                    o = null;
                } catch (RuntimeException x) {
                    slog.log(Level.WARNING, clazz + " " + o.getClass() + " " + m.getDeclaringClass() + " " + m.toString() + " " + x.getClass() + " " + x.getMessage());
                } catch (Exception x) {
                    slog.log(Level.WARNING, clazz + " " + o.getClass() + " " + m.getDeclaringClass() + " " + m.toString() + " " + x.getClass() + " " + x.getMessage());
                }

            }
        });
    }

    public Object mockEvent(Class aClass) {
        MockEvent me = null;
        this.events.add(me = new MockEvent(aClass));
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
    }

    public <A extends Sbb, B extends SbbLocalObject, C> A add(MockSbb<A, B, C> sbbHome) throws Exception {
        this.sbbs.add(sbbHome);
        sbbHome.init();
        doAnswer(ic -> {
            log.info("live activties " + this.activities.toString());
            log.info("bound activties " + this.sbbsA.toString());
            Set<ActivityContextInterface> set = this.sbbsA.entrySet().stream().filter(
                    e -> e.getValue().contains(sbbHome.getSbbLocalObject())
            ).map(
                    e -> this.activities.get(e.getKey())
            ).collect(Collectors.toSet());

            log.info(sbbHome.getSbbLocalObject() + " bound to " + set.toString());
            return set.toArray(new ActivityContextInterface[set.size()]);
        }).when(sbbHome.sbbContext).getActivities();
        return sbbHome.getSbb();
    }

    public void add(NameVendorVersion build, Class aClass) {
    }

    public void start() {
        this.state = ServiceLifecycle.ACTIVE;
        for (MockResourceAdaptor ra : ras) {
            try {
                ra.start();
            } catch (InvalidConfigurationException ex) {
                Logger.getLogger(MockSlee.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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

    public ChildRelation mockChildRelation(SbbLocalObject o) {
        return new MockChildRelation(o).mock;
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

}
