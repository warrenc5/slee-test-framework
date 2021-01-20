package mofokom.slee.testfw.resource;

import javax.slee.ActivityContextInterface;
import javax.slee.EventContext;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.FireableEventType;
import mofokom.slee.testfw.mocks.MockResourceAdaptor;
import mofokom.slee.testfw.mocks.MockSlee;
import mofokom.slee.testfw.mocks.MultiException;
import mofokom.slee.testfw.mocks.UnhandledEventException;
import static org.junit.jupiter.api.Assertions.fail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wozza
 */
public class DefaultEventHandler implements AnyEventHandler {

    private final MockSlee slee;
    private final String name;
    private Logger log = LoggerFactory.getLogger(this.getClass());

    public DefaultEventHandler(String name, MockSlee slee) {
        this.name = name;
        this.slee = slee;
    }

    public DefaultEventHandler(MockSlee slee) {
        this("default", slee);
    }

    @Override
    public void onAnyEvent(ActivityHandle handle, FireableEventType eventType, ActivityContextInterface aci, EventContext ec, MockResourceAdaptor ra) {
        try {
            log.debug("delivering event" + eventType.getEventType());
            slee.deliverEvent(eventType, aci, ec);
        } catch (MultiException ex) {
            log.error(ex.getMessage(), ex);
            fail(ex);
        } catch (UnhandledEventException ex) {
            log.warn(ex.getClass().getName() + " " + ex.getMessage());
        }
    }
}
