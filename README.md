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
- [PredictionRequest.java](src/main/java/org/icewheel/xgboostmldemo/model/PredictionRequest.java): What data we send (Age, Cholesterol, Blood Pressure, Heart Rate, Exercise Intensity).
- [PredictionResponse.java](src/main/java/org/icewheel/xgboostmldemo/model/PredictionResponse.java): What answer we get back (Risk Score, Risk Category, and **Reason Codes**).

---

## 🔍 Model Explainability (SHAP Values)

One of the most important parts of modern Machine Learning is not just getting a prediction, but understanding **why** the model made that choice. This project uses **SHAP (SHapley Additive exPlanations)** values to explain individual predictions.

### What are SHAP values?
Think of SHAP values as "credit assignment". If a person has an 80% risk score, SHAP values tell us how much each factor (Age, Cholesterol, etc.) pushed that score up or down from the average.
- **Positive SHAP Value**: This factor increased the risk (e.g., "High Cholesterol"). These are returned as **Bad Reasons**.
- **Negative SHAP Value**: This factor decreased the risk (e.g., "High Exercise Intensity"). These are returned as **Good Reasons**.

In `XGBoostService.java`, we use the `predictContrib()` method to get these values and then map them to human-readable explanations.

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

### 1. Predict Heart Disease Risk
**Endpoint**: `POST /api/prediction/predict`

**Request Body (JSON)**:
```json
{
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
  "riskScore": 0.85,
  "riskCategory": 1,
  "topBadReasons": ["Cholesterol level is high", "Age is high"],
  "topGoodReasons": ["High exercise intensity"]
}
```
- `riskScore`: A value between 0 and 1 (e.g., `0.85` means 85% probability of risk).
- `riskCategory`: `1` for High Risk, `0` for Low Risk.
- `topBadReasons`: Factors that increased the risk.
- `topGoodReasons`: Factors that decreased the risk.

### 2. Retrain the Model
If you want the model to learn again from a new set of synthetic data:
**Endpoint**: `POST /api/prediction/retrain`

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

## 📚 Reference Documentation
For further reference, please consider the following sections:
- [Official Gradle documentation](https://docs.gradle.org)
- [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.2/gradle-plugin)
- [Create an OCI image](https://docs.spring.io/spring-boot/4.0.2/gradle-plugin/packaging-oci-image.html)
- [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
- [Official XGBoost Documentation](https://xgboost.readthedocs.io/en/stable/)
