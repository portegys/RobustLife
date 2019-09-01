set OLDCLASSPATH=%CLASSPATH%
set CLASSPATH=.
javac -d . *.java
jar cvfm robustlife.jar robustlife-manifest.mf robustlife
set CLASSPATH=%OLDCLASSPATH%
set OLDCLASSPATH=

