/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mofokom.slee.testfw.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.*;
import javax.slee.facilities.*;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileTableActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;
import javax.slee.serviceactivity.ServiceStartedEvent;
import mofokom.slee.testfw.MockSlee;
import mofokom.slee.testfw.MockUsageParameters;
import mofokom.slee.testfw.SleeComponent;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 * @author wozza
 */
public class MockSbb<SBB extends Sbb, SBBLOCAL extends SbbLocalObject, USAGE> extends SleeComponent {

    public final SbbContext sbbContext;
    private final Tracer tracer;
    private Logger log;
    private final SBB sbb;
    private final Class<? extends SbbLocalObject> local;
    private final Class<? extends Sbb> sbbClass;
    private final Class usage;
    private SBBLOCAL sbbLocalObject;
    private static final Logger slog = Logger.getAnonymousLogger();
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

    private Map<String, Object> cmpMap = new HashMap<String, Object>();
    private SBB cmpProxy;

    public MockSbb(Class<? extends Sbb> sbbClass) throws InstantiationException, IllegalAccessException {
        this(sbbClass, MockSbbLocal.class, MockUsageParameters.class);
    }

    public MockSbb(Class<? extends Sbb> sbbClass, Class<? extends SbbLocalObject> local) throws InstantiationException, IllegalAccessException {
        this(sbbClass, local, MockUsageParameters.class);
    }

    public MockSbb(Class<? extends Sbb> sbbClass, Class<? extends SbbLocalObject> local, Class usage) throws InstantiationException, IllegalAccessException {
        this.local = local;
        this.usage = usage;
        this.sbbClass = sbbClass;
        log = Logger.getLogger(this.getClass().getSimpleName());

        sbb = (SBB) mock(sbbClass);

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
                    ((Throwable) o).printStackTrace();
                }
            }
            return null;
        }
    };

    private void setupStubs() {

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
        doReturn(tracer).when(sbbContext).getTracer(anyString());
        doReturn(new SbbID(sbbClass.getName(), "mockVendor", "mockVersion")).when(sbbContext).getSbb();
        doReturn(getSbbLocalObject()).when(sbbContext).getSbbLocalObject();

    }

    public static void doCallRealMethods(Object target, Class clazz) throws Exception {
        MockSlee.doCallRealMethods(target, clazz);
    }

    public void init() throws Exception {

        doCallRealMethods(sbb, javax.slee.Sbb.class);

        slog.info("Sbb Mock starting");
        this.setupStubs();
        sbb.setSbbContext(sbbContext);
    }

    public void deinit() {
        sbb.unsetSbbContext();
    }

    public SBB getSbb() {
        return sbb;
    }

    public SBBLOCAL getSbbLocalObject() {
        if (sbbLocalObject == null) {

            Set<Class> classes = new HashSet<>();
            classes.add(local);
            classes.add(MockSbbLocal.class);
            sbbLocalObject = (SBBLOCAL) Proxy.newProxyInstance(this.getClass().getClassLoader(), classes.toArray(new Class[]{}), new InvocationHandler() {

                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getDeclaringClass().equals(MockSbbLocal.class)) {
                        return sbb;
                    }

                    return method.invoke(sbb, args);
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
}
