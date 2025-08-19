from flask import Flask, request, Response
import joblib
import pandas as pd
import re
from urllib.parse import urlparse
from flask_cors import CORS
import traceback
import json
from collections import OrderedDict

app = Flask(__name__)
CORS(app)

model_path = r"C:\Users\USER\Desktop\Software_creative_design\Back_end\Model_ensemble_version_03_0.9663.pkl"
model = joblib.load(model_path)

def extract_features(url):
    parsed_url = urlparse(url)
    features = {
        'has_ip': 1 if re.match(r'http[s]?://\d+\.\d+\.\d+\.\d+', url) else 0,
        'long_url': len(url),
        'is_shortened': 1 if re.search(r"bit\.ly|tinyurl\.com|goo\.gl|shorte\.st|adf\.ly|t\.co|ow\.ly|tiny\.cc|is\.gd|clk\.im", url) else 0,
        'has_at_symbol': 1 if "@" in url else 0,
        'has_double_slash': 1 if '//' in parsed_url.path else 0,
        'has_dash': 1 if '-' in parsed_url.netloc else 0,
        'subdomain_count': parsed_url.netloc.count('.') - 1,
        'is_https': 1 if parsed_url.scheme == 'https' else 0,
        'domain_reg_length': len(parsed_url.netloc),
        'has_favicon': 1 if 'favicon' in url else 0,
        'non_standard_port': 1 if parsed_url.port not in [80, 443, None] else 0
    }
    return pd.DataFrame([features])

def infer_attack_type(features):
    try:
        if features['has_at_symbol'] == 1:
            return "Credential Harvesting"
        if features['is_shortened'] == 1 and features['has_ip'] == 0:
            return "Redirection Attack"
        if features['has_ip'] == 1 and features['is_https'] == 0:
            return "IP-Based Attack"
        if features['non_standard_port'] == 1:
            return "Port Exploit Attempt"
        if features['has_dash'] == 1 and features['subdomain_count'] >= 2:
            return "Fake Domain"
        if features['domain_reg_length'] > 30:
            return "Suspicious Long Domain"
        if features['subdomain_count'] >= 3 and features['is_https'] == 0:
            return "Unsecured Multi-Subdomain Attack"
        if features['has_double_slash'] == 1 and features['long_url'] > 80:
            return "Deep Path Obfuscation"
        if features['has_favicon'] == 0 and features['is_https'] == 0:
            return "Suspicious Unsecure Hosting"
        return "Unknown"
    except Exception:
        return "Unknown"

@app.route('/predict', methods=['POST'])
def predict():
    try:
        if not request.is_json:
            return Response(json.dumps({'error': 'Request must be in JSON format!'}), status=402, mimetype='application/json')

        data = request.get_json(silent=True)
        if data is None:
            return Response(json.dumps({'error': 'Invalid JSON format!'}), status=401, mimetype='application/json')

        original_url = data.get('url', '').strip()
        if not original_url:
            return Response(json.dumps({'error': 'Please provide a URL!'}), status=400, mimetype='application/json')

        url = original_url
        if not url.startswith(('http://', 'https://')):
            url = 'http://' + url

        df = extract_features(url)

        model_input_features = [
            'has_ip', 'long_url', 'is_shortened', 'has_at_symbol',
            'has_double_slash', 'has_dash', 'subdomain_count',
            'is_https', 'domain_reg_length', 'has_favicon', 'non_standard_port'
        ]
        model_input = df[model_input_features]

        prediction = model.predict(model_input)[0]
        prediction_proba = model.predict_proba(model_input)[0]

        probability_str = f"{prediction_proba[1] * 100:.1f}%" if prediction == 1 else f"{prediction_proba[0] * 100:.1f}%"

        response_data = OrderedDict([
            ("URL", original_url),
            ("Result", "Malicious" if prediction == 1 else "Safe"),
            ("Probability", probability_str)
        ])

        if prediction == 1:
            attack_type = infer_attack_type(df.iloc[0].to_dict())
            response_data["Attack_type"] = attack_type

        return Response(json.dumps(response_data), mimetype='application/json'), 200

    except Exception as e:
        traceback.print_exc()
        return Response(json.dumps({'error': str(e)}), status=500, mimetype='application/json')

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=7245)
