@echo OFF

if "%1"=="clean" goto clean

echo javac Ticket2Ride.java Pair.java Triple.java
javac Ticket2Ride.java Pair.java Triple.java
goto done

:clean
echo del *.class
del *.class

:done
