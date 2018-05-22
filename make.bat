@echo OFF

if "%1"=="clean" goto clean

echo javac *.java
javac *.java
goto done

:clean
echo del *.class
del *.class

:done
