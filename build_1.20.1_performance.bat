@echo off
REM Distant Horizons 1.20.1 Fabric Performance Build Script
REM This script builds the mod specifically for Minecraft 1.20.1 with maximum FPS optimizations

echo ====================================
echo Distant Horizons 1.20.1 Fabric Build
echo MAXIMUM FPS PERFORMANCE VERSION
echo ====================================

echo.
echo Setting up environment...
set GRADLE_OPTS=-Xmx4G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions

echo.
echo Cleaning previous builds...
call gradlew clean

echo.
echo Building for Minecraft 1.20.1 Fabric with performance optimizations...
call gradlew build -Pmc_version=1.20.1 -Pperformance=true --parallel --build-cache

echo.
if %errorlevel% equ 0 (
    echo ====================================
    echo BUILD SUCCESSFUL!
    echo ====================================
    echo.
    echo Output files are located in:
    echo - fabric\build\libs\DistantHorizons-*-fabric.jar
    echo.
    echo PERFORMANCE OPTIMIZATIONS INCLUDED:
    echo - Ultra-aggressive texture LOD bias
    echo - Optimized GPU state management  
    echo - Enhanced chunk caching
    echo - Performance-oriented filtering
    echo.
    echo Remember to use the JVM arguments from MAXIMUM_FPS_OPTIMIZATION_GUIDE.md
    echo for optimal runtime performance!
) else (
    echo ====================================
    echo BUILD FAILED!
    echo ====================================
    echo.
    echo Try running the build without performance flags:
    echo gradlew build --no-daemon --parallel
    echo.
    echo Or check the MAXIMUM_FPS_OPTIMIZATION_GUIDE.md for troubleshooting steps.
)

echo.
pause
