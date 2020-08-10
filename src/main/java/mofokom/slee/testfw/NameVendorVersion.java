/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mofokom.slee.testfw;

import javax.slee.EventTypeID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 *
 * @author wozza
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class NameVendorVersion {

    public String name, vendor, version;

    public static NameVendorVersion from(String name, String vendor, String version) {
        return new NameVendorVersion(name, vendor, version);
    }

    public static NameVendorVersion from(EventTypeID eventType) {
        return NameVendorVersion.builder().name(eventType.getName()).vendor(eventType.getVendor()).version(eventType.getVersion()).build();
    }
}
