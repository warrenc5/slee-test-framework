package mofokom.slee.testfw.mocks;

import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.EventContext;
import javax.slee.SLEEException;
import javax.slee.ServiceID;
import javax.slee.TransactionRequiredLocalException;

/**
 *
 * @author wozza
 */
public class MockEventContext implements EventContext {

    private Object event;
    private ActivityContextInterface aci;
    private Address address;
    private ServiceID serviceId;
    private boolean suspended;

    public MockEventContext(Object event, ActivityContextInterface aci, Address address, ServiceID serviceId) {
        this.event = event;
        this.aci = aci;
        this.address = address;
        this.serviceId = serviceId;
    }


    @Override
    public Object getEvent() {
        return event;
    }

    @Override
    public ActivityContextInterface getActivityContextInterface() {
        return aci;
    }

    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public ServiceID getService() {
        return serviceId;
    }

    @Override
    public void suspendDelivery() throws IllegalStateException, TransactionRequiredLocalException, SLEEException {
        suspended = true;
    }

    @Override
    public void suspendDelivery(int i) throws IllegalArgumentException, IllegalStateException, TransactionRequiredLocalException, SLEEException {
        suspended = true;
    }

    @Override
    public void resumeDelivery() throws IllegalStateException, TransactionRequiredLocalException, SLEEException {
        suspended = false;
    }

    @Override
    public boolean isSuspended() throws TransactionRequiredLocalException, SLEEException {
        return suspended;
    }

}
