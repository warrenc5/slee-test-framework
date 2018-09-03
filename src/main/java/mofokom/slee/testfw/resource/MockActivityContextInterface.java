/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mofokom.slee.testfw.resource;

import javax.slee.ActivityContextInterface;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;
import javax.slee.resource.ActivityHandle;
import mofokom.slee.testfw.MockSlee;

/**
 *
 * @author wozza
 */
public class MockActivityContextInterface<T> implements ActivityContextInterface {

    private final ActivityHandle handle;

    boolean ending;
    private final MockSlee mockSlee;

    public MockActivityContextInterface(MockSlee mockSlee, ActivityHandle handle) {
        this.mockSlee = mockSlee;
        this.handle = handle;
    }

    public MockActivityContextInterface(MockSlee mockSlee, ActivityHandle handle, T activity) {
        this(mockSlee, handle);
        mockSlee.activityMap.put(handle, activity);
    }

    @Override
    public Object getActivity() throws TransactionRequiredLocalException, SLEEException {
        return mockSlee.activityMap.get(handle);
    }

    @Override
    public void attach(SbbLocalObject slo) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException, SLEEException {
        mockSlee.sbbsA.get(handle).add(slo);
    }

    @Override
    public void detach(SbbLocalObject slo) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException, SLEEException {
        mockSlee.sbbsA.get(handle).remove(slo);
        if (mockSlee.sbbsA.get(handle).isEmpty()) {
            end();
        }
    }

    @Override
    public boolean isAttached(SbbLocalObject slo) throws NullPointerException, TransactionRequiredLocalException, TransactionRolledbackLocalException, SLEEException {
        return mockSlee.sbbsA.get(handle).contains(slo);
    }

    @Override
    public boolean isEnding() throws TransactionRequiredLocalException, SLEEException {
        return ending;
    }

    public void end() {
        ending = true;
    }

}
