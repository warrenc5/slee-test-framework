/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mofokom.slee.testfw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @author wozza
 */
@Data
@Builder
@AllArgsConstructor
public class NameVendorVersion {
    public String name,vendor,version;

}
