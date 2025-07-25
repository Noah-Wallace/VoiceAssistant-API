# Voice Assistant API – Civic Service Chatbot Interface

This project delivers a voice-driven assistant for navigating civic service workflows like checking vehicle status, renewing licenses, or accessing notifications. Users can speak commands instead of navigating complex forms. Built with modular Java EE, this assistant combines natural language understanding with RESTful APIs and a clean UI.

---

## Features

* **Voice-Powered Interface**: Users speak commands like “Check my vehicle status” or “Renew my license.”
* **Intent-Aware API**: `/api/chat` processes speech transcripts, classifies intent, and extracts actionable parameters.
* **Real-Life Use Cases**: Supports actions such as booking appointments, fetching notifications, checking challan status, and more.
* **JSF Frontend Integration**: Built-in voice button for speech input, seamless user experience.

---

## 🛠️ Quick Setup (Tomcat 9 + Jersey 2.x)

1. **Clone & Open in NetBeans**
2. **Train the Intent Classifier**

   * Update `intent.train` with training samples.
   * Run OpenNLP:

     ```bash
     bin/opennlp DoccatTrainer -model intent-classifier.bin -lang en -data intent.train -encoding UTF-8
     ```
   * Save `intent-classifier.bin` in `src/main/resources`.
3. **Build & Deploy**

   * `mvn clean package`
   * Deploy WAR to Tomcat 9
4. **Try It**

   * Load the JSF page → click “Speak”
   * Or POST to `/voiceassistantapi/api/chat`:

     ```json
     { "transcript": "Check vehicle details for MH12AB1234" }
     ```

---

## 🎤 Supported Commands

* “Renew my license”
* “Pay my road tax”
* “Check vehicle details for MH12AB1234”
* “Apply for NOC”
* “Download my RC”
* “Update address/contact”
* “Book a driving test”
* “Show notifications”
* “Delete account”
* “Track delivery”
* “Schedule appointment”

---

## Intent Detection Workflow

### NLP Model (OpenNLP)

* Uses a trained DoccatModel (`intent-classifier.bin`) to predict the user's intent.
* Fallback to rule-based detection when confidence < 0.6.

### Rule-Based Fallback

* **Keyword Matching**
* **Synonym Expansion** (via WordNet/JWNL)
* **Fuzzy Matching** (Levenshtein distance)

### Output

```json
{
  "intent": "renew_license",
  "confidence": 0.88,
  "reply": "Sure! Let's renew your license.",
  "action": "start_license_renewal",
  "parameters": {
    "vehicle_number": "MH12AB1234"
  }
}
```

---

## Add New Commands

* Append training samples to `intent.train`
* Retrain model using OpenNLP
* Update logic in `VoiceInterpretResource.java` if needed

---

## ⚙️ Advanced Capabilities

* **Multilingual Support**: Add `intent-hi.train`, `intent-es.train`, etc.
* **CI/CD**: GitHub Actions automates build + model validation
* **Synonym Logic**: Powered by JWNL
* **Fuzzy Matching**: Apache Commons Text
* **Unknown Query Logging**: Tracks unmatched queries for analytics

---

## 💻 Project Structure

* `/src/main/java` – API logic, model loader, intent parser
* `/src/main/webapp` – JSF views, JavaScript, CSS
* `/src/main/resources` – OpenNLP models, training data

---

## 📊 Architecture Diagram

```
[Transcript Input]
       ↓
 [Preprocessing]
       ↓
[OpenNLP Classifier]
     ↓     ↘
[Intent Match] → [Response JSON]
        ↓
[Fallback Matching]
        ↓
[Unknown Intent Logging]
```

---

## 📫 Developer

**Ayesha Tariq**
📧 [ayeshatariq.lmgc@gmail.com](mailto:ayeshatariq.lmgc@gmail.com)
🔗 GitHub: [github.com/ayesha-tariq-dev](https://github.com/ayesha-tariq-dev)

---

## 📌 Disclaimer

This project was developed independently as part of a civic tech internship. The repository contains original code, **not affiliated with or sourced from any government system**. No proprietary or confidential data is included.

---
