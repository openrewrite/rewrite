package org.openrewrite.yaml;

import org.openrewrite.yaml.tree.Yaml;

public class MappingUtils {
    public static boolean keyMatches(Yaml.Mapping.Entry e1, Yaml.Mapping.Entry e2) {
        return e1.getKey().getValue().equals(e2.getKey().getValue());
    }
}
