# XGBoost Native Memory Lab Report

This report documents the findings of the memory management lab, demonstrating the critical importance of the `dispose()` method when working with XGBoost's native (JNI) memory in Java.

## 📅 Test Environment
- **Platform**: Docker (Linux Container)
- **OS**: Debian (Eclipse Temurin 25-jdk)
- **JVM**: OpenJDK 25-ea (Native Memory Tracking enabled)
- **Memory Limit**: 512 MiB
- **Heap Limit (-Xmx)**: 256 MiB
- **XGBoost Version**: 4J 3.2.0

---

## 📊 Summary of Results

| Phase | Total Requests | RSS (Real Memory) | Memory Delta | Observation |
| :--- | :--- | :--- | :--- | :--- |
| **Baseline** | 0 | **156.9 MiB** | - | Application idle after startup. |
| **Safe Test** | 500,000 | **168.5 MiB** | +11.6 MiB | Stable memory. Reclaimed by GC. |
| **Leaky Test** | 500,000 | **206.1 MiB** | **+49.2 MiB** | Steady growth. Memory not reclaimed. |

---

## 🔍 Native Memory Tracking (NMT) Analysis

Using `jcmd <pid> VM.native_memory summary`, we analyzed where the memory was being allocated.

### 1. Baseline NMT (Selected Categories)
```text
Total: reserved=17483MB, committed=416MB
-                 Java Heap (reserved=256MB, committed=256MB)
-                     Class (reserved=1040MB, committed=10MB)
-                    Thread (reserved=28MB, committed=3MB)
-                      Code (reserved=243MB, committed=12MB)
-                  Internal (reserved=2MB, committed=2MB)
```

### 2. Leaky NMT (After 500,000 Requests)
```text
Total: reserved=17491MB, committed=424MB
-                 Java Heap (reserved=256MB, committed=256MB)
-                     Class (reserved=1040MB, committed=15MB)
-                    Thread (reserved=32MB, committed=4MB)
-                      Code (reserved=243MB, committed=18MB)
-                  Internal (reserved=3MB, committed=3MB)
```

### 💡 The "Invisible" Leak
Notice that while **RSS** (the actual memory usage seen by the OS/Docker) increased by **~50 MiB**, the **Committed JVM Memory** tracked by NMT only increased by **~8 MiB** (mostly Code and Class metadata for the generated objects).

This proves that:
1.  **XGBoost native allocations bypass the JVM's memory management.**
2.  The memory is allocated directly via `malloc` or similar in the C++ layer.
3.  Traditional Java profiling tools (VisualVM, JConsole) will **not** show this leak because it is outside the Java Heap and JVM internal categories.
4.  Only OS-level monitoring (like `docker stats` or `monitor_memory.ps1`) can reliably identify this type of JNI-based leak.

---

## 🧪 Conclusion
Without explicit calls to `.dispose()`, the application's memory footprint will grow linearly with the number of `DMatrix` objects created, eventually leading to an **OOM (Out Of Memory) Kill** by the operating system, even if the Java Heap is almost empty.

The `try-finally` pattern implemented in `XGBoostService.java` is **mandatory** for production-grade XGBoost applications in Java.
