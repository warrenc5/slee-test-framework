package mofokom.slee.testfw.mocks;

import javax.slee.resource.FireableEventType;
import mofokom.slee.testfw.NameVendorVersion;

/**
 *
 * @author wozza
 */
public class UnhandledEventException extends Exception {

    private final FireableEventType eventType;

    public UnhandledEventException(FireableEventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public String getMessage() {
        return NameVendorVersion.from(this.eventType.getEventType()).toString() + " " + this.eventType.getEventClassName();
    }

}
