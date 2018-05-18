@echo OFF

if "%1"=="clean" goto clean

echo javac Ticket2Ride.java
javac Ticket2Ride.java
goto done

:clean
echo del *.class
del *.class

:done
