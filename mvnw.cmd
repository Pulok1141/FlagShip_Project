@echo off
set "JAVA_HOME=C:\Users\USER\.jdks\corretto-22.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"D:\Software_New_Project\.tools\apache-maven-3.9.9\bin\mvn.cmd" -Dmaven.repo.local="D:\Software_New_Project\.m2\repository" %*
