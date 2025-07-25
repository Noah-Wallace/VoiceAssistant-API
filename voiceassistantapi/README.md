# Voice Assistant API for Parivahan 

This project brings a smart, voice-driven experience to Parivahan workflows. Speak your needs, and let the assistant handle the rest—whether it's checking your vehicle status, renewing your license, or paying your road tax. The app combines modern Java EE, NLP, and a friendly UI to make your life easier.

---

## Features?
- **Talk to Your Assistant:** Use your voice to navigate, get info, and trigger actions—no more endless clicking.
- **Rich REST API:** The `/api/chat` endpoint understands your intent and extracts details from your speech.
- **Real-World Intents:** From "renew my license" to "show my notifications"—the assistant covers a wide range of real Parivahan tasks.
- **Seamless Navigation:** Voice commands can move you between pages, fetch data, or start a process.

## How to Get Started (Tomcat 9 + Jersey 2.x)
1. **Clone the Repo & Open in NetBeans**
2. **Train the Assistant**
   - Edit `src/main/resources/intent.train` to add or tweak what the assistant understands.
   - Train your model with OpenNLP:
     ```cmd
     bin\opennlp.bat DoccatTrainer -model intent-classifier.bin -lang en -data intent.train -encoding UTF-8
     ```
   - Place the resulting `intent-classifier.bin` in `src/main/resources`.
3. **Build & Deploy**
   - Run `mvn clean package`.
   - Deploy the WAR to Tomcat 9 (Servlet 4.x, Java EE 8).
4. **Try It Out!**
   - Open a JSF page, click “Speak”, and say something like “Check vehicle details for MH12AB1234”.
   - Or use Postman/curl to POST to `/voiceassistantapi/api/chat`:
     ```json
     { "transcript": "Check vehicle details for MH12AB1234" }
     ```

## Example Commands
- “Renew my license”
- “Check vehicle details for MH12AB1234”
- “Pay my road tax”
- “Book a driving test”
- “Download my RC”
- “Check challan status”
- “Apply for NOC”
- “Update my address”
- “Get permit information”
- “Update my phone/email”
- “Show notifications”
- “Delete my account”
- “Track delivery”
- “Schedule appointment”
- “Show balance”
- “Transfer funds”
- “Show transactions”
- “Update profile picture”

## How to Add New Voice Commands
- Add new lines to `intent.train` for your use case.
- Retrain the model and replace `intent-classifier.bin`.
- Update the backend (`VoiceInterpretResource.java`) for new actions if needed.
- You can also add simple keyword-based or NLP-based cases in the code.

## Advanced Features
- **Fuzzy Matching:** Tolerant to typos and similar words using Levenshtein distance (Apache Commons Text).
- **Synonym Expansion:** Uses WordNet (JWNL) for real synonym matching—covers more user expressions.
- **CI/CD Integration:** GitHub Actions workflow for build, test, and (optionally) model retraining.
- **Multilingual Support:** Prepare new `intent-<lang>.train` and retrain models for each language. Backend can be extended to auto-detect and use the correct model.
- **Unknown Intent Logging:** All unrecognized queries are logged for analytics and future improvements.
- **Automated Model Training:** Use `train-model.bat` to retrain and deploy the OpenNLP model in one step.

## Multilingual Support
- To add a new language:
  1. Create `src/main/resources/intent-<lang>.train` (e.g., `intent-hi.train` for Hindi).
  2. Retrain a model for that language and save as `intent-classifier-<lang>.bin`.
  3. Extend backend to load and use the correct model based on input language.

## CI/CD
- Automated build and test with GitHub Actions (`.github/workflows/maven.yml`).
- Optionally retrain and commit model if `intent.train` changes.

## Retrain the Model (Automated)
- Edit `src/main/resources/intent.train` as needed.
- Run:
  ```cmd
  train-model.bat
  ```
- This will retrain and copy the new model to the correct location.

## Tech Stack & Compatibility
- **Java EE 8 (`javax.*`)**—fully compatible with Tomcat 9.
- **Jersey 2.x** (2.41) for REST/JAX-RS.
- **Tomcat 9** (Servlet 4.x).
- **Jackson** for easy JSON <-> POJO mapping.
- All dependencies are locked to compatible versions using Maven BOM.

## Pro
- `src/main/java` – Java REST API (see `VoiceInterpretResource.java`, `TranscriptRequest.java`)
- `src/main/webapp` – JSF pages, JavaScript, CSS
- `src/main/resources` – NLP model and training data

## Developer & Contact
- Ayesha Tariq
- ayeshatariq.lmgc@gmail.com

---

# Voice Assistant API – Detailed Workflow

This document describes the **detailed workflow** of the Voice Assistant API, focusing on how user transcripts are processed, how intents are detected, and how responses are generated. This is intended for senior developers maintaining or extending the system.

---

## 1. Request Flow

1. **Client sends a POST request** to `/chat` endpoint with a JSON payload:
    ```json
    { "transcript": "I want to renew my license" }
    ```

2. **The API receives and parses** the transcript.

---

## 2. Preprocessing

- The transcript is normalized:
  - Punctuation is removed.
  - Extra spaces are trimmed.
  - Text is converted to lowercase.

---

## 3. Entity Extraction

- The system uses regex to extract entities (e.g., vehicle numbers like `MH12AB1234`) from the transcript.
- Extracted entities are added to the response parameters.

---

## 4. Intent Detection (Hybrid Approach)

### A. **NLP Model-Based Detection**
- The API attempts to load a pre-trained OpenNLP `DoccatModel` (`intent-classifier.bin`) at startup.
- If available:
  - The transcript is tokenized.
  - The model predicts the most likely intent.
  - If the confidence score is above 0.6, this intent is used.

### B. **Rule-Based Fallback**
If the NLP model is unavailable or not confident:
- The system iterates through a list of hardcoded `Intent` objects.
- For each intent:
  - **Direct Keyword Match:** Checks if the transcript contains any of the intent’s keywords.
  - **Synonym Expansion:** Uses WordNet to expand intent names into synonyms and checks for their presence.
  - **Fuzzy Matching:** Uses Levenshtein distance to allow for minor typos or variations.

---

## 5. Response Construction

- The system builds a JSON response containing:
  - `reply`: Assistant’s message to the user.
  - `action`: Action code for the frontend/backend.
  - `intent`: Detected intent.
  - `confidence`: Confidence score.
  - `parameters`: Extracted entities (if any).

---

## 6. Logging and Analytics

- If no intent is matched, the transcript is logged as "Unknown intent" for future analysis and model improvement.

---

## 7. Extending the System

- **To add new intents:**
  - Add training examples to `intent.train` and retrain the OpenNLP model.
  - Add new `Intent` objects to the `INTENTS` array for rule-based fallback.
- **To improve detection:**
  - Enhance synonym expansion or fuzzy matching logic.
  - Add more entity extraction patterns as needed.

---

## 8. Error Handling

- Any exceptions during processing are caught and returned as a server error with a JSON error message.

---

## 9. Model Training (Offline)

- The `intent.train` file (TSV) is used to train the OpenNLP model externally.
- The resulting `intent-classifier.bin` is placed in the resources directory for runtime use.

---

## 10. Summary Diagram

```
Client Transcript
      |
      v
[Preprocessing]
      |
      v
[NLP Model] --(confident?)--> [Intent & Reply]
      | no
      v
[Keyword/Synonym/Fuzzy Match] --(matched?)--> [Intent & Reply]
      | no
      v
[Unknown Intent]
```

---

**For further details, refer to the `VoiceInterpretResource.java` implementation.**

