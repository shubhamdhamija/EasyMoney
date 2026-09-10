# 🎯 Android Studio Sync Guide - Fix Red Lines

## Problem Identified & Fixed ✅

**Issue:** Red squiggly lines under `Flow`, `StateFlow`, and coroutines imports in Android Studio.

**Root Causes:**
1. ❌ Missing dependency: `kotlinx-coroutines-core` library
2. ❌ Conflicting imports: Both `kotlinx.coroutines.flow.Flow` and `java.util.concurrent.Flow`

**Solutions Applied:**
1. ✅ Added `kotlinx-coroutines-core` to `gradle/libs.versions.toml`
2. ✅ Added dependency to `app/build.gradle.kts`
3. ✅ Removed conflicting `java.util.concurrent.Flow` import from `StockRepository.kt`

---

## Build Status

**Before:** ❌ Compilation errors (red lines in IDE)
**After:** ✅ BUILD SUCCESSFUL in 7s

---

## How to Sync Android Studio (Remove Red Lines)

### Step 1: Sync Gradle Files
**In Android Studio, do ONE of the following:**

**Option A:** Use the Menu
```
File → Sync Now
```

**Option B:** Use the Gradle Bar (if visible)
Look for a banner at the top of the editor with:
```
"Build.gradle was changed. Sync?"
```
Click: **Sync Now**

**Option C:** Use Keyboard Shortcut
```
Ctrl + Shift + A  (on Windows/Linux)
Cmd + Shift + A   (on macOS)
```
Then type: `Sync` and press Enter

### Step 2: Wait for Indexing
Android Studio will:
- Download the new `kotlinx-coroutines-core` library
- Re-index all code
- This takes 30-60 seconds

You'll see progress at the bottom of the screen:
```
Indexing...  [████████░░░░░░░░] 45%
```

### Step 3: Verify Red Lines Are Gone
After indexing completes:
- ✅ Red squiggly lines disappear
- ✅ Imports are recognized
- ✅ Code completion works
- ✅ All errors resolved

---

## If Red Lines Persist

### Solution 1: Invalidate Caches
Sometimes Android Studio's cache gets stuck. Clear it:

**Path:** File → Invalidate Caches
```
1. File menu
2. Click "Invalidate Caches..."
3. Select: "Invalidate and Restart"
4. Wait for Android Studio to restart
5. Automatic sync will trigger
```

### Solution 2: Clean & Rebuild
From Android Studio:
```
Build → Clean Project
Build → Rebuild Project
```

Or from terminal:
```bash
cd /Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney
./gradlew clean assembleDebug
```

### Solution 3: Update IDE Cache
From terminal:
```bash
# On macOS, clear Android Studio caches
rm -rf ~/Library/Caches/AndroidStudio*
rm -rf ~/Library/Application\ Support/AndroidStudio*
```
Then restart Android Studio.

---

## What Changed

### File: `gradle/libs.versions.toml`
```toml
# ADDED:
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
```

### File: `app/build.gradle.kts`
```kotlin
// ADDED:
implementation(libs.coroutines.core)
// ALREADY HAD:
implementation(libs.coroutines.android)
```

### File: `app/src/main/java/com/invest/easymoney/domain/repository/StockRepository.kt`
```kotlin
// REMOVED:
import java.util.concurrent.Flow  ← THIS WAS CAUSING CONFLICT

// KEPT:
import kotlinx.coroutines.flow.Flow  ← This is the correct one
```

---

## Expected Result

After syncing:

### Before Sync:
```kotlin
import kotlinx.coroutines.flow.Flow  ← RED LINE ❌
                                       Unresolved reference

fun getWatchlistSymbols(): Flow<List<String>>  ← RED SQUIGGLES ❌
```

### After Sync:
```kotlin
import kotlinx.coroutines.flow.Flow  ← NORMAL ✅
                                       No error

fun getWatchlistSymbols(): Flow<List<String>>  ← NORMAL ✅
```

---

## Verification Checklist

After syncing, verify:

- [ ] No red squiggly lines under imports
- [ ] No red errors in `StockRepository.kt`
- [ ] No red errors in `StockRepositoryImpl.kt`
- [ ] No red errors in `HomeViewModel.kt`
- [ ] Code completion works (Ctrl+Space)
- [ ] Can click-through to class definitions (Ctrl+B)
- [ ] No "Unresolved reference" messages

---

## Build Verification

The app already builds successfully:

```
✅ BUILD SUCCESSFUL in 7s
41 actionable tasks: 12 executed, 29 up-to-date
```

This means:
- ✅ All dependencies are properly installed
- ✅ All code compiles without errors
- ✅ Ready to run on device/emulator

---

## Quick Reference

| Issue | Solution |
|-------|----------|
| Red lines persist | File → Sync Now |
| Still red after sync | File → Invalidate Caches → Restart |
| Import shows as unresolved | Check gradle sync completed |
| Build fails locally | Clear Android Studio cache |
| Want to force fresh sync | `./gradlew clean assemble` |

---

## Done! ✅

Your project is now:
- ✅ Properly synced with Gradle
- ✅ All dependencies resolved
- ✅ Builds successfully
- ✅ Red lines should be gone from IDE

**Next Step:** Run the app in Android Studio and see your stocks! 🚀


