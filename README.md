# XGBoost Machine Learning Demo with Spring Boot

This project is a beginner-friendly demonstration of how to integrate the **XGBoost** machine learning library into a **Java** application using the **Spring Boot** framework.

## 🧠 Machine Learning for Everyone

If you are new to Machine Learning (ML), here is a simple way to understand what this project does:

1.  **The Goal**: We want to predict if a person has a high risk of heart disease based on their health data (like age, cholesterol, and blood pressure).
2.  **Training (Learning)**: Imagine showing a student thousands of medical records where you point out which ones resulted in heart disease and which didn't. The student starts noticing patterns (e.g., "Older people with high cholesterol often have higher risk"). In this project, **XGBoost** is that student.
3.  **Model**: After the student finishes studying, they have a "brain" full of patterns. We save this "brain" as a file called `model.json`.
4.  **Prediction (Guessing)**: Now, we can give the student a *new* person's data they have never seen before. The student uses their "brain" to give us a "Risk Score" (a probability between 0 and 1).

---

## 📂 Project Structure & Key Files

The project is organized into several components, each with a specific job:

### 1. Data Generation: `DataService.java`
Since we don't have a real hospital database, we use [DataService.java](src/main/java/org/icewheel/xgboostmldemo/service/DataService.java) to create **synthetic (fake) data**. 
- It generates 1,000 "virtual patients" with random health metrics.
- It calculates a "label" (0 for Low Risk, 1 for High Risk) using a mathematical formula so the model has something to learn from.

### 2. The ML Brain: `XGBoostService.java`
This is the core of the application. [XGBoostService.java](src/main/java/org/icewheel/xgboostmldemo/service/XGBoostService.java) handles the life cycle of the machine learning model:
- **Initialization**: When the app starts, it checks if `model.json` exists. If not, it triggers training.
- **Training**: It takes the data from `DataService`, feeds it into XGBoost, and tells it to find patterns.
- **Saving/Loading**: It saves the learned patterns into `model.json` in a human-readable format.
- **Predicting**: It takes a single person's data and asks the model for a risk score.

### 3. The Web Interface: `PredictionController.java`
[PredictionController.java](src/main/java/org/icewheel/xgboostmldemo/controller/PredictionController.java) allows you to talk to the application over the internet (or your local network). It provides "endpoints" where you can send data and get answers.

### 4. Data Models: `PredictionRequest.java` & `PredictionResponse.java`
These files define the "forms" used for communication:
- [PredictionRequest.java](src/main/java/org/icewheel/xgboostmldemo/model/PredictionRequest.java): What data we send (Patient ID, Age, Cholesterol, Blood Pressure, Heart Rate, Exercise Intensity).
- [PredictionResponse.java](src/main/java/org/icewheel/xgboostmldemo/model/PredictionResponse.java): What answer we get back (Patient ID, Risk Score, Risk Category, and **Reason Codes**).

---

## 🔍 Model Explainability (SHAP Values)

One of the most important parts of modern Machine Learning is not just getting a prediction, but understanding **why** the model made that choice. This project uses **SHAP (SHapley Additive exPlanations)** values to explain individual predictions.

### What are SHAP values?
Think of SHAP values as "credit assignment". If a person has an 80% risk score, SHAP values tell us how much each factor (Age, Cholesterol, etc.) pushed that score up or down from the average.
- **Positive SHAP Value**: This factor increased the risk (e.g., "High Cholesterol"). These are returned as **Bad Reasons**.
- **Negative SHAP Value**: This factor decreased the risk (e.g., "High Exercise Intensity"). These are returned as **Good Reasons**.

### Why Native XGBoost instead of `shap4j`?
A common question is whether to use external libraries like `shap4j`. This project intentionally uses the built-in `predictContrib()` method from the XGBoost library for several reasons:
1.  **Performance**: The native XGBoost implementation (Tree SHAP) is extremely fast as it runs directly in C++ via JNI.
2.  **Accuracy**: It is the official implementation of SHAP for XGBoost models, ensuring the most accurate contribution values.
3.  **Simplicity**: It avoids adding extra dependencies that might be unmaintained or provide slower (model-agnostic) versions of SHAP.
4.  **Maintainability**: We use a dynamic `FeatureDescriptor` approach in `XGBoostService.java` to map these values to human-readable reasons, making it easy to add or change features without complex external logic.

In `XGBoostService.java`, we capture these contributions and use a lookup map to provide friendly health advice.

---

## 🚀 How to Run the Project

### Prerequisites
- **Java 25**: This project uses the latest Java features.
- **Gradle**: Used for building the project (the `gradlew` wrapper is included).

### Steps to Run
1.  Open a terminal in the project root folder.
2.  Run the application:
    ```bash
    ./gradlew bootRun
    ```
3.  **On First Run**: The application will notice that `model.json` is missing. It will automatically generate data, train a new model, and save it. You will see logs like:
    `Model trained and saved to model.json`

---

## 🛠️ How to Use the API

You can interact with the running application using tools like **Postman**, **Insomnia**, or the command-line tool `curl`.

### 1. Predict Heart Disease Risk (Single)
**Endpoint**: `POST /api/prediction/predict`

**Request Body (JSON)**:
```json
{
  "patientId": "PATIENT_123",
  "age": 55,
  "cholesterol": 240,
  "bloodPressure": 145,
  "heartRate": 80,
  "exerciseIntensity": 2
}
```

**Response**:
```json
{
  "patientId": "PATIENT_123",
  "riskScore": 0.85,
  "riskCategory": 1,
  "topBadReasons": ["Cholesterol level is high", "Age is high"],
  "topGoodReasons": ["High exercise intensity"]
}
```

### 2. Predict Heart Disease Risk (Batch)
**Endpoint**: `POST /api/prediction/predict-batch`

**Request Body (JSON)**:
```json
[
  { "patientId": "P1", "age": 55, "cholesterol": 240, "bloodPressure": 145, "heartRate": 80, "exerciseIntensity": 2 },
  { "patientId": "P2", "age": 25, "cholesterol": 160, "bloodPressure": 110, "heartRate": 65, "exerciseIntensity": 9 }
]
```

**Response**:
A list of prediction objects (one for each input row).

**Why use Batch Prediction?**
As explained in the "Technical Deep Dive", sending multiple patients in a single `predict-batch` call is much more efficient than calling `predict` multiple times in a loop. It reduces the overhead of crossing the boundary between Java and the native XGBoost C++ engine (JNI) and allows for better internal parallelization.

### 3. Retrain the Model
If you want the model to learn again from a new set of synthetic data:
**Endpoint**: `POST /api/prediction/retrain`

---

## 🛠️ Technical Deep Dive (For Developers)

Even if you are new to Machine Learning, understanding these two concepts will help you work with XGBoost in Java:

### 1. What is a `DMatrix`?
In most Java programs, we use `Arrays` or `Lists` to store data. However, XGBoost is written in highly optimized **C++**. 
- A **`DMatrix`** is a special container that wraps your Java data and prepares it for the C++ engine. 
- It handles things like "missing values" and memory layout so the model can learn as fast as possible.
- In `XGBoostService.java`, you will see us converting our `float[]` arrays into `DMatrix` before training or predicting.

### 2. Understanding Data Dimensions (`nrow` and `ncol`)
When creating a `DMatrix` from a flat array (like `float[]`), XGBoost needs to know the shape of your data:
- **`nrow` (Number of Rows)**: This is the number of **samples** or **records** you are providing.
    - For **Training**, `nrow` is 1000 because we are feeding 1000 people's data at once.
    - For **Single Prediction**, `nrow` is 1 because we are asking for a result for only one specific person.
- **`ncol` (Number of Columns)**: This is the number of **features** or **attributes** each sample has. Since we give XGBoost a **flat list of numbers**, it needs to know how many columns are in each row to correctly "reshape" the list into a table. In this project, it is always `5` (Age, Cholesterol, BP, HR, Exercise).

**When is `nrow` more than 1?**
1. **Training**: To learn patterns from a large group of data points.
2. **Batch Prediction**: If you have a list of 100 people and want to get all their risk scores at once. It is much faster than calling the "predict" function 100 times in a loop, as it allows XGBoost to process them in parallel.

### 3. Manual Memory Management (`dispose()`)
Java usually handles memory automatically (Garbage Collector). But because `DMatrix` and `Booster` use **Native Memory** (memory outside the normal Java heap, managed by C++), Java doesn't know when to clean it up.
- We must manually call **`.dispose()`** on every `DMatrix` and `Booster` object once we are done with them.
- **Best Practice**: Use `try-finally` blocks (or `@PreDestroy` for long-lived objects like the `Booster`) to ensure that `dispose()` is called even if an error occurs.
- If we forget this, the application will leak memory, which can lead to `OutOfMemoryError` or system crashes, even if the Java Garbage Collector seems to have plenty of free space.

### 4. Training Hyperparameters (Why 1000 and 50?)
In `XGBoostService.java`, we use specific numbers for training:
- **`trainCount = 1000`**: This is the number of "virtual patients" we generate to teach the model. For a simple problem with 5 features, 1000 examples are enough for the model to learn the patterns without taking too much time or memory.
- **`round = 50`**: This is the number of "boosting rounds" (number of trees the model builds). Each new tree tries to fix the mistakes of the previous ones. 50 rounds are usually enough for this problem to reach high accuracy. Using too many rounds (e.g., 5000) might make the model "overfit," meaning it starts memorizing the fake data instead of learning general patterns.

---

## ⚙️ Configuration
The project settings are located in [src/main/resources/application.yaml](src/main/resources/application.yaml). You can change the name or location of the model file there:
```yaml
xgboost:
  model:
    path: model.json
```

## 🧪 Technical Highlights
- **Framework**: Spring Boot 4.0.2
- **Library**: XGBoost4J 3.2.0 (JVM Bindings)
- **Problem Type**: Binary Classification (`binary:logistic`)
- **Evaluation Metric**: Logarithmic Loss (`logloss`)
- **Model Format**: JSON (Human-readable and easy to inspect)
- **Dependencies**: Includes `jackson-databind` for JSON compatibility with the XGBoost library.

## 🧪 Memory Management Lab: The Impact of `dispose()`

As mentioned in the Technical Deep Dive, calling `.dispose()` on native XGBoost objects (`DMatrix`, `Booster`) is critical. This project includes a built-in "Lab" to help you observe this impact.

### Why is this necessary?
XGBoost is a C++ library. When you create a `DMatrix` in Java, it allocates memory in the **Native Heap** (managed by the OS, not the JVM). 
- If you **don't** call `dispose()`, this memory stays allocated forever.
- The Java Garbage Collector (GC) cannot see or free this memory.
- Eventually, your app will crash with an `OutOfMemoryError` or be killed by the OS.

### How to Run the Experiment

#### 1. Start the App with Native Memory Tracking (NMT)
Run the application with the JVM flag that enables memory tracking:
```bash
# Using gradlew (add the flag to application arguments if configured, 
# or run the JAR directly after building)
java -XX:NativeMemoryTracking=summary -jar build/libs/xgboost-ml-demo-0.0.1-SNAPSHOT.jar
```

#### 2. Monitor Memory Usage
Open a separate terminal and run the included monitoring script:
```powershell
./monitor_memory.ps1
```

#### 3. Run the "Safe" Stress Test
Trigger 500,000 predictions **with** `dispose()`:
```powershell
./stress_test.ps1 -count 500000
```
**Observation**: You should see memory usage remain stable or return to baseline after the test finishes.

#### 4. Run the "Leaky" Stress Test
Trigger 500,000 predictions **without** `dispose()` (simulated leak):
```powershell
./stress_test.ps1 -count 500000 -leak
```
**Observation**: Watch the `Private Memory` in `monitor_memory.ps1`. It will spike and **not** go down. Each 100-request batch leaks a small amount of native memory. If you run this enough times, the app will crash.

#### 5. Verify with JCMD
You can see exactly where the memory is leaked using the JDK's `jcmd` tool:
```bash
jcmd <pid> VM.native_memory summary
```
Look for the `Internal` or `Other` sections, which often grow when JNI/Native memory is leaked.

### 6. Docker (The Ultimate "Contained" Environment)
Using Docker is the best way to analyze memory because it isolates the application from the host and allows you to set strict resource limits.

**Why Docker helps analyze memory better:**
1.  **Isolation**: You monitor *only* the application's memory usage, without interference from other host processes.
2.  **Strict Limits**: You can set a hard memory limit (e.g., 512MB). If the app leaks native memory, the OS (via Docker) will kill it (`OOMKilled`). This is a definitive way to prove a leak exists.
3.  **No Host Tooling**: You don't need `monitor_memory.ps1` if you use `docker stats`.

**Steps to Run the Docker Lab:**
1.  **Start the Container**:
    ```bash
    docker-compose up --build
    ```
2.  **Monitor with Docker Stats**:
    In a new terminal, run:
    ```bash
    docker stats
    ```
    This shows real-time memory usage (RSS/Working Set) for the container.
3.  **Run the Leaky Test**:
    ```powershell
    ./stress_test.ps1 -count 500000 -leak
    ```
4.  **Observe OOMKilled**:
    As the native memory grows (visible in `docker stats`), the container will eventually reach its 512MB limit and be forcibly stopped by Docker. You can verify this by running:
    ```bash
    docker inspect <container_id> --format='{{.State.OOMKilled}}'
    ```

### 🏆 Official Lab Results
You can view a detailed, manually verified report in [LAB_REPORT.md](LAB_REPORT.md). 

To generate a fresh report on your own system, run the automated script:
```powershell
./run_lab_report.ps1
```
This will automatically build the environment, run the comparison tests, and save the results in `LAB_RESULTS_AUTOMATED.md`.

---

## 📚 Reference Documentation
For further reference, please consider the following sections:
- [Official Gradle documentation](https://docs.gradle.org)
- [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.2/gradle-plugin)
- [Create an OCI image](https://docs.spring.io/spring-boot/4.0.2/gradle-plugin/packaging-oci-image.html)
- [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
- [Official XGBoost Documentation](https://xgboost.readthedocs.io/en/stable/)
