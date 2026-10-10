#!/bin/sh
# javac / jar via JDK modules (this sandbox ships only a JRE launcher)
case "$1" in
  jar) shift; exec java -m jdk.jartool/sun.tools.jar.Main "$@" ;;
  *)   exec java -m jdk.compiler/com.sun.tools.javac.Main "$@" ;;
esac
