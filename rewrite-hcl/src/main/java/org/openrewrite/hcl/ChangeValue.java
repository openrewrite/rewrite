/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.hcl;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.hcl.tree.Expression;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeValue extends Recipe {

    @Option(displayName = "Key path",
            description = "A JSONPath expression locating the attribute whose value to change.",
            example = "$.resource.aws_db_instance.engine_version")
    String keyPath;

    @Option(displayName = "New value",
            description = "The new value as an HCL expression. String values have to be quoted, e.g. `\"8.0\"`, " +
                          "while `true`, `8`, `[\"a\", \"b\"]` or `var.engine_version` are used as is. " +
                          "If `regex` is enabled, the replacement for the parts of the current value matching " +
                          "`oldValue` instead, keeping its quotes. Capturing groups can be used, e.g. `db.t3.$1`.",
            example = "\"8.0\"")
    String value;

    @Option(displayName = "Old value",
            description = "Only change the value if it matches the configured `oldValue`. It is compared to numbers, " +
                          "booleans and strings without interpolation, the latter without their quotes. Any other " +
                          "value, like a reference, is left unchanged when `oldValue` is set.",
            example = "5.7",
            required = false)
    @Nullable
    String oldValue;

    @Option(displayName = "Regex",
            description = "Default `false`. If enabled, `oldValue` will be interpreted as a Regular Expression, " +
                          "to replace only all parts that match the regex. Capturing group can be used in `value`.",
            required = false)
    @Nullable
    Boolean regex;

    String displayName = "Change value";

    String description = "Change the value of an HCL attribute while leaving its key intact. " +
                         "The new value replaces the whole existing expression, unless `regex` is enabled.";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", keyPath, value);
    }

    @Override
    public Validated<Object> validate() {
        boolean isRegex = Boolean.TRUE.equals(regex);
        return super.validate()
                .and(Validated.test("value", "must be a single HCL expression", value,
                        v -> v == null || isRegex || parse(v) != null))
                .and(Validated.test("oldValue", "is required if `regex` is enabled", oldValue,
                        v -> !(isRegex && StringUtils.isNullOrEmpty(v))))
                .and(Validated.test("oldValue", "must be a valid regular expression", oldValue,
                        v -> !isRegex || v == null || isValidPattern(v)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(keyPath);
        boolean isRegex = Boolean.TRUE.equals(regex);
        if (isRegex && StringUtils.isNullOrEmpty(oldValue)) {
            return TreeVisitor.noop();
        }
        Pattern pattern = isRegex ? Pattern.compile(oldValue) : null;
        Expression newValue = isRegex ? null : parse(value);
        if (!isRegex && newValue == null) {
            return TreeVisitor.noop();
        }
        return new HclIsoVisitor<ExecutionContext>() {
            @Override
            public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, ExecutionContext ctx) {
                Hcl.Attribute a = super.visitAttribute(attribute, ctx);
                if (!matcher.matches(getCursor())) {
                    return a;
                }
                Expression replacement = replacement(a.getValue(), newValue, pattern);
                if (replacement == null) {
                    return a;
                }
                Expression v = ((Expression) new RandomizeIdVisitor<Integer>().visitNonNull(replacement, 0))
                        .withPrefix(a.getValue().getPrefix());
                if (!containsHeredoc(v)) {
                    v = autoFormat(v, ctx, new Cursor(getCursor().getParentOrThrow(), a.withValue(v)));
                }
                return v.printTrimmed(getCursor()).equals(a.getValue().printTrimmed(getCursor())) ? a : a.withValue(v);
            }
        };
    }

    private @Nullable Expression replacement(Expression current, @Nullable Expression newValue,
                                             @Nullable Pattern pattern) {
        if (pattern == null && StringUtils.isNullOrEmpty(oldValue)) {
            return newValue;
        }
        String source = simpleValueSource(current);
        if (source == null) {
            return null;
        }
        if (pattern == null) {
            return source.equals(oldValue) ? newValue : null;
        }
        if (!pattern.matcher(source).find()) {
            return null;
        }
        String replaced = pattern.matcher(source).replaceAll(value);
        return parse(current instanceof Hcl.QuotedTemplate ? '"' + replaced + '"' : replaced);
    }

    private static @Nullable String simpleValueSource(Expression value) {
        if (value instanceof Hcl.Literal) {
            return ((Hcl.Literal) value).getValueSource();
        } else if (value instanceof Hcl.QuotedTemplate) {
            List<Expression> parts = ((Hcl.QuotedTemplate) value).getExpressions();
            if (parts.isEmpty()) {
                return "";
            } else if (parts.size() == 1 && parts.get(0) instanceof Hcl.Literal) {
                return ((Hcl.Literal) parts.get(0)).getValueSource();
            }
        }
        return null;
    }

    private static boolean isValidPattern(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private static boolean containsHeredoc(Expression expression) {
        AtomicBoolean found = new AtomicBoolean();
        new HclIsoVisitor<AtomicBoolean>() {
            @Override
            public Hcl.HeredocTemplate visitHeredocTemplate(Hcl.HeredocTemplate heredocTemplate, AtomicBoolean found) {
                found.set(true);
                return heredocTemplate;
            }
        }.visit(expression, found);
        return found.get();
    }

    private static @Nullable Expression parse(String value) {
        return HclParser.builder().build()
                .parse(new InMemoryExecutionContext(), "value = " + value + "\n")
                .filter(Hcl.ConfigFile.class::isInstance)
                .map(Hcl.ConfigFile.class::cast)
                .filter(c -> c.getEof().getWhitespace().trim().isEmpty() &&
                             c.getBody().size() == 1 && c.getBody().get(0) instanceof Hcl.Attribute)
                .map(c -> ((Hcl.Attribute) c.getBody().get(0)).getValue())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
