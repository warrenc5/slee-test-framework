package mofokom.slee.testfw.model;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class YamlMockModule extends SimpleModule {
    
        
    public YamlMockModule(String name, Version version) {
        super(name, version);
        //super.setSerializerModifier(new IgnoreBeanPropertySerializerModifier(new String[]{"defaultAnswer", "mockSettings", "handler", "callbacks", "innerMock"}));
    }

    
}
