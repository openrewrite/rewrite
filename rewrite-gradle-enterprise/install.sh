#!/usr/bin/env bash

VERSION=0.1.0
GE_CONF='
gradleEnterpriseServer: https://ge-unstable.grdev.net/
extensionVersion: 1.3.6
'

if [ ! -f rewrite-gradle-enterprise.jar ]; then
    echo "Downloading Rewrite CLI"
    curl -Lo rewrite-gradle-enterprise.jar https://repo.gradle.org/gradle/libs-snapshots-local/org/openrewrite/rewrite-gradle-enterprise/$VERSION/rewrite-gradle-enterprise-$VERSION.jar
fi

java -jar rewrite-gradle-enterprise.jar -c "$GE_CONF" "$@"