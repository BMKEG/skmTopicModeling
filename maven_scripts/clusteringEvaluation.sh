#!/bin/sh
MAVEN_OPTS=-Xmx2g
mvn exec:java -Dexec.mainClass="edu.isi.bmkeg.script.DocumentClusteringsEvaluationScript" -Dexec.args="$*"
