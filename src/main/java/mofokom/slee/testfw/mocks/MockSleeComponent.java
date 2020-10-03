package mofokom.slee.testfw.mocks;

import lombok.Data;
import mofokom.slee.testfw.NameVendorVersion;

/**
 *
 * @author wozza
 */
@Data
public abstract class MockSleeComponent {

    public MockSleeComponent(NameVendorVersion nvv) {
        this.nvv = nvv;
    }
    public NameVendorVersion nvv;

    public NameVendorVersion getNvv() {
        return nvv;
    }


}
