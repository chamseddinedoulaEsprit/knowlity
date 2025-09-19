package controllers;

import com.esprit.knowlity.controller.student.StudentController;
import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.models.Chapitre;
import tn.esprit.models.Cours;
import tn.esprit.services.ServiceCours;
import tn.esprit.services.ServiceInscription;
import tn.esprit.services.ServiceFavoris;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import tn.knowlity.entity.User;
import tn.knowlity.tools.UserSessionManager;

import java.awt.Desktop;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CourseDetailsControllerEtudiant {
    @FXML private AnchorPane root;
    @FXML private VBox mainBox;
    @FXML private Button backButton;
    @FXML private Button editButton;
    @FXML private ImageView courseImage;
    @FXML private Label courseTitle;
    @FXML private Label matiereBadge;
    @FXML private Label categorieBadge;
    @FXML private Label dureeLabel;
    @FXML private Label prixLabel;
    @FXML private Label langueLabel;
    @FXML private VBox paymentBox;
    @FXML private Label paymentLink;
    @FXML private Label descriptionLabel;
    @FXML private ImageView teacherImage;
    @FXML private Label teacherEmail;
    @FXML private Label favoritesLabel;
    @FXML private Button statsButton;
    @FXML private VBox chaptersContainer;
    @FXML private Button enrollButton;
    @FXML private Button favoriteButton;
    @FXML private FontAwesomeIconView favoriteIcon;

    private Cours course;
    private final ServiceCours serviceCours;
    private final ServiceInscription serviceInscription;
    private final ServiceFavoris serviceFavoris;
    private User user = UserSessionManager.getInstance().getCurrentUser();
    private final int DEFAULT_USER_ID = user.getId();
    private static final String GMAIL_USERNAME = "chamseddinedoula7@gmail.com";
    private static final String GMAIL_PASSWORD = "xlvmkpnbcrjbrysu";

    public CourseDetailsControllerEtudiant() {
        this.serviceCours = new ServiceCours();
        this.serviceInscription = new ServiceInscription();
        this.serviceFavoris = new ServiceFavoris();
    }

    public void setCourse(Cours course) {
        this.course = course;
        initializeUI();
        updateEnrollButtonState();
        updateFavoriteButtonState();
    }

    private void initializeUI() {
        if (course == null) {
            return;
        }

        // Set course details
        courseTitle.setText(course.getTitle());
        descriptionLabel.setText(course.getDescription());

        // Position the enroll button
        mainBox.getChildren().remove(enrollButton);
        mainBox.getChildren().add(2, enrollButton); // Add below description

        // Ensure stats button is removed
        if (mainBox.getChildren().contains(statsButton)) {
            mainBox.getChildren().remove(statsButton);
        }

        // Set category and subject badges
        if (course.getMatiere() != null) {
            matiereBadge.setText(course.getMatiere().getTitre());
            if (course.getMatiere().getCategorie() != null) {
                categorieBadge.setText(course.getMatiere().getCategorie().getName());
            }
        }

        // Calculate total duration from chapters
        int totalDuration = course.getChapitres().stream()
                           .mapToInt(Chapitre::getDureeEstimee)
                           .sum();
        dureeLabel.setText(totalDuration + " minutes");

        // Set other details
        prixLabel.setText(course.getPrix() + " DT");
        langueLabel.setText(course.getLangue());
        
        // Set favorites count
        int favoritesCount = serviceFavoris.getNombreFavoris(course.getId());
        favoritesLabel.setText(String.valueOf(favoritesCount));

        // Load course image with modern design
        try {
            Image image = new Image("file:Uploads/" + course.getUrlImage());
            courseImage.setImage(image);
            courseImage.setStyle("-fx-border-color: #ccc; -fx-border-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 0);");
        } catch (Exception e) {
            System.err.println("Error loading course image: " + e.getMessage());
            // Set a default image
            try {
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-course.png"));
                courseImage.setImage(defaultImage);
            } catch (Exception ex) {
                System.err.println("Error loading default image: " + ex.getMessage());
            }
        }

        // Set teacher information
        if (course.getEnseignant() != null) {
            String teacherName = course.getEnseignant().getPrenom() + " " + course.getEnseignant().getNom();
            teacherEmail.setText(teacherName);
        } else {
            teacherEmail.setText("Enseignant non assigné");
        }

        // Load teacher image
        loadImage(teacherImage,course.getEnseignant().getImage());

        // Vérifier si l'utilisateur est inscrit
        boolean isEnrolled = serviceInscription.estInscrit(DEFAULT_USER_ID, course.getId());

        // Supprimer les anciens boutons d'action s'ils existent
        mainBox.getChildren().removeIf(node -> node instanceof HBox && node.getId() != null && node.getId().equals("actionButtonsContainer"));

        // Si l'utilisateur est inscrit, ajouter les boutons d'évaluation et de quiz
        if (isEnrolled) {
            // Créer les boutons
            Button evaluationButton = new Button("Passer l'évaluation");
            evaluationButton.setOnAction(this::handleListeEvaluationsAction);
            evaluationButton.getStyleClass().addAll("action-button", "evaluation-button");
            evaluationButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 5;");

            Button quizButton = new Button("Passer les quiz");
            quizButton.setOnAction(this::handleListeQuizAction);
            quizButton.getStyleClass().addAll("action-button", "quiz-button");
            quizButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 5;");

            // Créer une HBox pour les boutons
            HBox actionButtons = new HBox(20); // 20 pixels d'espacement
            actionButtons.setId("actionButtonsContainer");
            actionButtons.setAlignment(Pos.CENTER);
            actionButtons.setPadding(new Insets(20, 0, 20, 0));
            actionButtons.getChildren().addAll(evaluationButton, quizButton);

            // Ajouter la HBox après la description du cours
            int descriptionIndex = mainBox.getChildren().indexOf(descriptionLabel);
            if (descriptionIndex >= 0) {
                mainBox.getChildren().add(descriptionIndex + 1, actionButtons);
            } else {
                mainBox.getChildren().add(actionButtons);
            }
        }

        // Load chapters only if enrolled
        if (isEnrolled) {
            loadChapters();
        } else {
            chaptersContainer.getChildren().clear();
            Label notEnrolledLabel = new Label("Inscrivez-vous pour accéder au contenu du cours");
            notEnrolledLabel.getStyleClass().add("not-enrolled-message");
            notEnrolledLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 16px; -fx-padding: 20;");
            chaptersContainer.getChildren().add(notEnrolledLabel);
        }
    }

    private void loadChapters() {
        chaptersContainer.getChildren().clear();
        List<Chapitre> chapters = course.getChapitres();
        
        for (int i = 0; i < chapters.size(); i++) {
            Chapitre chapitre = chapters.get(i);
            VBox chapterCard = createChapterCard(chapitre);
            
            // Ajouter l'animation avec délai
            PauseTransition delay = new PauseTransition(Duration.millis(i * 100));
            delay.setOnFinished(e -> chapterCard.getStyleClass().add("show"));
            delay.play();
            
            chaptersContainer.getChildren().add(chapterCard);
        }
    }

    private VBox createChapterCard(Chapitre chapitre) {
        VBox card = new VBox(15);
        card.getStyleClass().add("chapter-card");
        card.setMaxWidth(Double.MAX_VALUE);
        
        // En-tête du chapitre
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Numéro du chapitre avec style
        Label chapterNumber = new Label(String.format("%02d", chapitre.getChapOrder()));
        chapterNumber.getStyleClass().add("chapter-number");
        
        VBox contentBox = new VBox(8);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        
        // Titre du chapitre en noir
        Label title = new Label(chapitre.getTitle());
        title.getStyleClass().add("chapter-title");
        title.setStyle("-fx-text-fill: black;");
        title.setWrapText(true);
        
        // Description
        Label description = new Label(chapitre.getContenu());
        description.getStyleClass().add("chapter-description");
        description.setWrapText(true);
        
        contentBox.getChildren().addAll(title, description);
        
        // Informations du chapitre
        HBox infoBox = new HBox(20);
        infoBox.getStyleClass().add("chapter-info-box");
        infoBox.setAlignment(Pos.CENTER_LEFT);
        
        // Durée
        HBox durationBox = new HBox(8);
        durationBox.getStyleClass().add("chapter-info-item");
        
        FontAwesomeIconView clockIcon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
        clockIcon.getStyleClass().add("chapter-icon");
        
        Label duration = new Label(chapitre.getDureeEstimee() + " min");
        duration.getStyleClass().add("chapter-duration");
        
        durationBox.getChildren().addAll(clockIcon, duration);
        
        // Nombre de vues
        HBox viewsBox = new HBox(8);
        viewsBox.getStyleClass().add("chapter-info-item");
        
        FontAwesomeIconView eyeIcon = new FontAwesomeIconView(FontAwesomeIcon.EYE);
        eyeIcon.getStyleClass().add("chapter-icon");
        
        Label views = new Label(chapitre.getNbrVues() + " vues");
        views.getStyleClass().add("chapter-views");
        
        viewsBox.getChildren().addAll(eyeIcon, views);
        
        // Bouton Voir le contenu
        Button viewButton = new Button("Voir le contenu");
        viewButton.getStyleClass().add("view-chapter-button");
        
        FontAwesomeIconView playIcon = new FontAwesomeIconView(FontAwesomeIcon.PLAY_CIRCLE);
        playIcon.getStyleClass().addAll("chapter-icon");
        viewButton.setGraphic(playIcon);
        
        // Statut du chapitre (verrouillé/déverrouillé)
        FontAwesomeIconView lockIcon = new FontAwesomeIconView(FontAwesomeIcon.LOCK);
        lockIcon.getStyleClass().addAll("chapter-icon", "lock-icon");
        
        infoBox.getChildren().addAll(durationBox, viewsBox, lockIcon);
        
        // Layout final
        header.getChildren().addAll(chapterNumber, contentBox);
        
        HBox actionBox = new HBox();
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.getChildren().add(viewButton);
        
        card.getChildren().addAll(header, infoBox, actionBox);
        
        // Action du bouton
        viewButton.setOnAction(event -> viewChapter(chapitre));
        
        return card;
    }

    private void viewChapter(Chapitre chapitre) {
        System.out.println("Viewing chapter: " + chapitre.getTitle());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ChapitreDetailsEtudiant.fxml"));
            Parent root = loader.load();
            System.out.println(chapitre);
            ChapitreDetailsControllerEtudiant controller = loader.getController();
            controller.setChapitre(chapitre, course);
            mainBox.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Failed to load EditCours.fxml: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ListeCoursEtudiant.fxml"));
            mainBox.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Error loading ListeCoursEtudiant.fxml: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditAction() {
        try {
            if (course == null) {
                System.err.println("No course selected for editing");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EditCours.fxml"));
            Parent root = loader.load();
            EditCoursController controller = loader.getController();
            controller.setCourse(course);
            mainBox.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Failed to load EditCours.fxml: " + e.getMessage());
        }
    }

    @FXML
    private void handleStatsAction() {
        System.out.println("Viewing stats for course: " + (course != null ? course.getTitle() : "null"));
        // TODO: Implement stats navigation
    }

    @FXML
    private void handleDeleteAction() {
        if (course == null) {
            System.err.println("No course selected for deletion");
            return;
        }

        // Confirm deletion
        boolean confirmed = showConfirmationDialog("Supprimer le cours", "Êtes-vous sûr de vouloir supprimer ce cours ?");
        if (!confirmed) {
            return;
        }

        try {
            // Call the service to delete the course
            serviceCours.delete(course);

            // Show a success message
            showAlert("Succès", "Le cours a été supprimé avec succès.", Alert.AlertType.INFORMATION);

            // Navigate back to the list of courses
            handleBack();
        } catch (Exception e) {
            System.err.println("Failed to delete course: " + e.getMessage());
            showAlert("Erreur", "Une erreur est survenue lors de la suppression du cours.", Alert.AlertType.ERROR);
        }
    }

    // Utility method to show a confirmation dialog
    private boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType okButton = new ButtonType("Oui", ButtonBar.ButtonData.YES);
        ButtonType cancelButton = new ButtonType("Non", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(okButton, cancelButton);

        return alert.showAndWait().filter(response -> response == okButton).isPresent();
    }

    // Utility method to show an alert dialog
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    void handleListes(ActionEvent event) {
        System.out.println("handleListes clicked");
        loadScene("/ListeCoursEtudiant.fxml");
    }

    private void loadScene(String fxmlPath) {
        try {
            // Load the new FXML
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            // Get the current stage from a known node
            Stage stage = (Stage) mainBox.getScene().getWindow();
            // Create a new scene with the loaded root
            Scene scene = new Scene(root, 1000, 700); // Match FXML dimensions
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateEnrollButtonState() {
        if (course == null) return;
        
        try {
            if (serviceInscription.estInscrit(DEFAULT_USER_ID, course.getId())) {
                enrollButton.setText("Déjà inscrit");
                enrollButton.setDisable(true);
            } else {
                if (course.getPrix() > 0) {
                    enrollButton.setText("Payer et s'inscrire (" + course.getPrix() + " DT)");
                } else {
                    enrollButton.setText("S'inscrire gratuitement");
                }
                enrollButton.setDisable(false);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de l'inscription : " + e.getMessage());
            enrollButton.setDisable(true);
        }
    }

    @FXML
    private void handleEnrollAction() {
        if (course == null) {
            showAlert("Erreur", "Impossible de s'inscrire au cours.", Alert.AlertType.ERROR);
            return;
        }

        try {
            // Vérifier si l'étudiant est déjà inscrit
            if (serviceInscription.estInscrit(DEFAULT_USER_ID, course.getId())) {
                showAlert("Information", "Vous êtes déjà inscrit à ce cours.", Alert.AlertType.INFORMATION);
                return;
            }

            // Si le cours est payant
            if (course.getPrix() > 0) {
                if (course.getLienDePaiment() != null && !course.getLienDePaiment().isEmpty()) {
                    // Demander confirmation avant de rediriger vers le paiement
                    boolean confirmPaiement = showConfirmationDialog(
                        "Confirmation de paiement",
                        "Vous allez être redirigé vers la page de paiement.\nMontant à payer : " + course.getPrix() + " DT\n\nVoulez-vous continuer ?"
                    );

                    if (confirmPaiement) {
                        try {
                            // Ouvrir le lien de paiement dans le navigateur par défaut
                            Desktop.getDesktop().browse(new URI(course.getLienDePaiment()));
                            
                            // Créer une nouvelle fenêtre de confirmation
                            Stage confirmStage = new Stage();
                            confirmStage.setTitle("Confirmation de paiement");
                            
                            VBox confirmBox = new VBox(10);
                            confirmBox.setPadding(new Insets(20));
                            confirmBox.setAlignment(Pos.CENTER);
                            
                            Label messageLabel = new Label("Veuillez compléter votre paiement dans le navigateur.\nUne fois le paiement effectué, cliquez sur 'Confirmer'.");
                            messageLabel.setWrapText(true);
                            
                            Button confirmButton = new Button("J'ai effectué le paiement");
                            Button cancelButton = new Button("Annuler");
                            
                            HBox buttonBox = new HBox(10);
                            buttonBox.setAlignment(Pos.CENTER);
                            buttonBox.getChildren().addAll(confirmButton, cancelButton);
                            
                            confirmBox.getChildren().addAll(messageLabel, buttonBox);
                            
                            Scene confirmScene = new Scene(confirmBox);
                            confirmStage.setScene(confirmScene);
                            
                            // Gérer les actions des boutons
                            confirmButton.setOnAction(e -> {
                                confirmStage.close();
                                procederInscription();
                            });
                            
                            cancelButton.setOnAction(e -> {
                                confirmStage.close();
                                showAlert("Information", "L'inscription a été annulée. Vous pouvez réessayer plus tard.", Alert.AlertType.INFORMATION);
                            });
                            
                            // Afficher la fenêtre de confirmation
                            confirmStage.show();
                        } catch (Exception e) {
                            showAlert("Erreur", "Impossible d'ouvrir le lien de paiement : " + e.getMessage(), Alert.AlertType.ERROR);
                        }
                    }
                } else {
                    showAlert("Erreur", "Le lien de paiement n'est pas disponible.", Alert.AlertType.ERROR);
                }
            } else {
                // Si le cours est gratuit, procéder directement à l'inscription
                procederInscription();
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'inscription : " + e.getMessage());
            showAlert("Erreur", "Une erreur est survenue lors de l'inscription.", Alert.AlertType.ERROR);
        }
    }

    private void procederInscription() {
        try {
            // Inscrire l'étudiant
            serviceInscription.inscrireEtudiant(DEFAULT_USER_ID, course.getId());

            // Envoyer l'email de confirmation
            sendConfirmationEmail(user.getEmail(), course.getTitle());

            // Mettre à jour l'interface
            updateEnrollButtonState();

            // Rafraîchir l'interface pour afficher les chapitres
            initializeUI();

            // Afficher un message de succès
            showAlert("Succès", "Inscription réussie ! Un email de confirmation vous a été envoyé.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'inscription : " + e.getMessage());
            showAlert("Erreur", "Une erreur est survenue lors de l'inscription.", Alert.AlertType.ERROR);
        }
    }

    private void sendConfirmationEmail(String to, String courseTitle) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_USERNAME, GMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(GMAIL_USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Confirmation d'inscription au cours");
            
            String htmlContent = String.format(
                "<html><body>" +
                "<h2>Confirmation d'inscription</h2>" +
                "<p>Vous êtes maintenant inscrit au cours : <strong>%s</strong></p>" +
                "<p>Merci de nous avoir rejoint !</p>" +
                "</body></html>",
                courseTitle
            );
            
            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);

        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
        }
    }

    private void updateFavoriteButtonState() {
        try {
            if (serviceFavoris.estDansFavoris(DEFAULT_USER_ID, course.getId())) {
                favoriteButton.getStyleClass().add("active");
                favoriteIcon.setGlyphName("STAR");
            } else {
                favoriteButton.getStyleClass().remove("active");
                favoriteIcon.setGlyphName("STAR_O");
            }
        } catch (Exception e) {
            System.err.println("Error updating favorite button state: " + e.getMessage());
        }
    }

    @FXML
    private void handleFavoriteAction() {
        try {
            if (serviceFavoris.estDansFavoris(DEFAULT_USER_ID, course.getId())) {
                serviceFavoris.retirerDesFavoris(DEFAULT_USER_ID, course.getId());
            } else {
                serviceFavoris.ajouterAuxFavoris(DEFAULT_USER_ID, course.getId());
            }
            updateFavoriteButtonState();
        } catch (Exception e) {
            System.err.println("Error handling favorite action: " + e.getMessage());
            showAlert("Erreur", "Une erreur est survenue lors de la gestion des favoris.", Alert.AlertType.ERROR);
        }
    }

    // Modernize back button
    @FXML
    private void initialize() {
        backButton.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-cursor: hand;");
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #bbb; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-cursor: hand;"));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-cursor: hand;"));
    }

    public void handleListeEvaluationsAction(ActionEvent actionEvent) {
        try {
            if (course == null) {
                System.err.println("No course selected for listing evaluations");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/knowlity/view/student/student.fxml"));
            Parent root = loader.load();
            StudentController controller = loader.getController();
            controller.setCourse(course);
            mainBox.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Failed to load EditCours.fxml: " + e.getMessage());
        }
    }

    public void handleListeQuizAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ListQuizEtudiant.fxml"));
            Parent root = loader.load();
            
            ListQuizEtudiantController controller = loader.getController();
            controller.setCourse(course);
            
            Stage stage = new Stage();
            stage.setTitle("Liste des Quiz");
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur de chargement");
            alert.setContentText("Impossible de charger la liste des quiz : " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadImage(ImageView imageView, String imagePath) {
        String path = imagePath != null && !imagePath.isEmpty() ? imagePath.trim() : "/images/placeholder.png";
        if (path != null && !path.isEmpty()) {
            path = path.substring(path.lastIndexOf("\\") + 1); // Handles backslashes
            path = path.substring(path.lastIndexOf("/") + 1);  // Handles forward slashes
        }

        if (!path.startsWith("/images/") && !path.equals("/images/placeholder.png")) {
            path = "/images/" + path;
        }

        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                System.err.println("Image not found: " + path);
                stream = getClass().getResourceAsStream("/images/placeholder.png");
            }
            imageView.setImage(new Image(stream));
        } catch (Exception e) {
            System.err.println("Error loading image: " + path + ". Error: " + e.getMessage());
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
        }
    }

}
