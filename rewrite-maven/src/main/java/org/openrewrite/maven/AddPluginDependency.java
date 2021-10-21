package org.openrewrite.maven;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.tree.Xml.Tag;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddPluginDependency extends Recipe {

    private static final XPathMatcher PLUGIN_MATCHER = new XPathMatcher("/project/build/plugins/plugin");
    private static final String FOUND_DEPENDENCY_MSG = "plugin-dependency-found";

    private String pluginGroupId;
    private String pluginArtifactId;

    private String groupId;
    private String artifactId;
    private String version;

    @Override
    public String getDisplayName() {
        return "Add dependency to plugin " + pluginGroupId + ":" + pluginArtifactId;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddPluginDependencyVisitor();
    }

    private class AddPluginDependencyVisitor extends MavenVisitor {

        @Override
        public Xml visitTag(Tag tag, ExecutionContext ctx) {
            if (PLUGIN_MATCHER.matches(getCursor()) && hasGroupAndArtifact(pluginGroupId, pluginArtifactId)) {
                Tag dependencies = tag.getChild("dependencies").orElse(null);
                if (dependencies != null) {
                    doAfterVisit(new DependencyExistVisitor(dependencies));
                } else {
                    doAfterVisit(new AddToTagVisitor<>(tag, Tag.build("<dependencies/>"), null));
                }
                doAfterVisit(new AddDependencyTagVisitor(tag));
            }
            return super.visitTag(tag, ctx);
        }

        private boolean hasGroupAndArtifact(String groupId, String artifactId) {
            Tag tag = getCursor().getValue();
            return groupId.equals(tag.getChildValue("groupId").orElse(model.getGroupId())) &&
                    tag.getChildValue("artifactId")
                            .map(a -> a.equals(artifactId))
                            .orElse(artifactId == null);
        }

    }

    private class AddDependencyTagVisitor extends MavenVisitor {

        private final Tag scope;

        public AddDependencyTagVisitor(Tag tag) {
            this.scope = tag;
        }

        @Override
        public Xml visitTag(Tag tag, ExecutionContext ctx) {
            // Any tag
            if (getCursor().isScopeInPath(scope)) {
                if ("dependencies".equals(tag.getName())) {
                    if (!Boolean.TRUE.equals(ctx.getMessage(FOUND_DEPENDENCY_MSG))) {
                        Tag dependencyTag = Tag.build(
                                "\n<dependency>\n" +
                                        "<groupId>" + groupId + "</groupId>\n" +
                                        "<artifactId>" + artifactId + "</artifactId>\n" +
                                        (version == null ? "" :
                                                "<version>" + version + "</version>\n") +
                                        "</dependency>"
                        );

                        doAfterVisit(new AddToTagVisitor<>(tag, dependencyTag,
                                new InsertDependencyComparator(tag.getChildren(), dependencyTag)));

                    }
                }
            }

            return super.visitTag(tag, ctx);
        }

    }

}
