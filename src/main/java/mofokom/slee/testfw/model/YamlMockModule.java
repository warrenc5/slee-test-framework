/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mofokom.slee.testfw.model;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 *
 * @author wozza
 */
public class YamlMockModule extends SimpleModule {
    
        
    public YamlMockModule(String name, Version version) {
        super(name, version);
        super.setSerializerModifier(new IgnoreBeanPropertySerializerModifier(new String[]{"defaultAnswer", "mockSettings", "handler", "callbacks", "innerMock"}));
    }

    
}
