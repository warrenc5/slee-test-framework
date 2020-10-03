/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mofokom.slee.testfw.mocks;

import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.SLEEException;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 *
 * @author wozza
 */
public class MockChildRelation extends MockSleeComponent {

    public ChildRelation mock = mock(ChildRelation.class);
    public final MockSbb sbb;
    public final MockSbb parent;
    public String alias;
    public int priority;

    public MockChildRelation(MockSbb parent, MockSbb sbb) {
        super(sbb.nvv);
        this.sbb = sbb;
        this.parent = parent;
        try {
            //TODO answer add to list
            doAnswer(mh -> {
                sbb.childRelation.put(nvv, this);
                return sbb.getSbbLocalObject();
            }).when(mock).create();

        } catch (CreateException ex) {
            throw new RuntimeException(ex);
        } catch (SLEEException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

}
