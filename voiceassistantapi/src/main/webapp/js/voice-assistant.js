// voice-assistant.js
// Web Speech API integration for JSF pages

class VoiceAssistant {
    constructor({ onResult, onReply }) {
        this.recognition = null;
        this.synth = window.speechSynthesis;
        this.onResult = onResult;
        this.onReply = onReply;
        this.setupRecognition();
    }

    setupRecognition() {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            alert('SpeechRecognition not supported in this browser.');
            return;
        }
        this.recognition = new SpeechRecognition();
        this.recognition.lang = 'en-US';
        this.recognition.interimResults = false;
        this.recognition.maxAlternatives = 1;
        this.recognition.onresult = (event) => {
            const transcript = event.results[0][0].transcript;
            if (this.onResult) this.onResult(transcript);
        };
        this.recognition.onerror = (event) => {
            alert('Speech recognition error: ' + event.error);
        };
    }

    startListening() {
        if (this.recognition) this.recognition.start();
    }

    speak(text) {
        if (!this.synth) return;
        const utter = new SpeechSynthesisUtterance(text);
        this.synth.speak(utter);
    }

    sendTranscript(transcript) {
        fetch('/api/voice/interpret', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ transcript })
        })
        .then(res => res.json())
        .then(data => {
            if (this.onReply) this.onReply(data);
            if (data.reply) this.speak(data.reply);
            // Pass parameters to handleAction
            if (data.action) this.handleAction(data.action, data.parameters || {});
        });
    }

    handleAction(action, parameters = {}) {
        // Navigation and UI actions based on action code
        switch (action) {
            case 'RENEW_LICENSE':
                window.location.href = 'renewLicense.xhtml';
                break;
            case 'CHECK_STATUS':
                window.location.href = 'status.xhtml';
                break;
            case 'GO_HOME':
                window.location.href = 'index.html';
                break;
            case 'HELP':
                alert('Help: You can say things like renew license, check status, go home, logout, check vehicle details, pay tax, book test, download RC, check challan, apply NOC, update address, get permit.');
                break;
            case 'LOGOUT':
                window.location.href = 'logout.xhtml';
                break;
            case 'CHECK_VEHICLE':
                if (parameters.vehicleNumber) {
                    alert('Vehicle details for: ' + parameters.vehicleNumber);
                    // Optionally, fetch and display vehicle info here
                } else {
                    alert('Please specify a vehicle number.');
                }
                break;
            case 'PAY_TAX':
                window.location.href = 'payTax.xhtml';
                break;
            case 'BOOK_TEST':
                window.location.href = 'bookTest.xhtml';
                break;
            case 'DOWNLOAD_RC':
                window.location.href = 'downloadRC.xhtml';
                break;
            case 'CHECK_CHALLAN':
                window.location.href = 'challanStatus.xhtml';
                break;
            case 'APPLY_NOC':
                window.location.href = 'applyNOC.xhtml';
                break;
            case 'UPDATE_ADDRESS':
                window.location.href = 'updateAddress.xhtml';
                break;
            case 'GET_PERMIT':
                window.location.href = 'permitInfo.xhtml';
                break;
            default:
                // No navigation
        }
    }
}

// Usage example (to be included in JSF pages):
// const va = new VoiceAssistant({
//     onResult: (transcript) => va.sendTranscript(transcript),
//     onReply: (data) => { /* custom UI update */ }
// });
// document.getElementById('speakBtn').onclick = () => va.startListening();
