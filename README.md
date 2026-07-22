# 🚆 Indian Train Delay Predictor — AI Service

This repository contains the **standalone AI microservice** for the Indian Train Delay Predictor project. The service is built with Python and Flask, exposing machine learning prediction models via a lightweight REST API.

> **Note:** This repository currently contains the AI service only. There is no backend or frontend web application present in this repository at this time.

---

## 🛠️ Features

* **Flask ML Microservice:** Serves delay predictions over an HTTP REST API.
* **Standard ML Pipeline:** Powered by Python data science libraries (`scikit-learn`, `pandas`, `numpy`, `xgboost`).
* **Container Ready:** Includes `docker-compose.yml` for simplified local setup and service orchestration.

---

## 📁 Project Structure

```text
Indian-Train-Delay-Prediction/
├── ai/
│   ├── app.py              # Main Flask application & prediction routes
│   ├── requirements.txt    # Python dependencies
│   └── models/             # ML model binaries / artifacts
├── docker-compose.yml      # Service orchestration for local development
├── .gitignore
└── README.md