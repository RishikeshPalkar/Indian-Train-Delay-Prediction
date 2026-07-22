"""
ITPD Flask Prediction Service
Accepts exactly the features defined in model_manifest.json (written by train.py).
"""

import os
import json
import joblib
import numpy as np
import pandas as pd
from flask import Flask, request, jsonify

app = Flask(__name__)

BASE_DIR  = os.path.dirname(__file__)
MODEL_PATH    = os.path.join(BASE_DIR, "model.pkl")
MANIFEST_PATH = os.path.join(BASE_DIR, "model_manifest.json")

# ── Load model ──────────────────────────────────────────────────
model = None
if os.path.exists(MODEL_PATH):
    model = joblib.load(MODEL_PATH)
    print("Model loaded successfully.")
else:
    print("model.pkl not found - run train.py first.")

# ── Load feature manifest ────────────────────────────────────────
manifest = {}
FEATURE_COLS = []
if os.path.exists(MANIFEST_PATH):
    with open(MANIFEST_PATH) as f:
        manifest = json.load(f)
    FEATURE_COLS = manifest.get("all_features", [])
    print(f"Manifest loaded - {len(FEATURE_COLS)} features expected.")
else:
    # Fallback: define features inline (must match train.py)
    FEATURE_COLS = [
        "train_type", "season",
        "source_station_category", "destination_station_category",
        "primary_delay_cause",
        "year", "month", "day_of_week", "departure_hour",
        "is_weekend", "is_night_departure", "is_peak_hour",
        "is_festival_season", "distance_km", "num_scheduled_stops",
        "scheduled_travel_hours", "is_monsoon_season", "is_fog_risk",
        "fog_risk_score", "zone_fog_index", "zone_congestion_index",
    ]
    print("model_manifest.json not found - using hardcoded feature list.")


# ════════════════════════════════════════════════════════════════
# /predict
# ════════════════════════════════════════════════════════════════
@app.route("/predict", methods=["POST"])
def predict():
    if model is None:
        return jsonify({"error": "Model not loaded. Run train.py first."}), 503

    data = request.get_json(force=True, silent=True)
    if not data:
        return jsonify({"error": "No JSON body received."}), 400

    # Check required features
    missing = [f for f in FEATURE_COLS if f not in data]
    if missing:
        return jsonify({"error": f"Missing features: {missing}"}), 400

    try:
        # Build DataFrame preserving column order
        row = {col: data[col] for col in FEATURE_COLS}
        input_df = pd.DataFrame([row])

        # Primary prediction
        y_pred = model.predict(input_df)
        predicted_delay = int(round(max(0, float(y_pred[0]))))

        # ── Confidence from individual trees ──────────────────────
        rf           = model.named_steps["regressor"]
        preprocessor = model.named_steps["preprocessor"]
        X_transformed = preprocessor.transform(input_df)

        tree_preds = np.array([
            tree.predict(X_transformed)[0] for tree in rf.estimators_
        ])
        std_dev = float(tree_preds.std())
        low  = max(0, int(round(predicted_delay - 1.96 * std_dev)))
        high = max(0, int(round(predicted_delay + 1.96 * std_dev)))

        # Confidence: 100% when std→0, lower when std grows
        denom = predicted_delay + std_dev + 1   # avoid div-by-zero
        raw_conf = max(0, 1 - (std_dev / denom))
        confidence = int(round(raw_conf * 100))

        return jsonify({
            "predicted_delay_minutes":  predicted_delay,
            "confidence_interval_low":  low,
            "confidence_interval_high": high,
            "prediction_confidence":    confidence,
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ════════════════════════════════════════════════════════════════
# /health
# ════════════════════════════════════════════════════════════════
@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status":       "healthy",
        "model_loaded": model is not None,
        "num_features": len(FEATURE_COLS),
        "model_metrics": {
            "test_r2": manifest.get("test_r2"),
            "mae":     manifest.get("mae"),
            "rmse":    manifest.get("rmse"),
        }
    })


# ════════════════════════════════════════════════════════════════
# /features  — useful for debugging
# ════════════════════════════════════════════════════════════════
@app.route("/features", methods=["GET"])
def features():
    return jsonify({"expected_features": FEATURE_COLS})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
