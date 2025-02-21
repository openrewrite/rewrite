package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.semver.LatestRelease;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class AddDependencyVisitorTest {

    @Test
    void shouldHonorMetadataLastRelease() throws MavenDownloadingException {
        // given
        AddDependencyVisitor.MavenMetadataSupplier metadataSupplier = () -> new MavenMetadata(new MavenMetadata.Versioning(
                  "1.0.1RC",
                  "1.0.0",
                  null,null, null, null
                ));
        LatestRelease comparator = new LatestRelease(null);

        // when
        String version = AddDependencyVisitor.findVersionToUse(comparator, "latest.release", metadataSupplier, null, null);

        // test
        assertThat(version).isEqualTo("1.0.0");
    }
}
