package mofokom.slee.testfw.mocks;

import javax.slee.EventTypeID;
import javax.slee.resource.FireableEventType;

/**
 *
 * @author wozza
 */
class MockFireableEventType implements FireableEventType {

    private final EventTypeID eventType;
    private final Object event;

    public MockFireableEventType(EventTypeID eventType, Object event) {
        this.eventType = eventType;
        this.event = event;
    }

    @Override
    public EventTypeID getEventType() {
        return eventType;
    }

    @Override
    public String getEventClassName() {
        return event.getClass().getName();
    }

    @Override
    public ClassLoader getEventClassLoader() {
        return event.getClass().getClassLoader();
    }

}
