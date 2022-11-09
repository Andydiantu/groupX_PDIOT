import os
import json
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

import io
import tensorflow as tf
from tensorflow import keras
import numpy as np
from PIL import Image

from flask import Flask, request, jsonify

model = keras.models.load_model("har.h5")


# input is a np array with shape (50*12)
def prediction(data):
    data = (data - (-0.12563613669930004)) / 25.2273530887114
    data = np.array([data])
    prediction = model(data)
    prediction = np.argmax(prediction, axis=1)
    return prediction[0]


app = Flask(__name__)

@app.route("/", methods = ["GET","POST"])
def index():
    if request.method == "POST":
        data = request.data
        if data is None:
            return jsonify({"error": "no data"})
        try:
            windowData = json.loads(data)
            windowData = json.loads(windowData)

            print(type(windowData))
            windowData = np.array(windowData)
            print(windowData.shape)

            result = prediction(windowData)
            result = {"prediction": int(result)}
            return jsonify(result)


        except Exception as e:
            return jsonify({"error": str(e)})
    return "OK"


if __name__ == "__main__":
    app.run(debug=True)