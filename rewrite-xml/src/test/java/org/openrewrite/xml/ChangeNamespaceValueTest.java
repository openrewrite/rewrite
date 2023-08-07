package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

public class ChangeNamespaceValueTest implements RewriteTest {

    @Test
    void replaceVersion24Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", "http://java.sun.com/xml/ns/j2ee", "2.4")),
          xml(
            """
                <web-app xmlns="http://java.sun.com/xml/ns/javaee" version="2.4"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """,
            """
                <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="2.4"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """
          )
        );
    }

    @Test
    void replaceVersion25Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", "http://java.sun.com/xml/ns/java", "2.5,3.0")),
          xml(
            """
                <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="2.5"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """,
            """
                <web-app xmlns="http://java.sun.com/xml/ns/java" version="2.5"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """
          )
        );
    }

    @Test
    void replaceVersion30Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", "http://java.sun.com/xml/ns/java", "2.5,3.0")),
          xml(
            """
                <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="3.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_0.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """,
            """
                <web-app xmlns="http://java.sun.com/xml/ns/java" version="3.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_0.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """
          )
        );
    }

    @Test
    void replaceVersion31Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", "http://xmlns.jcp.org/xml/ns/javaee", "3.1+")),
          xml(
            """
                <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="3.1"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_1.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """,
            """
                <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="3.1"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_1.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """
          )
        );
    }

    @Test
    void replaceVersion32Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", "http://xmlns.jcp.org/xml/ns/javaee", "3.1+")),
          xml(
            """
                <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="3.2"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_2.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """,
            """
                <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="3.2"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_2.xsd"
                    id="WebApp_ID">
                    <display-name>testWebDDNamespace</display-name>
                </web-app>
            """
          )
        );
    }
}

