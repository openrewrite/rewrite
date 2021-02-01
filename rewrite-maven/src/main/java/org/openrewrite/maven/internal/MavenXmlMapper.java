/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.internal;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import javax.xml.stream.XMLInputFactory;

public class MavenXmlMapper {
    private static final ObjectMapper readMapper;
    private static final ObjectMapper writeMapper;

    static {
        // disable namespace handling, as some POMs contain undefined namespaces like Xlint in
        // https://repo.maven.apache.org/maven2/com/sun/istack/istack-commons/3.0.11/istack-commons-3.0.11.pom
        XMLInputFactory input = new WstxInputFactory();
        input.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        input.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        XmlFactory xmlFactory = new XmlFactory(input, new WstxOutputFactory());
        {
            ObjectMapper m = XmlMapper.builder(xmlFactory)
                    .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                    .defaultUseWrapper(false)
                    .build()
                    .registerModule(new ParameterNamesModule())
                    .disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                    .disable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            readMapper = m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                    .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY));
        }
        {
            writeMapper = XmlMapper.builder(xmlFactory)
                    .defaultUseWrapper(false)
                    .build()
                    .registerModule(new JaxbAnnotationModule());
        }
    }

    private MavenXmlMapper() {
    }

    public static ObjectMapper readMapper() {
        return readMapper;
    }

    public static ObjectMapper writeMapper() {
        return writeMapper;
    }
}
