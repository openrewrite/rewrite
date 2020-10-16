package org.openrewrite.maven.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.text.StringEscapeUtils;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class RawMavenMetadata {
    private static final ObjectMapper xmlMapper = new XmlMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final XmlParser xmlParser = new XmlParser() {
        @Override
        public boolean accept(URI path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    };

    public static final RawMavenMetadata EMPTY = new RawMavenMetadata(new RawMavenMetadata.Versioning(emptyList()));

    Versioning versioning;

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Versioning {
        Collection<String> versions;
    }

    public static RawMavenMetadata parse(byte[] document) {
        try {
            return xmlMapper.readValue(document, RawMavenMetadata.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
