package com.voiceassistantapi.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import java.io.InputStream;
import java.io.IOException;
import org.apache.commons.text.similarity.LevenshteinDistance;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.IndexWord;
import java.util.HashSet;

/**
 * REST endpoint for interpreting voice commands.
 * Accepts a transcript and returns a reply and action code.
 */
@Path("/chat")
public class VoiceInterpretResource {
    private static class Intent {
        String action;
        String reply;
        Predicate<String> matcher;
        Intent(String action, String reply, Predicate<String> matcher) {
            this.action = action;
            this.reply = reply;
            this.matcher = matcher;
        }
    }

    private static final Intent[] INTENTS = new Intent[] {
        new Intent("RENEW_LICENSE", "Sure, I can help you renew your license.",
            t -> t.contains("renew license") || t.contains("license renewal") || t.contains("renew my license") || t.contains("renewal of license") || t.contains("renewal license")),
        new Intent("CHECK_STATUS", "Checking your application status.",
            t -> t.contains("check status") || t.contains("application status") || t.contains("status of my application") || t.contains("track application") || t.contains("application progress")),
        new Intent("UPDATE_PHONE", "Updating your phone number.",
            t -> t.contains("update phone") || t.contains("change phone") || t.contains("edit phone") || t.contains("modify phone number") || t.contains("phone update")),
        new Intent("SHOW_PROFILE", "Here is your profile information.",
            t -> t.contains("show profile") || t.contains("my profile") || t.contains("profile info") || t.contains("display profile") || t.contains("profile details")),
        new Intent("CANCEL_APPLICATION", "Cancelling your application.",
            t -> t.contains("cancel application") || t.contains("withdraw application") || t.contains("delete application") || t.contains("remove application")),
        new Intent("RESET_PASSWORD", "Resetting your password.",
            t -> t.contains("reset password") || t.contains("forgot password") || t.contains("change password") || t.contains("password reset")),
        new Intent("CONTACT_SUPPORT", "Connecting you to support.",
            t -> t.contains("contact support") || t.contains("help desk") || t.contains("customer support") || t.contains("get help") || t.contains("support team")),
        new Intent("PAY_FEES", "Redirecting to fee payment.",
            t -> t.contains("pay fees") || t.contains("fee payment") || t.contains("make payment") || t.contains("pay my fees")),
        new Intent("GET_RECEIPT", "Fetching your receipt.",
            t -> t.contains("get receipt") || t.contains("download receipt") || t.contains("show receipt") || t.contains("receipt copy")),
        new Intent("UPDATE_EMAIL", "Updating your email address.",
            t -> t.contains("update email") || t.contains("change email") || t.contains("edit email") || t.contains("modify email address")),
        new Intent("SHOW_NOTIFICATIONS", "Here are your notifications.",
            t -> t.contains("show notifications") || t.contains("my notifications") || t.contains("display notifications") || t.contains("notification list")),
        new Intent("DELETE_ACCOUNT", "Deleting your account.",
            t -> t.contains("delete account") || t.contains("remove account") || t.contains("close account") || t.contains("account deletion")),
        new Intent("TRACK_DELIVERY", "Tracking your delivery.",
            t -> t.contains("track delivery") || t.contains("delivery status") || t.contains("where is my delivery") || t.contains("delivery tracking")),
        new Intent("SCHEDULE_APPOINTMENT", "Scheduling your appointment.",
            t -> t.contains("schedule appointment") || t.contains("book appointment") || t.contains("make appointment") || t.contains("appointment booking")),
        new Intent("SHOW_BALANCE", "Here is your current balance.",
            t -> t.contains("show balance") || t.contains("account balance") || t.contains("display balance") || t.contains("balance info")),
        new Intent("TRANSFER_FUNDS", "Transferring funds.",
            t -> t.contains("transfer funds") || t.contains("send money") || t.contains("fund transfer") || t.contains("move money")),
        new Intent("SHOW_TRANSACTIONS", "Here are your recent transactions.",
            t -> t.contains("show transactions") || t.contains("transaction history") || t.contains("recent transactions") || t.contains("display transactions")),
        new Intent("UPDATE_PROFILE_PICTURE", "Updating your profile picture.",
            t -> t.contains("update profile picture") || t.contains("change profile picture") || t.contains("edit profile picture") || t.contains("profile photo update")),
        // Add more intents here as needed
    };

    private static DoccatModel nlpModel = null;
    static {
        try (InputStream modelIn = VoiceInterpretResource.class.getResourceAsStream("/intent-classifier.bin")) {
            if (modelIn != null) {
                nlpModel = new DoccatModel(modelIn);
            }
        } catch (IOException e) {
            // Model not available, fallback to keyword
        }
    }

    private static String nlpDetectIntent(String transcript) {
        if (nlpModel == null) return null;
        DocumentCategorizerME categorizer = new DocumentCategorizerME(nlpModel);
        String[] tokens = transcript.split("\\s+");
        double[] outcomes = categorizer.categorize(tokens);
        String category = categorizer.getBestCategory(outcomes);
        double bestScore = outcomes[categorizer.getIndex(category)];
        if (bestScore > 0.6) return category;
        return null;
    }

    private static final Pattern VEHICLE_PATTERN = Pattern.compile("[A-Z]{2}\\d{2}[A-Z]{2}\\d{4}");

    private static String preprocessTranscript(String transcript) {
        // Remove punctuation, extra spaces, and convert to lower case
        String normalized = transcript.replaceAll("[\\p{Punct}]", " ")
                                      .replaceAll("\\s+", " ")
                                      .trim()
                                      .toLowerCase(Locale.ENGLISH);
        // Add more normalization/typo correction here if needed
        return normalized;
    }

    private static final Logger LOGGER = Logger.getLogger(VoiceInterpretResource.class.getName());
    private static final LevenshteinDistance LEVENSHTEIN = new LevenshteinDistance();
    // Placeholder for synonym expansion (e.g., via JWNL/WordNet)
    private static Dictionary wordnetDictionary = null;
    static {
        try {
            wordnetDictionary = Dictionary.getDefaultResourceInstance();
        } catch (JWNLException e) {
            LOGGER.warning("WordNet dictionary not initialized: " + e.getMessage());
        }
    }
    // Real synonym expansion using WordNet
    private static List<String> expandSynonyms(String phrase) {
        List<String> synonyms = new ArrayList<>();
        synonyms.add(phrase); // Always include the original phrase
        if (wordnetDictionary == null) return synonyms;
        try {
            String[] words = phrase.split(" ");
            for (String word : words) {
                IndexWord iw = wordnetDictionary.lookupIndexWord(POS.VERB, word);
                if (iw != null) {
                    iw.getSenses().forEach(synset -> synset.getWords().forEach(w -> synonyms.add(w.getLemma().replace('_', ' '))));
                }
                iw = wordnetDictionary.lookupIndexWord(POS.NOUN, word);
                if (iw != null) {
                    iw.getSenses().forEach(synset -> synset.getWords().forEach(w -> synonyms.add(w.getLemma().replace('_', ' '))));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("WordNet synonym expansion failed: " + e.getMessage());
        }
        // Remove duplicates
        return new ArrayList<>(new HashSet<>(synonyms));
    }
    // Fuzzy match utility
    private static boolean fuzzyMatch(String input, String keyword) {
        int threshold = 2; // Allow up to 2 edits (tune as needed)
        return LEVENSHTEIN.apply(input, keyword) <= threshold;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response interpret(String inputStr) {
        try (javax.json.JsonReader reader = javax.json.Json.createReader(new java.io.StringReader(inputStr))) {
            javax.json.JsonObject input = reader.readObject();
            String transcript = preprocessTranscript(input.getString("transcript", ""));
            String reply = "Sorry, I didn't understand.";
            String action = "UNKNOWN";
            String intent = "UNKNOWN";
            double confidence = 0.0;
            JsonObjectBuilder params = Json.createObjectBuilder();

            // Entity extraction example (vehicle number)
            Matcher vehicleMatcher = VEHICLE_PATTERN.matcher(transcript.toUpperCase());
            if (vehicleMatcher.find()) {
                params.add("vehicleNumber", vehicleMatcher.group());
            }

            // NLP-based intent detection
            String nlpIntent = nlpDetectIntent(transcript);
            if (nlpIntent != null) {
                intent = nlpIntent;
                switch (nlpIntent) {
                    case "RENEW_LICENSE":
                        reply = "Sure, I can help you renew your license.";
                        action = "RENEW_LICENSE";
                        confidence = 0.9;
                        break;
                    case "CHECK_STATUS":
                        reply = "Checking your application status.";
                        action = "CHECK_STATUS";
                        confidence = 0.9;
                        break;
                    case "GO_HOME":
                        reply = "Navigating to the home page.";
                        action = "GO_HOME";
                        confidence = 0.9;
                        break;
                    case "HELP":
                        reply = "Here is some help information.";
                        action = "HELP";
                        confidence = 0.9;
                        break;
                    case "LOGOUT":
                        reply = "Logging you out.";
                        action = "LOGOUT";
                        confidence = 0.9;
                        break;
                    case "CHECK_VEHICLE":
                        reply = "Fetching vehicle details.";
                        action = "CHECK_VEHICLE";
                        confidence = 0.9;
                        break;
                    case "PAY_TAX":
                        reply = "Redirecting to road tax payment.";
                        action = "PAY_TAX";
                        confidence = 0.9;
                        break;
                    case "BOOK_TEST":
                        reply = "Booking your driving test.";
                        action = "BOOK_TEST";
                        confidence = 0.9;
                        break;
                    case "DOWNLOAD_RC":
                        reply = "Downloading your Registration Certificate.";
                        action = "DOWNLOAD_RC";
                        confidence = 0.9;
                        break;
                    case "CHECK_CHALLAN":
                        reply = "Checking your challan status.";
                        action = "CHECK_CHALLAN";
                        confidence = 0.9;
                        break;
                    case "APPLY_NOC":
                        reply = "Applying for NOC.";
                        action = "APPLY_NOC";
                        confidence = 0.9;
                        break;
                    case "UPDATE_ADDRESS":
                        reply = "Updating your address.";
                        action = "UPDATE_ADDRESS";
                        confidence = 0.9;
                        break;
                    case "GET_PERMIT":
                        reply = "Fetching permit information.";
                        action = "GET_PERMIT";
                        confidence = 0.9;
                        break;
                    case "UPDATE_PHONE":
                        reply = "Updating your phone number.";
                        action = "UPDATE_PHONE";
                        confidence = 0.9;
                        break;
                    case "SHOW_PROFILE":
                        reply = "Here is your profile information.";
                        action = "SHOW_PROFILE";
                        confidence = 0.9;
                        break;
                    case "CANCEL_APPLICATION":
                        reply = "Cancelling your application.";
                        action = "CANCEL_APPLICATION";
                        confidence = 0.9;
                        break;
                    case "RESET_PASSWORD":
                        reply = "Resetting your password.";
                        action = "RESET_PASSWORD";
                        confidence = 0.9;
                        break;
                    case "CONTACT_SUPPORT":
                        reply = "Connecting you to support.";
                        action = "CONTACT_SUPPORT";
                        confidence = 0.9;
                        break;
                    case "PAY_FEES":
                        reply = "Redirecting to fee payment.";
                        action = "PAY_FEES";
                        confidence = 0.9;
                        break;
                    case "GET_RECEIPT":
                        reply = "Fetching your receipt.";
                        action = "GET_RECEIPT";
                        confidence = 0.9;
                        break;
                    case "UPDATE_EMAIL":
                        reply = "Updating your email address.";
                        action = "UPDATE_EMAIL";
                        confidence = 0.9;
                        break;
                    case "SHOW_NOTIFICATIONS":
                        reply = "Here are your notifications.";
                        action = "SHOW_NOTIFICATIONS";
                        confidence = 0.9;
                        break;
                    case "DELETE_ACCOUNT":
                        reply = "Deleting your account.";
                        action = "DELETE_ACCOUNT";
                        confidence = 0.9;
                        break;
                    case "TRACK_DELIVERY":
                        reply = "Tracking your delivery.";
                        action = "TRACK_DELIVERY";
                        confidence = 0.9;
                        break;
                    case "SCHEDULE_APPOINTMENT":
                        reply = "Scheduling your appointment.";
                        action = "SCHEDULE_APPOINTMENT";
                        confidence = 0.9;
                        break;
                    case "SHOW_BALANCE":
                        reply = "Here is your current balance.";
                        action = "SHOW_BALANCE";
                        confidence = 0.9;
                        break;
                    case "TRANSFER_FUNDS":
                        reply = "Transferring funds.";
                        action = "TRANSFER_FUNDS";
                        confidence = 0.9;
                        break;
                    case "SHOW_TRANSACTIONS":
                        reply = "Here are your recent transactions.";
                        action = "SHOW_TRANSACTIONS";
                        confidence = 0.9;
                        break;
                    case "UPDATE_PROFILE_PICTURE":
                        reply = "Updating your profile picture.";
                        action = "UPDATE_PROFILE_PICTURE";
                        confidence = 0.9;
                        break;
                    // ...add more intents as needed...
                }
            } else {
                // Fallback to keyword-based, synonym, and fuzzy matching
                boolean matched = false;
                for (Intent intentObj : INTENTS) {
                    // Try direct keyword match
                    if (intentObj.matcher.test(transcript)) {
                        reply = intentObj.reply;
                        action = intentObj.action;
                        intent = intentObj.action;
                        confidence = 1.0;
                        matched = true;
                        break;
                    }
                    // Try synonym expansion (placeholder)
                    for (String synonym : expandSynonyms(intentObj.action.replace("_", " ").toLowerCase())) {
                        if (transcript.contains(synonym)) {
                            reply = intentObj.reply;
                            action = intentObj.action;
                            intent = intentObj.action;
                            confidence = 0.95;
                            matched = true;
                            break;
                        }
                        // Fuzzy match
                        if (fuzzyMatch(transcript, synonym)) {
                            reply = intentObj.reply;
                            action = intentObj.action;
                            intent = intentObj.action;
                            confidence = 0.9;
                            matched = true;
                            break;
                        }
                    }
                    if (matched) break;
                }
                if (!matched) {
                    // Log unknown intent for analytics
                    LOGGER.warning("Unknown intent: " + transcript);
                }
            }
            JsonObject result = Json.createObjectBuilder()
                    .add("reply", reply)
                    .add("action", action)
                    .add("intent", intent)
                    .add("confidence", confidence)
                    .add("parameters", params)
                    .build();
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
