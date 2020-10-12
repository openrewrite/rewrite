package org.openrewrite.maven;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openrewrite.Change;
import org.openrewrite.Refactor;
import org.openrewrite.maven.AddOrUpdateDependencyManagement;
import org.openrewrite.maven.MavenParser;

import java.util.ArrayList;
import java.util.List;

public class AddOrUpdateDependencyManagementTest {
    @Test
    public void shouldCreateDependencyManagementWithDependencyWhenNoneExists() {

        String before =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "\n" +
                        "</project>";

        String expected =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.junit.jupiter</groupId>\n" +
                        "                <artifactId>junit-jupiter-api</artifactId>\n" +
                        "                <version>5.6.2</version>\n" +
                        "                <scope>test</scope>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "\n" +
                        "</project>";

        AddOrUpdateDependencyManagement adm = new AddOrUpdateDependencyManagement();
        adm.setGroupId("org.junit.jupiter");
        adm.setArtifactId("junit-jupiter-api");
        adm.setVersion("5.6.2");
        adm.setScope("test");

        MavenParser parser = MavenParser.builder().build();
        List<Change> changes = new ArrayList<>(new Refactor()
                .visit(adm)
                .fix(parser.parse(before)));
        Assertions.assertEquals(1, changes.size());

        String after = changes.get(0).getFixed().print();
        Assertions.assertEquals(expected, after);
    }

    @Test
    public void shouldAddDependencyWhenDependencyManagementAlreadyExists() {

        String before =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.junit.jupiter</groupId>\n" +
                        "                <artifactId>junit-jupiter-api</artifactId>\n" +
                        "                <version>5.6.2</version>\n" +
                        "                <scope>test</scope>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "</project>";

        String expected =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.junit.jupiter</groupId>\n" +
                        "                <artifactId>junit-jupiter-api</artifactId>\n" +
                        "                <version>5.6.2</version>\n" +
                        "                <scope>test</scope>\n" +
                        "            </dependency>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.projectlombok</groupId>\n" +
                        "                <artifactId>lombok</artifactId>\n" +
                        "                <version>1.18.12</version>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "</project>";

        AddOrUpdateDependencyManagement adm = new AddOrUpdateDependencyManagement();
        adm.setGroupId("org.projectlombok");
        adm.setArtifactId("lombok");
        adm.setVersion("1.18.12");

        MavenParser parser = MavenParser.builder().build();
        List<Change> changes = new ArrayList<>(new Refactor()
                .visit(adm)
                .fix(parser.parse(before)));
        Assertions.assertEquals(1, changes.size());

        String after = changes.get(0).getFixed().print();
        Assertions.assertEquals(expected, after);
    }

    @Test
    public void shouldUpdateVersionIfDifferent() {

        String before =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.junit.jupiter</groupId>\n" +
                        "                <artifactId>junit-jupiter-api</artifactId>\n" +
                        "                <version>5.6.2</version>\n" +
                        "                <scope>test</scope>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "\n" +
                        "</project>";

        String expected =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.junit.jupiter</groupId>\n" +
                        "                <artifactId>junit-jupiter-api</artifactId>\n" +
                        "                <version>10.100</version>\n" +
                        "                <scope>test</scope>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "\n" +
                        "</project>";

        AddOrUpdateDependencyManagement adm = new AddOrUpdateDependencyManagement();
        adm.setGroupId("org.junit.jupiter");
        adm.setArtifactId("junit-jupiter-api");
        adm.setVersion("10.100");
        adm.setScope("test");

        MavenParser parser = MavenParser.builder().build();
        List<Change> changes = new ArrayList<>(new Refactor()
                .visit(adm)
                .fix(parser.parse(before)));
        Assertions.assertEquals(1, changes.size());

        String after = changes.get(0).getFixed().print();
        Assertions.assertEquals(expected, after);
    }

    @Test
    public void shouldUpdateScopeIfDifferent() {

        String before =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.junit.jupiter</groupId>\n" +
                        "                <artifactId>junit-jupiter-api</artifactId>\n" +
                        "                <version>5.6.2</version>\n" +
                        "                <scope>compile</scope>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "\n" +
                        "</project>";

        String expected =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.junit.jupiter</groupId>\n" +
                        "                <artifactId>junit-jupiter-api</artifactId>\n" +
                        "                <version>10.100</version>\n" +
                        "                <scope>test</scope>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "\n" +
                        "</project>";

        AddOrUpdateDependencyManagement adm = new AddOrUpdateDependencyManagement();
        adm.setGroupId("org.junit.jupiter");
        adm.setArtifactId("junit-jupiter-api");
        adm.setVersion("10.100");
        adm.setScope("test");

        MavenParser parser = MavenParser.builder().build();
        List<Change> changes = new ArrayList<>(new Refactor()
                .visit(adm)
                .fix(parser.parse(before)));
        Assertions.assertEquals(1, changes.size());

        String after = changes.get(0).getFixed().print();
        Assertions.assertEquals(expected, after);
    }


    @Test
    public void shouldRemoveScopeIfRemoved() {

        String before =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.junit.jupiter</groupId>\n" +
                        "                <artifactId>junit-jupiter-api</artifactId>\n" +
                        "                <version>5.6.2</version>\n" +
                        "                <scope>compile</scope>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "\n" +
                        "</project>";

        String expected =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>org.openrewrite.maven</groupId>\n" +
                        "    <artifactId>dependency-management-example</artifactId>\n" +
                        "    <version>0.1-SNAPSHOT</version>\n" +
                        "    <name>dependency-management-example</name>\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.junit.jupiter</groupId>\n" +
                        "                <artifactId>junit-jupiter-api</artifactId>\n" +
                        "                <version>10.100</version>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "\n" +
                        "</project>";

        AddOrUpdateDependencyManagement adm = new AddOrUpdateDependencyManagement();
        adm.setGroupId("org.junit.jupiter");
        adm.setArtifactId("junit-jupiter-api");
        adm.setVersion("10.100");
        adm.setScope(null);

        MavenParser parser = MavenParser.builder().build();
        List<Change> changes = new ArrayList<>(new Refactor()
                .visit(adm)
                .fix(parser.parse(before)));
        Assertions.assertEquals(1, changes.size());

        String after = changes.get(0).getFixed().print();
        Assertions.assertEquals(expected, after);
    }
}
