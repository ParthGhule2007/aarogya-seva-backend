# AI Model Service Contract & Implementation Specification

> **Audience**: AI / Machine Learning Engineer  
> **Platform**: Healthcare Platform Chatbot  
> **Consumer Microservice**: `CHATBOT-SERVICE` (Spring Boot, Port `8085`)

---

## 1. Overview & Architecture

The **CHATBOT-SERVICE** connects the patient dashboard to your AI model.

```
+-----------------+              +-------------------+              +--------------------+
|  User Dashboard |  (REST/Text) |  CHATBOT-SERVICE  |  (HTTP/REST) |  AI Model Service  |
|  (Web/Mobile)   | -----------> |   (Spring Boot)   | -----------> |   (Your Service)   |
|                 | <----------- |    Port: 8085     | <----------- |     Port: 8087     |
+-----------------+              +-------------------+              +--------------------+
```

### Separation of Responsibilities:
- **CHATBOT-SERVICE**:
  - Creates and manages conversations in PostgreSQL (schema: `chatbot`).
  - Stores user messages (`USER` role) and AI responses (`ASSISTANT` role).
  - Fetches the user's age and conversation history.
  - Calls your AI service via HTTP.
- **AI Model Service (Your Service)**:
  - Exposes an HTTP endpoint on port `8087`.
  - Receives the user's `age`, `message`, and recent `conversationHistory`.
  - Generates an age-tailored, medically context-aware text response.
  - Returns a clean JSON response containing the text.

---

## 2. API Endpoint Specification

- **HTTP Method**: `POST`
- **Path**: `/api/v1/ai/chat`
- **Default Port**: `8087` (`http://localhost:8087/api/v1/ai/chat`)
- **Headers**:
  ```http
  Content-Type: application/json
  Accept: application/json
  ```

---

## 3. Request Payload (What CHATBOT-SERVICE Sends to You)

```json
{
  "age": 24,
  "message": "I am having stomach pain",
  "conversationHistory": [
    {
      "role": "USER",
      "content": "I have been feeling weak since morning"
    },
    {
      "role": "ASSISTANT",
      "content": "Are you experiencing any other symptoms?"
    }
  ]
}
```

### Field Definitions:

| Field | Type | Required | Description |
|---|---|---|---|
| `age` | `Integer` | **Yes** | Patient's age in years. **Important**: Your model prompt or medical logic should account for age (e.g., pediatric vs. adult vs. geriatric care considerations). |
| `message` | `String` | **Yes** | The current message typed or spoken by the patient. |
| `conversationHistory` | `Array[Object]` | **Yes** | Chronological list of prior messages in the conversation (up to the latest 20 messages). May be empty `[]` for the first message. |
| `conversationHistory[i].role` | `String` | **Yes** | Either `"USER"` or `"ASSISTANT"`. |
| `conversationHistory[i].content` | `String` | **Yes** | The text content of that previous message. |

> **Privacy Note**: `userId` and `username` are intentionally **NOT** sent to your model to ensure patient data minimization and privacy compliance.

---

## 4. Response Payload (What Your Service Must Return)

Your service must return HTTP status `200 OK` with the following JSON structure:

```json
{
  "success": true,
  "data": {
    "response": "Can you tell me how long you have been experiencing the pain, and is it sharp or dull?"
  }
}
```

### Response Rules:
1. **Plain Text**: `data.response` must be a plain text string.
2. **Do NOT Return**:
   - Audio files or audio URLs (frontend handles text-to-speech / speech-to-text).
   - HTML or markdown tables.
   - User profile data or database IDs.
3. **Latency**: Please ensure response latency is ideally under 5–10 seconds. The Chatbot Service has an HTTP timeout of 10 seconds (configurable).

---

## 5. Error Handling

If an error occurs inside your model pipeline (e.g., out-of-memory, context length exceeded, inference failure):
- Return HTTP status `500 Internal Server Error` (or `400` for bad input) with:
  ```json
  {
    "success": false,
    "message": "Inference failed: <brief error description>"
  }
  ```
The Chatbot Service will cleanly catch this and return a `502 Bad Gateway` to the frontend without leaking stack traces.

---

## 6. Python Starter Code (FastAPI Implementation Template)

You can copy and run this template directly in Python:

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List
import uvicorn

app = FastAPI(title="Healthcare AI Model Service", version="1.0.0")

class HistoryItem(BaseModel):
    role: str = Field(..., description="Role: USER or ASSISTANT")
    content: str = Field(..., description="Text message content")

class AiChatRequest(BaseModel):
    age: int = Field(..., description="Patient age in years")
    message: str = Field(..., description="Current message from user")
    conversationHistory: List[HistoryItem] = Field(default=[], description="Chronological history")

class AiResponseData(BaseModel):
    response: str

class AiChatResponse(BaseModel):
    success: bool = True
    data: AiResponseData

# TODO: Load your model here (HuggingFace pipeline, PyTorch, LangChain, etc.)
# model = load_trained_model(...)

@app.post("/api/v1/ai/chat", response_model=AiChatResponse)
async def chat(request: AiChatRequest):
    try:
        # Build prompt from conversationHistory + current message + age
        prompt_context = f"Patient Age: {request.age}\n"
        for item in request.conversationHistory:
            prompt_context += f"{item.role}: {item.content}\n"
        prompt_context += f"USER: {request.message}\nASSISTANT:"

        # Generate model prediction:
        # prediction_text = model.generate(prompt_context)
        
        # Example placeholder:
        prediction_text = (
            f"Based on your age of {request.age}, for symptoms like '{request.message}', "
            f"I recommend resting and drinking plenty of fluids. How long have you felt this way?"
        )

        return AiChatResponse(
            success=True,
            data=AiResponseData(response=prediction_text)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8087)
```

To run this:
```bash
pip install fastapi uvicorn pydantic
python ai_server.py
```
