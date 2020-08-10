package mofokom.slee.testfw.mocks;

import javax.slee.profile.Profile;
import lombok.Data;
import lombok.EqualsAndHashCode;
import mofokom.slee.testfw.NameVendorVersion;
import static org.mockito.Mockito.*;

@Data
@EqualsAndHashCode
public class MockProfile<ABSTRACT extends Profile, CMPIFACE, LOCAL, TABLE, MANAGEMENT> extends MockSleeComponent {

    private final ABSTRACT profileClazz;
    private MANAGEMENT management;
    private CMPIFACE cmpIface;
    private LOCAL local;
    private TABLE table;

    public MockProfile(NameVendorVersion nvv, Class profileClazz, Class cmpIface, Class local,Class table, Class management) {
        this(nvv, profileClazz);
        this.cmpIface = (CMPIFACE) mock(cmpIface);
        this.local = (LOCAL) mock(local);
        this.table = (TABLE) mock(table);
        this.management = (MANAGEMENT) mock(management);
    }

    public MockProfile(NameVendorVersion nvv, Class profileClazz) {
        super(nvv);
        this.profileClazz = (ABSTRACT) mock(profileClazz);
    }

}
