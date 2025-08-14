@echo off
echo === Graph Reasoning System Demo ===

REM Check if OPENAI_API_KEY is set
if "%OPENAI_API_KEY%"=="" (
    echo Warning: OPENAI_API_KEY environment variable is not set
    echo Please set it with: set OPENAI_API_KEY=your_api_key
    echo.
)

REM Check Java version
echo Checking Java version...
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

REM Check Maven
echo Checking Maven...
mvn -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven 3.6 or higher
    pause
    exit /b 1
)

REM Check if Neo4j is running (simplified check)
echo Checking Neo4j connection...
REM Note: Windows doesn't have nc by default, so we'll skip this check
REM The Java application will handle the connection error

REM Build the project
echo Building project...
mvn clean compile -q

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo Build successful

REM Run the demo
echo Starting demo...
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.GraphReasoningDemo" -q

echo Demo completed
pause