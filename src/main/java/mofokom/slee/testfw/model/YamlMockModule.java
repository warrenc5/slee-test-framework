/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mofokom.slee.testfw.model;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author wozza
 */
public class YamlMockModule extends SimpleModule {
    
        
    public YamlMockModule(String name, Version version) {
        super(name, version);
        super.setSerializerModifier(new BeanSerializerModifier() {
            List ignoreList = Arrays.asList(new String[]{"defaultAnswer", "mockSettings", "handler", "callbacks", "innerMock"});

            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                return beanProperties.stream().filter((bp) -> !ignoreList.contains(bp.getName())).collect(Collectors.toList());
            }
        });
    }
    
}
