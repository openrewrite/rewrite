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
package org.openrewrite.coldfusion.internal;

import org.openrewrite.java.tree.JavaType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The types of declaration attributes. One the language defines is named for the tag the declaration is in tag
 * syntax, as {@code cfcomponent.accessors}. Any other is metadata a framework reads, named for itself, as WireBox's
 * {@code singleton}.
 */
final class LanguageAttributes {
    static final String COMPONENT = "cfcomponent";
    static final String INTERFACE = "cfinterface";
    static final String FUNCTION = "cffunction";
    static final String ARGUMENT = "cfargument";
    static final String PROPERTY = "cfproperty";

    /**
     * Each tag's attributes by their name in lower case, to the name its type has, which differs for an alias.
     */
    private static final Map<String, Map<String, String>> ATTRIBUTES = new HashMap<>();

    // Lucee's core tag library (core-base.tld) first, then the attributes only Adobe documents, which are mostly
    // its ORM mapping. Lucee reads those in its Hibernate extension.
    static {
        define(COMPONENT, "accessors", "synchronized", "persistent", "extends", "implements", "modifier",
                "displayname", "hint", "output", "style", "namespace", "serviceportname", "serviceaddress",
                "porttypename", "bindingname", "wsdlfile", "alias", "mappedsuperclass", "javasettings", "initmethod");
        define(COMPONENT, "autoindex", "batchsize", "cachename", "cacheuse", "catalog", "consumes",
                "discriminatorcolumn", "discriminatorvalue", "dynamicinsert", "dynamicupdate", "embedded", "entityname",
                "httpmethod", "indexable", "indexlanguage", "invokeimplicitaccessor", "joincolumn", "lazy",
                "optimisticlock", "produces", "readonly", "rest", "restpath", "rowid", "savemapping", "schema",
                "selectbeforeupdate", "serializable", "table", "wsversion");
        alias(COMPONENT, "display", "displayname");
        define(INTERFACE, "displayname", "extends", "hint", "javasettings");
        alias(INTERFACE, "name", "displayname");
        define(FUNCTION, "name", "returntype", "bufferoutput", "roles", "access", "output", "displayname", "hint",
                "abstract", "description", "returnformat", "securejson", "verifyclient", "localmode", "cachedwithin",
                "modifier", "consumes", "produces", "httpmethod", "restpath");
        define(ARGUMENT, "name", "type", "required", "default", "displayname", "hint", "passby");
        define(PROPERTY, "name", "type", "required", "default", "displayname", "hint", "access", "getter", "setter");
        define(PROPERTY, "cacheuse", "cascade", "cfc", "column", "constrained", "dbdefault", "elementcolumn",
                "elementtype", "fetch", "fieldtype", "fkcolumn", "formula", "generated", "generator", "index", "insert",
                "inverse", "inversejoincolumn", "joincolumn", "lazy", "length", "linkcatalog", "linkschema", "linktable",
                "mappedby", "missingrowignored", "notnull", "optimisticlock", "orderby", "ormtype", "params",
                "persistent", "precision", "remotingfetch", "scale", "selectkey", "sequence", "serializable",
                "singularname", "source", "sqltype", "structkeycolumn", "structkeytype", "table", "unique", "uniquekey",
                "unsavedvalue", "update", "validate", "validateparams", "where");
    }

    private final Map<String, JavaType.FullyQualified> types = new HashMap<>();

    private static void define(String tag, String... names) {
        Map<String, String> attributes = ATTRIBUTES.computeIfAbsent(tag, t -> new HashMap<>());
        for (String name : names) {
            attributes.put(name, name);
        }
    }

    private static void alias(String tag, String alias, String name) {
        ATTRIBUTES.get(tag).put(alias, name);
    }

    /**
     * The type of an attribute of a declaration written in tag syntax as {@code tag}. Names are case-insensitive,
     * so a type's name is in lower case.
     */
    JavaType.FullyQualified type(String tag, String attribute) {
        String key = attribute.toLowerCase(Locale.ROOT);
        String name = ATTRIBUTES.get(tag).get(key);
        return types.computeIfAbsent(name == null ? key : tag + "." + name, fqn -> new JavaType.Class(null, 0, fqn,
                JavaType.FullyQualified.Kind.Annotation, null, null, null, null, null, null, null));
    }
}
