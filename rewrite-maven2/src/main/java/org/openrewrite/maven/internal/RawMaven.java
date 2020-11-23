package org.openrewrite.maven.internal;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public class RawMaven {
    private static final ObjectMapper xmlMapper;

    static {
        // disable namespace handling, as some POMs contain undefined namespaces like Xlint in
        // https://repo.maven.apache.org/maven2/com/sun/istack/istack-commons/3.0.11/istack-commons-3.0.11.pom
        XMLInputFactory input = new WstxInputFactory();
        input.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        input.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        xmlMapper = new XmlMapper(new XmlFactory(input, new WstxOutputFactory()))
                .disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    Xml.Document document;

    @With
    RawPom pom;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawMaven rawMaven = (RawMaven) o;
        return getURI().equals(rawMaven.getURI());
    }

    @JsonIgnore
    public URI getURI() {
        return document.getSourcePath();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getURI());
    }

    @Override
    public String toString() {
        return "RawMaven{" +
                "pom=" + pom +
                '}';
    }

    public static RawMaven parse(Parser.Input source, @Nullable URI relativeTo) {
        Xml.Document document = new MavenXmlParser().parseInputs(singletonList(source), relativeTo)
                .iterator().next();

        try {
            return new RawMaven(document, xmlMapper.readValue(source.getSource(), RawPom.class));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse " + source.getUri(), e);
        }
    }

    @JsonIgnore
    public Map<String, String> getActiveProperties() {
        return pom.getActiveProperties();
    }

    @JsonIgnore
    public List<RawPom.Dependency> getActiveDependencies() {
        return pom.getActiveDependencies();
    }

    private static class MavenXmlParser extends XmlParser {
        @Override
        public boolean accept(URI path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    }
}
