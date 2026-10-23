"""
Standalone Mock AI Model Service for Healthcare Chatbot
------------------------------------------------------
This mock server runs on port 8087 and implements the exact contract expected by CHATBOT-SERVICE:
Endpoint: POST /api/v1/ai/chat

Requirements: Python 3.7+ (No external pip dependencies required).
To run:
    python mock_ai_service.py
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import sys

PORT = 8087

class AiModelHandler(BaseHTTPRequestHandler):

    def do_POST(self):
        if self.path == "/api/v1/ai/chat":
            content_length = int(self.headers.get("Content-Length", 0))
            request_body = self.rfile.read(content_length).decode("utf-8")

            try:
                data = json.loads(request_body)
            except Exception as e:
                self._send_response(400, {"error": "Invalid JSON", "details": str(e)})
                return

            age = data.get("age")
            message = data.get("message", "")
            history = data.get("conversationHistory", [])

            print("\n" + "=" * 50)
            print(">>> RECEIVED REQUEST FROM CHATBOT-SERVICE:")
            print(f"    User Age: {age}")
            print(f"    Current Message: '{message}'")
            print(f"    History Messages Count: {len(history)}")
            for idx, h in enumerate(history, 1):
                print(f"      {idx}. [{h.get('role')}]: {h.get('content')}")
            print("=" * 50)

            # Simulated AI medical assistant response tailored to age and symptoms
            if age is not None and age < 18:
                ai_text = (
                    f"Hello. As a pediatric/adolescent healthcare assistant, I have noted that you are experiencing "
                    f"'{message}'. How long have you felt this way, and are your parents or a guardian aware?"
                )
            elif age is not None and age >= 60:
                ai_text = (
                    f"Hello. I understand you are experiencing '{message}'. At {age} years of age, it is important to "
                    f"monitor this closely. Do you have any other symptoms such as fever, breathlessness, or dizziness?"
                )
            else:
                ai_text = (
                    f"Thank you for sharing your symptoms. I understand you are having '{message}'. "
                    f"Can you tell me how long you have been experiencing this, and is the sensation constant or intermittent?"
                )

            response_payload = {
                "success": True,
                "data": {
                    "response": ai_text
                }
            }

            self._send_response(200, response_payload)
        else:
            self._send_response(404, {"error": f"Endpoint not found: {self.path}"})

    def _send_response(self, status_code, payload):
        response_bytes = json.dumps(payload).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(response_bytes)))
        self.end_headers()
        self.wfile.write(response_bytes)

    def log_message(self, format, *args):
        # Clean console log
        sys.stderr.write(f"[AI-SERVICE-LOG] {self.address_string()} - {format % args}\n")

def run():
    server_address = ("localhost", PORT)
    httpd = HTTPServer(server_address, AiModelHandler)
    print(f"Mock AI Model Service listening on http://localhost:{PORT}")
    print(f"Endpoint: POST http://localhost:{PORT}/api/v1/ai/chat")
    print("Press Ctrl+C to stop.\n")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down Mock AI Service.")
        httpd.server_close()

if __name__ == "__main__":
    run()
