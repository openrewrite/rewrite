/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.yaml;

import org.openrewrite.yaml.tree.Yaml;

import java.util.HashMap;
import java.util.Map;

public class ReplaceAliasWithAnchorValueVisitor<P> extends YamlVisitor<P> {
    Map<String, Yaml> anchorValues = new HashMap<>();
    @Override
    public Yaml visitMapping(Yaml.Mapping mapping, P p) {
        if (mapping.getAnchor() != null) {
            anchorValues.put(mapping.getAnchor().getKey(), mapping.withAnchor(null));
        }
        return super.visitMapping(mapping, p);
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar, P p) {
        if (scalar.getAnchor() != null) {
            anchorValues.put(scalar.getAnchor().getKey(), scalar.withAnchor(null));
        }
        return super.visitScalar(scalar, p);
    }

    @Override
    public Yaml visitAlias(Yaml.Alias alias, P p) {
        Yaml.Alias al = (Yaml.Alias) super.visitAlias(alias, p);
        Yaml anchorVal = anchorValues.get(al.getAnchor().getKey());
        if (anchorVal != null) {
            return anchorVal;
        }
        return al;
    }
}
