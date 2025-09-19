package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.models.EventRegistration;
import tn.esprit.models.Events;
import tn.esprit.services.ServiceEventRegistration;
import tn.knowlity.entity.User;
import tn.knowlity.tools.UserSessionManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatbotController {

    @FXML private VBox chatHeader;
    @FXML private ImageView chatbotLogo;
    @FXML private Label logoText;
    @FXML private Button closeChatbot;
    @FXML private ScrollPane chatBody;
    @FXML private VBox messageContainer;
    @FXML private HBox chatFooter;
    @FXML private TextArea messageInput;
    @FXML private Button sendMessage;

    private Stage stage;
    private static final String API_KEY = "AIzaSyBHhllR3_CLW-9EsTxGxsIrFQGog0DbF0c";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent?key=" + API_KEY;
    private List<ChatMessage> chatHistory;
    private User user = UserSessionManager.getInstance().getCurrentUser();


    public void initialize() {
        chatHistory = new ArrayList<>();
        Events event;
        ServiceEventRegistration serviceEventRegistration = new ServiceEventRegistration();
        List<EventRegistration> eventRegistrations = serviceEventRegistration.getAll();
        eventRegistrations=serviceEventRegistration.getByUserId(user.getId());
        String registration= "list of events :";
        for (EventRegistration eventRegistration : eventRegistrations) {
            event = eventRegistration.getEvent();
            registration+=event.toString();
        }
        System.out.println(registration);
        chatHistory.add(new ChatMessage("user", "Context: You are an AI assistant for Knowlity. This website is about online education. \n" +
                "            Your job is to help users navigate and understand its features.\n" +
                "\n" +
                "            Here is some basic information about the current user: \n" +
                "            - Name: "+ user.getNom() +
                "            - Role: "+ Arrays.toString(user.getRoles()) +
                "\n" +
                "            his/her Courses: \n" +
                "            java , python \n" +
                "\n" +
                "his/her Events: \n" +
                    registration+
                "            Additional Notes:\n" +
                "            - If the user needs help with courses, guide them on enrollment, pricing, or available subjects.\n" +
                "            - If the user asks about events, provide details about schedules, participation, and deadlines.\n" +
                "            - If they ask unrelated questions, you can provide general knowledge, but always prioritize website-related queries."));

        addBotMessage("Hey there\nHow can I help you today?");
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void closeChatbot() {
        stage.close();
    }

    @FXML
    private void handleSendMessage() {
        String userMessage = messageInput.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        addUserMessage(userMessage);
        messageInput.clear();

        HBox thinkingMessage = addBotMessage("...");
        thinkingMessage.getStyleClass().add("thinking");

        generateBotResponse(userMessage, thinkingMessage);
    }

    private void addUserMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setSpacing(10);
        messageBox.getStyleClass().add("user-message");

        VBox messageContent = new VBox(2);
        Label messageText = new Label(message);
        messageText.setWrapText(true);
        messageText.setMaxWidth(300);
        messageText.setStyle("-fx-background-color: #5a7ca3; -fx-text-fill: white; -fx-padding: 12 16 12 16; -fx-background-radius: 13 13 3 13;");

        Label timestamp = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timestamp.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        messageContent.getChildren().addAll(messageText, timestamp);
        messageBox.getChildren().add(messageContent);
        messageContainer.getChildren().add(messageBox);
        scrollToBottom();
    }

    private HBox addBotMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setSpacing(10);
        messageBox.getStyleClass().add("bot-message");

        ImageView botAvatar = new ImageView(new Image(getClass().getResourceAsStream("/images/robotic.png")));
        botAvatar.setFitHeight(35);
        botAvatar.setFitWidth(35);

        VBox messageContent = new VBox(2);
        Label messageText = new Label(message);
        messageText.setWrapText(true);
        messageText.setMaxWidth(300);
        messageText.setStyle("-fx-background-color: rgba(0, 0, 0, 0.075); -fx-background-radius: 13 13 13 3; -fx-padding: 12 16 12 16;");

        Label timestamp = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timestamp.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        messageContent.getChildren().addAll(messageText, timestamp);
        messageBox.getChildren().addAll(botAvatar, messageContent);
        messageContainer.getChildren().add(messageBox);
        scrollToBottom();
        return messageBox;
    }

    private void scrollToBottom() {
        chatBody.setVvalue(1.0);
    }

    private void generateBotResponse(String userMessage, HBox thinkingMessage) {
        chatHistory.add(new ChatMessage("user", userMessage));

        String requestBody = buildRequestBody();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    try {
                        String botResponse = extractBotResponse(response);
                        Platform.runLater(() -> {
                            messageContainer.getChildren().remove(thinkingMessage);
                            addBotMessage(botResponse);
                            chatHistory.add(new ChatMessage("model", botResponse));
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            messageContainer.getChildren().remove(thinkingMessage);
                            addBotMessage("Sorry, I'm having trouble responding. Please try again.");
                        });
                    }
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        messageContainer.getChildren().remove(thinkingMessage);
                        addBotMessage("Sorry, I'm having trouble responding. Please try again.");
                    });
                    return null;
                });
    }

    private String buildRequestBody() {
        StringBuilder json = new StringBuilder("{\"contents\": [");
        for (int i = 0; i < chatHistory.size(); i++) {
            ChatMessage msg = chatHistory.get(i);
            json.append("{\"role\": \"").append(msg.getRole()).append("\", \"parts\": [{\"text\": \"")
                    .append(msg.getText().replace("\"", "\\\"")).append("\"}]}");
            if (i < chatHistory.size() - 1) {
                json.append(",");
            }
        }
        json.append("]}");
        return json.toString();
    }

    private String extractBotResponse(String response) {
        int startIndex = response.indexOf("\"text\": \"") + 9;
        int endIndex = response.indexOf("\"", startIndex);
        return response.substring(startIndex, endIndex);
    }

    private static class ChatMessage {
        private String role;
        private String text;

        public ChatMessage(String role, String text) {
            this.role = role;
            this.text = text;
        }

        public String getRole() {
            return role;
        }

        public String getText() {
            return text;
        }
    }
}