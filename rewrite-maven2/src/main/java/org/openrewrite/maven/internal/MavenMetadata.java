package org.openrewrite.maven.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.xml.XmlParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collection;

import static java.util.Collections.emptyList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class MavenMetadata {
    private static final ObjectMapper xmlMapper = new XmlMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final XmlParser xmlParser = new XmlParser() {
        @Override
        public boolean accept(URI path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    };

    public static final MavenMetadata EMPTY = new MavenMetadata(new MavenMetadata.Versioning(emptyList()));

    Versioning versioning;

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Versioning {
        Collection<String> versions;
    }

    public static MavenMetadata parse(byte[] document) {
        try {
            return xmlMapper.readValue(document, MavenMetadata.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
