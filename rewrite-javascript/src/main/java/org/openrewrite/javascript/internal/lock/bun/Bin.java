/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.bun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Port of bun's src/install/bin.zig {@code Bin} (bun 1.3.10), without the linker: a {@code bin} string, a one
 * entry map, a {@code directories.bin}, or a map of several.
 */
final class Bin {

    enum Tag {
        NONE, FILE, NAMED_FILE, DIR, MAP
    }

    static final Bin NONE = new Bin(Tag.NONE, Collections.<String>emptyList());

    final Tag tag;

    /**
     * file and dir: one string; named file: name and path; map: names and paths alternating.
     */
    final List<String> value;

    Bin(Tag tag, List<String> value) {
        this.tag = tag;
        this.value = value;
    }

    boolean eql(Bin r) {
        if (tag != r.tag) {
            return false;
        }
        if (tag != Tag.MAP) {
            return value.equals(r.value);
        }
        if (value.size() != r.value.size()) {
            return false;
        }
        outer:
        for (int i = 0; i < value.size(); i += 2) {
            for (int j = 0; j < r.value.size(); j += 2) {
                if (value.get(i).equals(r.value.get(j))) {
                    if (!value.get(i + 1).equals(r.value.get(j + 1))) {
                        return false;
                    }
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * {@code parseAppend}: a package's {@code bin} as the text lockfile writes it.
     */
    static Bin parseAppend(Json.Expr binExpr) {
        if (binExpr.isObject()) {
            List<Json.Property> props = binExpr.properties();
            if (props.isEmpty()) {
                return NONE;
            }
            if (props.size() == 1) {
                String binName = props.get(0).key.asString();
                String value = props.get(0).value.asString();
                if (binName == null || value == null) {
                    return NONE;
                }
                List<String> named = new ArrayList<>(2);
                named.add(binName);
                named.add(value);
                return new Bin(Tag.NAMED_FILE, named);
            }
            List<String> map = new ArrayList<>(props.size() * 2);
            for (Json.Property prop : props) {
                String key = prop.key.asString();
                String value = prop.value.asString();
                if (key == null || value == null) {
                    return NONE;
                }
                map.add(key);
                map.add(value);
            }
            return new Bin(Tag.MAP, map);
        }
        String str = binExpr.asString();
        if (str != null && !str.isEmpty()) {
            return new Bin(Tag.FILE, Collections.singletonList(str));
        }
        return NONE;
    }

    static Bin parseAppendFromDirectories(Json.Expr binExpr) {
        String str = binExpr.asString();
        if (str != null) {
            return new Bin(Tag.DIR, Collections.singletonList(str));
        }
        return NONE;
    }

    void toJsonSingleLine(StringBuilder out, BunRelease release) {
        switch (tag) {
            case FILE:
            case DIR:
                Strings.appendJsonString(out, value.get(0), true, release);
                break;
            case NAMED_FILE:
                out.append("{ ");
                Strings.appendJsonString(out, value.get(0), true, release);
                out.append(": ");
                Strings.appendJsonString(out, value.get(1), true, release);
                out.append(" }");
                break;
            case MAP: {
                out.append('{');
                boolean first = true;
                for (int i = 0; i < value.size(); i += 2) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    out.append(' ');
                    Strings.appendJsonString(out, value.get(i), true, release);
                    out.append(": ");
                    Strings.appendJsonString(out, value.get(i + 1), true, release);
                }
                out.append(" }");
                break;
            }
            default:
                break;
        }
    }

    /**
     * {@code toJson(.multi_line)}, before 1.2.3 without a line break after each entry of a named file or a map
     * ({@link BunRelease#writesAWorkspaceBinAnEntryALine()}).
     */
    void toJsonMultiLine(StringBuilder out, int[] indent, BunRelease release) {
        if (!release.writesAWorkspaceBinAnEntryALine() && (tag == Tag.NAMED_FILE || tag == Tag.MAP)) {
            out.append("{\n");
            indent[0]++;
            for (int i = 0; i < value.size(); i += 2) {
                if (i > 0) {
                    out.append(',');
                }
                TextLockfileWriter.writeIndent(out, indent);
                Strings.appendJsonString(out, value.get(i), true, release);
                out.append(": ");
                Strings.appendJsonString(out, value.get(i + 1), true, release);
            }
            if (tag == Tag.NAMED_FILE) {
                out.append('\n');
            }
            indent[0]--;
            TextLockfileWriter.writeIndent(out, indent);
            out.append('}');
            return;
        }
        switch (tag) {
            case FILE:
            case DIR:
                Strings.appendJsonString(out, value.get(0), true, release);
                break;
            case NAMED_FILE:
                out.append("{\n");
                indent[0]++;
                TextLockfileWriter.writeIndent(out, indent);
                Strings.appendJsonString(out, value.get(0), true, release);
                out.append(": ");
                Strings.appendJsonString(out, value.get(1), true, release);
                out.append(",\n");
                indent[0]--;
                TextLockfileWriter.writeIndent(out, indent);
                out.append('}');
                break;
            case MAP: {
                out.append('{');
                indent[0]++;
                boolean any = false;
                for (int i = 0; i < value.size(); i += 2) {
                    if (!any) {
                        any = true;
                        out.append('\n');
                    }
                    TextLockfileWriter.writeIndent(out, indent);
                    Strings.appendJsonString(out, value.get(i), true, release);
                    out.append(": ");
                    Strings.appendJsonString(out, value.get(i + 1), true, release);
                    out.append(",\n");
                }
                if (!any) {
                    out.append('}');
                    indent[0]--;
                    return;
                }
                indent[0]--;
                TextLockfileWriter.writeIndent(out, indent);
                out.append('}');
                break;
            }
            default:
                break;
        }
    }
}
