package mofokom.slee.testfw.mocks;

import mofokom.slee.testfw.NameVendorVersion;
import static org.mockito.Mockito.mock;

public class MockEvent extends MockSleeComponent {

    private final Object event;

    public MockEvent(NameVendorVersion nvv, Class aClass) {
        super(nvv);
        this.event = mock(aClass);
    }

}
