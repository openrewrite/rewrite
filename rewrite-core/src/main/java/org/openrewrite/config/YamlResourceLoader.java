package org.openrewrite.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openrewrite.SourceVisitor;
import org.openrewrite.Validated;
import org.openrewrite.ValidationException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

import static org.openrewrite.Validated.required;
import static org.openrewrite.Validated.test;

public class YamlResourceLoader implements ProfileConfigurationLoader, SourceVisitorLoader {
    private static final ObjectMapper propertyConverter = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Map<String, ProfileConfiguration> profiles = new HashMap<>();
    private final Collection<SourceVisitor<?>> visitors = new ArrayList<>();

    public YamlResourceLoader(InputStream yamlInput) {
        try (yamlInput) {
            Yaml yaml = new Yaml();
            for (Object resource : yaml.loadAll(yamlInput)) {
                if (resource instanceof Map) {
                    @SuppressWarnings("unchecked") Map<String, Object> resourceMap = (Map<String, Object>) resource;
                    String type = resourceMap.getOrDefault("type", "invalid").toString();
                    switch(type) {
                        case "beta.openrewrite.org/v1/visitor":
                            mapVisitor(resourceMap);
                            break;
                        case "beta.openrewrite.org/v1/profile":
                            mapProfile(resourceMap);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void mapVisitor(Map<String, Object> visitorMap) {
        Validated validation = required("name", visitorMap.get("name"))
                .and(required("visitors", visitorMap.get("visitors")))
                .and(test("visitors",
                        "must be a list",
                        visitorMap.get("visitors"),
                        visitors -> visitors instanceof List));

        if (validation.isInvalid()) {
            throw new ValidationException(validation);
        }

        List<SourceVisitor<Object>> subVisitors = new ArrayList<>();

        //noinspection unchecked
        for (Object subVisitorNameAndConfig : (List<Object>) visitorMap.get("visitors")) {
            try {
                if (subVisitorNameAndConfig instanceof String) {
                    //noinspection unchecked
                    subVisitors.add((SourceVisitor<Object>) visitorClass((String) subVisitorNameAndConfig)
                            .getDeclaredConstructor().newInstance());
                } else if (subVisitorNameAndConfig instanceof Map) {
                    //noinspection unchecked
                    for (Map.Entry<String, Object> subVisitorEntry : ((Map<String, Object>) subVisitorNameAndConfig)
                            .entrySet()) {
                        @SuppressWarnings("unchecked") SourceVisitor<Object> subVisitor =
                                (SourceVisitor<Object>) visitorClass(subVisitorEntry.getKey())
                                        .getDeclaredConstructor().newInstance();

                        propertyConverter.updateValue(subVisitor, subVisitorEntry.getValue());

                        subVisitors.add(subVisitor);
                    }
                }
            } catch (Exception e) {
                throw new ValidationException(
                        Validated.invalid("visitor", subVisitorNameAndConfig, "must be constructable", e)
                );
            }
        }

        this.visitors.add(new CompositeSourceVisitor(visitorMap.get("name").toString(), subVisitors));
    }

    private Class<?> visitorClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return Class.forName("org.openrewrite." + name);
        }
    }

    private void mapProfile(Map<String, Object> profileMap) {
        ProfileConfiguration profile = new ProfileConfiguration();
        try {
            propertyConverter.updateValue(profile, profileMap);
        } catch (JsonMappingException e) {
            throw new ValidationException(Validated.invalid("profile", profileMap,
                    "must be a valid profile configuration", e));
        }

        profiles.compute(profile.getName(), (name, existing) -> profile.merge(existing));
    }

    @Override
    public Collection<ProfileConfiguration> loadProfiles() {
        return profiles.values();
    }

    @Override
    public Collection<SourceVisitor<?>> loadVisitors() {
        return visitors;
    }
}
