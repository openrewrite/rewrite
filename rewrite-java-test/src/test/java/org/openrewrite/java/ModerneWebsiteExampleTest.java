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
package org.openrewrite.java;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generates the LST example data behind the LST page on www.moderne.ai, and asserts the
 * type-attribution chain it relies on: {@code new AllowAllHostnameVerifier()} resolves up
 * through Apache HttpClient to the {@code javax.net.ssl.HostnameVerifier#verify(..)} contract
 * it disables — a chain spanning user code, a library, and the JDK that text/dataflow can't see.
 * <p>
 * {@code @Disabled} to stay out of CI. To refresh the data: remove {@code @Disabled}, run the
 * test, then copy {@code build/moderne-website-data/*} into the website's {@code src/data}.
 */
class ModerneWebsiteExampleTest {

    static final String SOURCE =
            "import org.apache.http.conn.ssl.AllowAllHostnameVerifier;\n" +
            "import javax.net.ssl.HostnameVerifier;\n" +
            "\n" +
            "class TlsConfig {\n" +
            "\n" +
            "    // accept a certificate issued for ANY host\n" +
            "    HostnameVerifier hostnameVerifier() {\n" +
            "        return new AllowAllHostnameVerifier();\n" +
            "    }\n" +
            "}\n";

    static final String INSECURE_FQN = "org.apache.http.conn.ssl.AllowAllHostnameVerifier";
    static final String CONTRACT_FQN = "javax.net.ssl.HostnameVerifier";

    static final Path OUT = Path.of("build/moderne-website-data");

    @Disabled("Run on demand to regenerate the moderne.ai LST example data; excluded from CI")
    @Test
    void dump() throws IOException {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        SourceFile sf = JavaParser.fromJavaVersion()
                .classpathFromResources(ctx, "httpclient")
                .build()
                .parse(ctx, SOURCE)
                .findFirst()
                .orElseThrow();
        J.CompilationUnit cu = (J.CompilationUnit) sf;

        Dumper d = new Dumper();
        d.visit(cu, 0);
        d.buildTypeGraph();
        d.buildDense();
        d.buildWalk();

        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve("lst-real.json"), d.toJson());
        Files.writeString(OUT.resolve("lst-dense.json"), d.toDenseJson());
        Files.writeString(OUT.resolve("lst-tree.txt"), d.tree.toString());

        String summary = "syntaxNodes=" + d.nodes.size()
                + " childEdges=" + d.childEdges.size()
                + " spaceObjects=" + d.spaceCount
                + " whitespaceChars=" + d.wsChars
                + " comments=" + d.commentCount
                + " markers=" + d.markerCount + d.markerKinds
                + " typeEntryPoints=" + d.typeEntries.size()
                + " typesReachable=" + d.reachableTypeCount
                + " typeEdgesReachable=" + d.reachableEdgeCount
                + " recordedTypeNodes=" + d.typeNodes.size()
                + " recordedTypeEdges=" + d.typeEdges.size();
        Files.writeString(OUT.resolve("lst-summary.txt"), summary + "\n");
        System.out.println("\n===LST SUMMARY===\n" + summary + "\n=================\n");

        assertThat(d.nodes).isNotEmpty();
        assertThat(d.insecureType)
                .as("new AllowAllHostnameVerifier() resolved to its real library type")
                .isNotNull();
        assertThat(d.insecureType.getFullyQualifiedName()).isEqualTo(INSECURE_FQN);

        List<JavaType.FullyQualified> chain = d.pathTo(d.insecureType, CONTRACT_FQN);
        assertThat(chain)
                .as("the LST resolves the verifier up to the javax.net.ssl.HostnameVerifier contract")
                .isNotNull();
        assertThat(chain).extracting(JavaType.FullyQualified::getFullyQualifiedName)
                .containsExactly(
                        "org.apache.http.conn.ssl.AllowAllHostnameVerifier",
                        "org.apache.http.conn.ssl.AbstractVerifier",
                        "org.apache.http.conn.ssl.X509HostnameVerifier",
                        "javax.net.ssl.HostnameVerifier");
        assertThat(chain.get(chain.size() - 1).getMethods())
                .as("the disabled hostname-verification contract method verify(..)")
                .anyMatch(m -> m.getName().equals("verify"));
    }

    static final class Dumper extends JavaVisitor<Integer> {
        record N(String id, String cat, String kind, String label) {}
        record E(String from, String to, String role) {}
        record TN(String id, String kind, String label, String fqn) {}
        record DN(String id, String kind, String label, int depth) {}
        record WS(String id, String label, String kind, String role) {}

        final List<N> nodes = new ArrayList<>();
        final List<E> childEdges = new ArrayList<>();
        final List<E> typeAttr = new ArrayList<>();
        final List<TN> typeNodes = new ArrayList<>();
        final List<E> typeEdges = new ArrayList<>();
        final List<DN> denseNodes = new ArrayList<>();
        final List<E> denseEdges = new ArrayList<>();
        final List<WS> walk = new ArrayList<>();
        final StringBuilder tree = new StringBuilder();

        final Deque<String> parentStack = new ArrayDeque<>();
        final IdentityHashMap<JavaType, String> typeIds = new IdentityHashMap<>();
        final Map<JavaType, Boolean> typeEntries = new IdentityHashMap<>();
        final Set<String> recordedTypeNodes = new HashSet<>();
        int typeSeq = 0;

        int spaceCount, wsChars, commentCount, markerCount;
        final TreeSet<String> markerKinds = new TreeSet<>();
        int reachableTypeCount, reachableEdgeCount;
        JavaType.FullyQualified insecureType;

        String typeId(JavaType t) {
            return typeIds.computeIfAbsent(t, x -> "t" + (typeSeq++));
        }

        @Override
        public J visit(Tree t, Integer p) {
            if (!(t instanceof J)) {
                return (J) super.visit(t, p);
            }
            J j = (J) t;
            String id = j.getId().toString();
            String kind = nodeKind(j);
            String label = labelFor(j);
            nodes.add(new N(id, "syntax", kind, label));
            if (!parentStack.isEmpty()) {
                childEdges.add(new E(parentStack.peek(), id, "child"));
            }

            tree.append("  ".repeat(parentStack.size())).append(kind);
            if (!label.isEmpty()) tree.append("  ").append(label);
            String ts = typeShort(j);
            if (ts != null) tree.append("   :: ").append(ts);
            tree.append('\n');

            recordTypeAttr(j, id);

            parentStack.push(id);
            J r = (J) super.visit(t, p);
            parentStack.pop();
            return r;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, Integer p) {
            if (space != null) {
                spaceCount++;
                if (space.getWhitespace() != null) wsChars += space.getWhitespace().length();
                commentCount += space.getComments().size();
            }
            return super.visitSpace(space, loc, p);
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, Integer p) {
            markerCount++;
            markerKinds.add(marker.getClass().getSimpleName());
            return super.visitMarker(marker, p);
        }

        void recordTypeAttr(J j, String id) {
            if (j instanceof TypedTree tt && tt.getType() != null) {
                addEntry(tt.getType());
                typeAttr.add(new E(id, typeId(tt.getType()), "type"));
            }
            if (j instanceof J.MethodInvocation mi && mi.getMethodType() != null) {
                addEntry(mi.getMethodType());
                typeAttr.add(new E(id, typeId(mi.getMethodType()), "methodType"));
            }
            // capture the insecure library type: new AllowAllHostnameVerifier()
            if (j instanceof TypedTree tt2 && tt2.getType() instanceof JavaType.FullyQualified fq
                    && INSECURE_FQN.equals(fq.getFullyQualifiedName())) {
                insecureType = fq;
            }
            if (j instanceof J.MethodDeclaration md && md.getMethodType() != null) {
                addEntry(md.getMethodType());
                typeAttr.add(new E(id, typeId(md.getMethodType()), "methodType"));
            }
            if (j instanceof J.NewClass nc && nc.getConstructorType() != null) {
                addEntry(nc.getConstructorType());
                typeAttr.add(new E(id, typeId(nc.getConstructorType()), "constructor"));
            }
            if (j instanceof J.VariableDeclarations.NamedVariable nv && nv.getVariableType() != null) {
                addEntry(nv.getVariableType());
                typeAttr.add(new E(id, typeId(nv.getVariableType()), "variableType"));
            }
        }

        void addEntry(JavaType t) {
            typeEntries.put(t, Boolean.TRUE);
            typeId(t);
        }

        void buildTypeGraph() {
            // full reachable closure (counts only)
            Set<JavaType> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            Deque<JavaType> q = new ArrayDeque<>(typeEntries.keySet());
            int edges = 0;
            while (!q.isEmpty()) {
                JavaType t = q.poll();
                if (!seen.add(t)) continue;
                for (Map.Entry<String, JavaType> nb : neighborsFull(t)) {
                    edges++;
                    if (nb.getValue() != null && !seen.contains(nb.getValue())) q.add(nb.getValue());
                }
            }
            reachableTypeCount = seen.size();
            reachableEdgeCount = edges;

            // depth-bounded structural subgraph, recorded for the graphic
            final int MAX_DEPTH = 3;
            IdentityHashMap<JavaType, Integer> depth = new IdentityHashMap<>();
            Deque<JavaType> rq = new ArrayDeque<>();
            for (JavaType e : typeEntries.keySet()) { depth.put(e, 0); rq.add(e); }
            while (!rq.isEmpty()) {
                JavaType t = rq.poll();
                int dd = depth.get(t);
                recordTypeNode(t);
                if (dd >= MAX_DEPTH) continue;
                for (Map.Entry<String, JavaType> nb : neighborsStructural(t)) {
                    JavaType to = nb.getValue();
                    if (to == null) continue;
                    typeEdges.add(new E(typeId(t), typeId(to), nb.getKey()));
                    if (!depth.containsKey(to)) { depth.put(to, dd + 1); rq.add(to); }
                }
            }
        }

        void recordTypeNode(JavaType t) {
            String id = typeId(t);
            if (!recordedTypeNodes.add(id)) return;
            typeNodes.add(new TN(id, typeKind(t), typeLabel(t), fqnOf(t)));
        }

        // bounded BFS over the reachable type graph -> the "density" slice the hero renders
        void buildDense() {
            final int MAX_NODES = 1900, MAX_EDGES = 5200;
            IdentityHashMap<JavaType, Integer> depth = new IdentityHashMap<>();
            Set<JavaType> recorded = Collections.newSetFromMap(new IdentityHashMap<>());
            Deque<JavaType> q = new ArrayDeque<>();
            for (JavaType e : typeEntries.keySet()) { depth.put(e, 0); q.add(e); }
            List<JavaType> order = new ArrayList<>();
            while (!q.isEmpty() && order.size() < MAX_NODES) {
                JavaType t = q.poll();
                if (!recorded.add(t)) continue;
                order.add(t);
                denseNodes.add(new DN(typeId(t), typeKind(t), typeLabel(t), depth.get(t)));
                int d = depth.get(t);
                for (Map.Entry<String, JavaType> nb : neighborsFull(t)) {
                    JavaType to = nb.getValue();
                    if (to != null && !depth.containsKey(to)) { depth.put(to, d + 1); q.add(to); }
                }
            }
            for (JavaType t : order) {
                if (denseEdges.size() >= MAX_EDGES) break;
                for (Map.Entry<String, JavaType> nb : neighborsFull(t)) {
                    if (denseEdges.size() >= MAX_EDGES) break;
                    if (nb.getValue() != null && recorded.contains(nb.getValue()))
                        denseEdges.add(new E(typeId(t), typeId(nb.getValue()), nb.getKey()));
                }
            }
        }

        // resolve the verifier up its hierarchy to the HostnameVerifier#verify(..) it disables
        void buildWalk() {
            if (insecureType == null) return;
            List<JavaType.FullyQualified> path = pathTo(insecureType, CONTRACT_FQN);
            if (path == null) return;
            for (int i = 0; i < path.size(); i++) {
                JavaType.FullyQualified t = path.get(i);
                String role;
                if (i == 0) role = "insecure";
                else {
                    JavaType.FullyQualified prev = path.get(i - 1);
                    role = (prev.getKind() == JavaType.FullyQualified.Kind.Interface || prev.getSupertype() == t)
                            ? "extends" : "implements";
                }
                walk.add(new WS(typeId(t), typeLabel(t), typeKind(t), role));
            }
            JavaType.FullyQualified contract = path.get(path.size() - 1);
            for (JavaType.Method m : contract.getMethods())
                if (m.getName().equals("verify")) {
                    walk.add(new WS(typeId(m), typeLabel(m), typeKind(m), "disables"));
                    break;
                }
        }

        // shortest path up extends + implements from `from` to the type whose FQN == targetFqn
        List<JavaType.FullyQualified> pathTo(JavaType.FullyQualified from, String targetFqn) {
            IdentityHashMap<JavaType.FullyQualified, JavaType.FullyQualified> parent = new IdentityHashMap<>();
            Set<JavaType.FullyQualified> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            Deque<JavaType.FullyQualified> q = new ArrayDeque<>();
            q.add(from); seen.add(from);
            JavaType.FullyQualified target = null;
            while (!q.isEmpty()) {
                JavaType.FullyQualified t = q.poll();
                if (targetFqn.equals(t.getFullyQualifiedName())) { target = t; break; }
                List<JavaType.FullyQualified> ups = new ArrayList<>();
                if (t.getSupertype() != null) ups.add(t.getSupertype());
                ups.addAll(t.getInterfaces());
                for (JavaType.FullyQualified u : ups)
                    if (u != null && seen.add(u)) { parent.put(u, t); q.add(u); }
            }
            if (target == null) return null;
            List<JavaType.FullyQualified> path = new ArrayList<>();
            for (JavaType.FullyQualified t = target; t != null; t = parent.get(t)) path.add(t);
            Collections.reverse(path);
            return path;
        }

        String toDenseJson() {
            StringBuilder b = new StringBuilder();
            b.append("{\n  \"summary\": {")
                    .append("\"typesReachable\":").append(reachableTypeCount)
                    .append(",\"typeEdgesReachable\":").append(reachableEdgeCount)
                    .append(",\"denseNodes\":").append(denseNodes.size())
                    .append(",\"denseEdges\":").append(denseEdges.size())
                    .append("},\n  \"nodes\": [\n");
            for (int i = 0; i < denseNodes.size(); i++) {
                DN n = denseNodes.get(i);
                b.append("    {\"id\":").append(js(n.id)).append(",\"kind\":").append(js(n.kind))
                        .append(",\"label\":").append(js(n.label)).append(",\"depth\":").append(n.depth).append("}")
                        .append(i < denseNodes.size() - 1 ? ",\n" : "\n");
            }
            b.append("  ],\n  \"edges\": [\n");
            for (int i = 0; i < denseEdges.size(); i++) {
                E e = denseEdges.get(i);
                b.append("    {\"from\":").append(js(e.from)).append(",\"to\":").append(js(e.to))
                        .append(",\"role\":").append(js(e.role)).append("}").append(i < denseEdges.size() - 1 ? ",\n" : "\n");
            }
            b.append("  ],\n  \"walk\": [\n");
            for (int i = 0; i < walk.size(); i++) {
                WS w = walk.get(i);
                b.append("    {\"id\":").append(js(w.id)).append(",\"label\":").append(js(w.label))
                        .append(",\"kind\":").append(js(w.kind)).append(",\"role\":").append(js(w.role)).append("}")
                        .append(i < walk.size() - 1 ? ",\n" : "\n");
            }
            b.append("  ]\n}\n");
            return b.toString();
        }

        List<Map.Entry<String, JavaType>> neighborsStructural(JavaType t) {
            List<Map.Entry<String, JavaType>> out = new ArrayList<>();
            if (t instanceof JavaType.Variable v) {
                add(out, "owner", v.getOwner());
                add(out, "type", v.getType());
            } else if (t instanceof JavaType.Method m) {
                add(out, "declaringType", m.getDeclaringType());
                add(out, "returns", m.getReturnType());
                for (JavaType pt : m.getParameterTypes()) add(out, "param", pt);
            } else if (t instanceof JavaType.Parameterized par) {
                add(out, "raw", par.getType());
                for (JavaType tp : par.getTypeParameters()) add(out, "typeArg", tp);
            } else if (t instanceof JavaType.Array a) {
                add(out, "element", a.getElemType());
            } else if (t instanceof JavaType.GenericTypeVariable g) {
                for (JavaType b : g.getBounds()) add(out, "bound", b);
            } else if (t instanceof JavaType.FullyQualified fq) {
                add(out, "extends", fq.getSupertype());
                for (JavaType.FullyQualified i : fq.getInterfaces()) add(out, "implements", i);
                for (JavaType tp : fq.getTypeParameters()) add(out, "typeParam", tp);
            }
            return out;
        }

        // structural edges plus a type's own methods/members (the full reachable universe)
        List<Map.Entry<String, JavaType>> neighborsFull(JavaType t) {
            List<Map.Entry<String, JavaType>> out = neighborsStructural(t);
            if (t instanceof JavaType.FullyQualified fq) {
                for (JavaType.Method m : fq.getMethods()) add(out, "method", m);
                for (JavaType.Variable v : fq.getMembers()) add(out, "member", v);
            }
            return out;
        }

        void add(List<Map.Entry<String, JavaType>> out, String role, JavaType t) {
            if (t != null) out.add(Map.entry(role, t));
        }

        static String nodeKind(J j) {
            String n = j.getClass().getName();
            int dollar = n.lastIndexOf('$');
            return "J." + (dollar >= 0 ? n.substring(dollar + 1) : n.substring(n.lastIndexOf('.') + 1));
        }

        static String labelFor(J j) {
            if (j instanceof J.Identifier i) return q(i.getSimpleName());
            if (j instanceof J.Literal l) return q(String.valueOf(l.getValueSource()));
            if (j instanceof J.MethodInvocation mi) return q(mi.getName().getSimpleName() + "()");
            if (j instanceof J.MethodDeclaration md) return q(md.getSimpleName() + "()");
            if (j instanceof J.ClassDeclaration cd) return q("class " + cd.getSimpleName());
            if (j instanceof J.VariableDeclarations.NamedVariable nv) return q(nv.getSimpleName());
            if (j instanceof J.Primitive pr) return q(String.valueOf(pr.getType()));
            if (j instanceof J.Import im) return q(im.getTypeName());
            return "";
        }

        static String q(String s) { return "“" + s + "”"; }

        static String typeShort(J j) {
            if (j instanceof TypedTree tt && tt.getType() != null) return typeLabel(tt.getType());
            return null;
        }

        static String typeKind(JavaType t) {
            String n = t.getClass().getName();
            int dollar = n.lastIndexOf('$');
            return "JavaType." + (dollar >= 0 ? n.substring(dollar + 1) : n.substring(n.lastIndexOf('.') + 1));
        }

        static String shortName(String fqn) {
            if (fqn == null) return "?";
            int lt = fqn.indexOf('<');
            String base = lt >= 0 ? fqn.substring(0, lt) : fqn;
            int dot = base.lastIndexOf('.');
            return dot >= 0 ? base.substring(dot + 1) : base;
        }

        static String typeLabel(JavaType t) {
            if (t instanceof JavaType.Primitive p) return p.getKeyword();
            if (t instanceof JavaType.Parameterized par) {
                StringBuilder sb = new StringBuilder(shortName(par.getType().getFullyQualifiedName())).append('<');
                List<JavaType> tp = par.getTypeParameters();
                for (int i = 0; i < tp.size(); i++) { if (i > 0) sb.append(", "); sb.append(typeLabel(tp.get(i))); }
                return sb.append('>').toString();
            }
            if (t instanceof JavaType.Class c) return shortName(c.getFullyQualifiedName());
            if (t instanceof JavaType.Method m) return shortName(m.getDeclaringType().getFullyQualifiedName()) + "#" + m.getName() + "()";
            if (t instanceof JavaType.Variable v) return v.getName();
            if (t instanceof JavaType.Array a) return typeLabel(a.getElemType()) + "[]";
            if (t instanceof JavaType.GenericTypeVariable g) return g.getName();
            return typeKind(t);
        }

        static String fqnOf(JavaType t) {
            if (t instanceof JavaType.FullyQualified fq) return fq.getFullyQualifiedName();
            if (t instanceof JavaType.Method m) return m.getDeclaringType().getFullyQualifiedName() + "#" + m.getName();
            if (t instanceof JavaType.Variable v) return (v.getOwner() instanceof JavaType.FullyQualified o ? o.getFullyQualifiedName() : "?") + "#" + v.getName();
            return typeLabel(t);
        }

        String toJson() {
            StringBuilder b = new StringBuilder();
            b.append("{\n");
            b.append("  \"source\": ").append(js(SOURCE)).append(",\n");
            b.append("  \"summary\": {")
                    .append("\"syntaxNodes\":").append(nodes.size())
                    .append(",\"childEdges\":").append(childEdges.size())
                    .append(",\"spaceObjects\":").append(spaceCount)
                    .append(",\"whitespaceChars\":").append(wsChars)
                    .append(",\"comments\":").append(commentCount)
                    .append(",\"markers\":").append(markerCount)
                    .append(",\"typeEntryPoints\":").append(typeEntries.size())
                    .append(",\"typesReachable\":").append(reachableTypeCount)
                    .append(",\"typeEdgesReachable\":").append(reachableEdgeCount)
                    .append("},\n");
            b.append("  \"syntax\": {\n    \"nodes\": [\n");
            for (int i = 0; i < nodes.size(); i++) {
                N n = nodes.get(i);
                b.append("      {\"id\":").append(js(n.id)).append(",\"kind\":").append(js(n.kind))
                        .append(",\"label\":").append(js(n.label)).append("}").append(i < nodes.size() - 1 ? ",\n" : "\n");
            }
            b.append("    ],\n    \"edges\": [\n");
            for (int i = 0; i < childEdges.size(); i++) {
                E e = childEdges.get(i);
                b.append("      {\"from\":").append(js(e.from)).append(",\"to\":").append(js(e.to)).append("}")
                        .append(i < childEdges.size() - 1 ? ",\n" : "\n");
            }
            b.append("    ]\n  },\n");
            b.append("  \"typeAttribution\": [\n");
            for (int i = 0; i < typeAttr.size(); i++) {
                E e = typeAttr.get(i);
                b.append("    {\"from\":").append(js(e.from)).append(",\"to\":").append(js(e.to))
                        .append(",\"role\":").append(js(e.role)).append("}").append(i < typeAttr.size() - 1 ? ",\n" : "\n");
            }
            b.append("  ],\n");
            b.append("  \"types\": {\n    \"nodes\": [\n");
            for (int i = 0; i < typeNodes.size(); i++) {
                TN n = typeNodes.get(i);
                b.append("      {\"id\":").append(js(n.id)).append(",\"kind\":").append(js(n.kind))
                        .append(",\"label\":").append(js(n.label)).append(",\"fqn\":").append(js(n.fqn)).append("}")
                        .append(i < typeNodes.size() - 1 ? ",\n" : "\n");
            }
            b.append("    ],\n    \"edges\": [\n");
            for (int i = 0; i < typeEdges.size(); i++) {
                E e = typeEdges.get(i);
                b.append("      {\"from\":").append(js(e.from)).append(",\"to\":").append(js(e.to))
                        .append(",\"role\":").append(js(e.role)).append("}").append(i < typeEdges.size() - 1 ? ",\n" : "\n");
            }
            b.append("    ]\n  }\n}\n");
            return b.toString();
        }

        static String js(String s) {
            if (s == null) return "null";
            StringBuilder b = new StringBuilder("\"");
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> b.append("\\\"");
                    case '\\' -> b.append("\\\\");
                    case '\n' -> b.append("\\n");
                    case '\r' -> b.append("\\r");
                    case '\t' -> b.append("\\t");
                    default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
                }
            }
            return b.append('"').toString();
        }
    }
}
