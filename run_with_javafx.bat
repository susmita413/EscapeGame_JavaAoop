@echo off
echo Setting up JavaFX and running Escape Game...

REM Set Java home
set JAVA_HOME=C:\Program Files\Java\jdk-24
set PATH=%JAVA_HOME%\bin;%PATH%

REM Create lib directory if it doesn't exist
if not exist "lib" mkdir lib

REM Download JavaFX if not present
if not exist "lib\javafx-controls-21.0.6.jar" (
    echo Downloading JavaFX dependencies...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.6/javafx-controls-21.0.6.jar' -OutFile 'lib\javafx-controls-21.0.6.jar'"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.6/javafx-fxml-21.0.6.jar' -OutFile 'lib\javafx-fxml-21.0.6.jar'"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-web/21.0.6/javafx-web-21.0.6.jar' -OutFile 'lib\javafx-web-21.0.6.jar'"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-swing/21.0.6/javafx-swing-21.0.6.jar' -OutFile 'lib\javafx-swing-21.0.6.jar'"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-media/21.0.6/javafx-media-21.0.6.jar' -OutFile 'lib\javafx-media-21.0.6.jar'"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar' -OutFile 'lib\gson-2.10.1.jar'"
)

echo Starting the application...
java -cp "target/classes;lib/*" com.example.escapeGame.Launcher

pause
