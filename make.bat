@echo OFF

if "%1"=="clean" goto clean

echo javac Ticket2Ride.java Triple.java
javac Ticket2Ride.java Triple.java
goto done

:clean
echo del *.class
del *.class

:done
