package org.openrewrite.java.internal.template;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JavaTemplateParserTest implements RewriteTest {

    @Test
    void testParseTemplate() {
        rewriteRun(
          spec -> spec
            .recipeFromResources("org.openrewrite.java.spring.security5.UpgradeSpringSecurity_5_8")
            .parser(JavaParser.fromJavaVersion().classpath("spring-core-5.3.+", "spring-context-5.3.+", "spring-beans-5.3.+", "spring-web-5.3.+", "spring-security-web-5.8.+", "spring-security-config-5.8.+", "spring-security-core-5.8.+", "tomcat-embed")),
          java(
            """
            package com.example;
 
            import org.springframework.context.annotation.Configuration;
            import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
            import org.springframework.security.config.annotation.web.builders.HttpSecurity;
            import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
            import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
            import org.springframework.security.config.http.SessionCreationPolicy;
 
            /**
             * Security configuration.
             */
            @Configuration
            @EnableWebSecurity
            public class  WebSecurityConfig extends WebSecurityConfigurerAdapter {
 
                @Configuration
                public static class AdminWebSecurityConfig extends WebSecurityConfigurerAdapter {
 
                    @Override
                    protected void configure( final AuthenticationManagerBuilder auth ) throws Exception {}
 
                    @Override
                    protected void configure( final HttpSecurity http ) throws Exception {
                        http.antMatcher( "***" )
                            .authorizeRequests().anyRequest().hasAuthority( "***" )
                            .and()
                            .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
                    }
                }
            }
            """,
            """
            package com.example;

            import org.springframework.context.annotation.Configuration;
            import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
            import org.springframework.security.config.annotation.web.builders.HttpSecurity;
            import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
            import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
            import org.springframework.security.config.http.SessionCreationPolicy;

            /**
             * Security configuration.
             */
            @Configuration
            @EnableWebSecurity
            public class  WebSecurityConfig {

                @Configuration
                public static class AdminWebSecurityConfig extends WebSecurityConfigurerAdapter {

                    /*~~(Migrate manually based on https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter)~~>*/@Override
                    protected void configure( final AuthenticationManagerBuilder auth ) throws Exception {}

                    @Override
                    protected void configure( final HttpSecurity http ) throws Exception {
                        http.securityMatcher("***")
                                .authorizeHttpRequests(requests -> requests.anyRequest().hasAuthority("***"))
                                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
                    }
                }
            }
            """
          )
        );
    }
}