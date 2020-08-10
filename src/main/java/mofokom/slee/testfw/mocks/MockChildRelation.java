/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mofokom.slee.testfw.mocks;

import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import mofokom.slee.testfw.NameVendorVersion;
import mofokom.slee.testfw.mocks.MockSleeComponent;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 *
 * @author wozza
 */
public class MockChildRelation extends MockSleeComponent{

    public ChildRelation mock = mock(ChildRelation.class);

    public MockChildRelation(NameVendorVersion nvv, SbbLocalObject o) {
        super(nvv);
        try {
            doReturn(o).when(mock).create();
        } catch (CreateException ex) {
            throw new RuntimeException(ex);
        } catch (SLEEException ex) {
            throw new RuntimeException(ex);
        }
    }

}
