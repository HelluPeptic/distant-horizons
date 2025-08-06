# BUILD SYSTEM FIX GUIDE

## Problem

The current build system uses:

-   Gradle 7.6 (outdated)
-   Architectury Loom 1.1.376 (outdated, no longer supported)
-   Incompatible repository configurations

## Solution Steps

### 1. Update Gradle Wrapper

```powershell
.\gradlew wrapper --gradle-version 8.5
```

### 2. Update build.gradle (root)

Update the loom version to a newer compatible version:

```gradle
plugins {
    id 'architectury-plugin' version '3.4-SNAPSHOT'
    id 'dev.architectury.loom' version '1.4-SNAPSHOT' apply false
}
```

### 3. Clear Gradle Cache

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches"
```

### 4. Try Build Again

```powershell
.\gradlew clean build -Pmc_version=1.20.1
```

## Our FPS Optimizations Are Ready

Once the build system is fixed, our aggressive FPS optimizations will be compiled in:

-   Ultra-aggressive texture LOD bias (-1.25f)
-   Performance-oriented filtering (GL_LINEAR_MIPMAP_NEAREST)
-   Enhanced bias multiplier (1.5x)
-   Optimized build configurations
