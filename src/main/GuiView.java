package main;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class GuiView extends Application {
	private String fxmlFileName = "/gui_test.fxml";
	private String GuiTitle = "Digital Multimeter Mark 1.0";
	private Stage stage = new Stage();
	private static GuiView instance;

	public GuiView() {
		instance = this;
		System.out.println("Initialised GuiView");
	}

	/**
	 * Enables other classes to access methods within this class, once the program has been
	 * launched.
	 * 
	 * @return an instance of the GuiView class.
	 */
	public static GuiView getInstance() {
		return instance;
	}

	public Stage getStage() {
		return this.stage;
	}

	// PUT IN LISTENERS FOR RE-SIZING THE ELEMENTS ON THE SCREEN
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;

		// Load the FXML file
		FXMLLoader loader = new FXMLLoader(this.getClass().getResource(fxmlFileName));

		// Set up the scene and add it to the stage
		Scene scene = new Scene(loader.load()); // can customise height
		primaryStage.setScene(scene);
		primaryStage.setResizable(true); // enable maximisation of screen
		primaryStage.setMinWidth(771D);
		primaryStage.setMinHeight(523D);

		// Get access to the GUI controller
		GuiController controller = loader.getController();

		// Add width/height listeners.
		sceneWidthChange(scene, controller);
		sceneHeightChange(scene, controller);

		// Set window title
		primaryStage.setTitle(GuiTitle);

		// Display the GUI
		primaryStage.show();
	}


	/**
	 * A private helper function which adds a listener to the width property of the scene, and to
	 * some of the contained elements.
	 * 
	 * @param scene
	 *            the scene which has the width listener attached to it.
	 * @param controller
	 *            the GuiController to access the elements of the scene.
	 */
	private void sceneWidthChange(Scene scene, GuiController controller) {
		// Set up scene width listener
		scene.widthProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldSceneWidth,
					Number newSceneWidth) {
				double chartWidth = ((double) newSceneWidth) - 335D;

				controller.appPane.setMinWidth((double) newSceneWidth);
				controller.lineChart.setMinWidth(chartWidth); // TODO: SEE IMPACT LATER OF USING RA
																// INSTEAD OF LC
				// controller.rightAnchor.setMinWidth(chartWidth);
				controller.measurementsLabel.setMinWidth(chartWidth);
				// controller.leftAnchor.setMinWidth((double) newSceneWidth);
			}

		});
	}

	/**
	 * A private helper function which adds a listener to the height property of the scene, and to
	 * some of the contained elements.
	 * 
	 * @param scene
	 *            the scene which has the height listener attached to it.
	 * @param controller
	 *            the GuiController to access the elements of the scene.
	 */
	private void sceneHeightChange(Scene scene, GuiController controller) {
		// Set up scene height listener
		scene.heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldSceneHeight,
					Number newSceneHeight) {
				controller.appPane.setMinHeight((double) newSceneHeight);
				controller.leftAnchor.setMinHeight((double) newSceneHeight - 10D);
				controller.rightAnchor.setMinHeight((double) newSceneHeight - 10D);
			}

		});
	}

	/**
	 * /** A helper function, to display a pop-up dialog box to the user, when they are about to
	 * exit connected or disconnected mode, or save a file. Dialog box structure modified from
	 * http://code.makery.ch/blog/javafx-dialogs-official/.
	 * 
	 * @param title
	 *            the name of the alert box
	 * @param context
	 *            the message displayed in the alert box
	 * @param errorType
	 *            if the message should be a warning or error type
	 * @param alertType
	 *            the actual type of the alert box, (i.e. only displaying ok for an error alert)
	 * @return the status of the User's selection
	 */
	public Alert alertUser(String title, String context, String errorType, AlertType alertType) {
		ImageView image = new ImageView(new Image(
				getClass().getResourceAsStream("/com/sun/javafx/scene/control/skin/" + errorType)));
		Node graphic = image;
		Alert alert = new Alert(alertType);

		alert.setGraphic(graphic); // Changes the graphic from a confirmation one to a warning one
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(context);
		// Keeps alert box on the screen when stage is maximised
		alert.initOwner(GuiView.getInstance().getStage());

		// Linking CSS style sheet to dialog pane.
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getStylesheets()
				.add(getClass().getResource("/dialogstyle.css").toExternalForm());

		return alert;
	}
}
