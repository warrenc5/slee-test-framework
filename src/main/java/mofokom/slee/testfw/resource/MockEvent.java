package mofokom.slee.testfw.resource;

import mofokom.slee.testfw.SleeComponent;
import static org.mockito.Mockito.mock;

public class MockEvent extends SleeComponent {

    private final Object event;

    public MockEvent(Class aClass) {
        this.event = mock(aClass);
    }

}
