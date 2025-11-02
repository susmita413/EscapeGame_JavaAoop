@echo off
echo Starting Escape Game directly with Java...
set JAVA_HOME=C:\Program Files\Java\jdk-24
set PATH=%JAVA_HOME%\bin;%PATH%

echo Compiling the application...
javac -cp "target/classes;lib/*" -d target/classes src/main/java/com/example/escapeGame/*.java src/main/java/com/example/escapeGame/server/*.java src/main/java/com/example/escapeGame/net/*.java

echo Starting the application...
java --module-path "C:\Program Files\Java\javafx-21.0.6\lib" --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing,javafx.media -cp "target/classes;lib/*" com.example.escapeGame.Launcher

pause
