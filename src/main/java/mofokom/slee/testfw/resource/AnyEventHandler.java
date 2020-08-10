package mofokom.slee.testfw.resource;

import javax.slee.ActivityContextInterface;
import javax.slee.EventContext;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.FireableEventType;
import mofokom.slee.testfw.mocks.MockResourceAdaptor;

/**
 *
 * @author wozza
 */
@FunctionalInterface
public interface AnyEventHandler {

    public void onAnyEvent(ActivityHandle handle, FireableEventType eventType, ActivityContextInterface aci, EventContext ec, MockResourceAdaptor ra);

}
