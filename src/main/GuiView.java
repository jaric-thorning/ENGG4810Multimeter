package main;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
	
	/**
	 * Hooks onto GUI shutdown event and does relevant tasks (i.e. close any open ports, shutdown threads).
	 */
	@Override
	public void stop() throws Exception {
		RecordedResults.shutdownRecordedResultsThread();
		// Close ports
		super.stop();
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
		primaryStage.setMinWidth(1274D);
		primaryStage.setMinHeight(600D);

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

			// 23D is the padding...
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldSceneWidth,
					Number newSceneWidth) {
				double chartWidth = ((double) newSceneWidth) - 450D;

				controller.appPane.setMinWidth((double) newSceneWidth);
				controller.lineChart.setMinWidth(chartWidth);
				controller.GraphingResultsLabel.setMinWidth(chartWidth);

				/* THIS IS TO RESIZE THE MASK */
				/* FIXME */
				updateMaskDimensions(controller);
			}

		});
	}

	// FIXME: ERRORS IF I MODIFY HERE THE POLYGON POINTS
	private void updateMaskDimensions(GuiController controller) {
		//lower
		controller.lowerBoundary.getPoints().set(2,
				controller.chartBackground.getLayoutBounds().getWidth());
		controller.lowerBoundary.getPoints().set(4,
				controller.chartBackground.getLayoutBounds().getWidth());
		
		//Upper
		controller.upperBoundary.getPoints().set(2,
				controller.chartBackground.getLayoutBounds().getWidth());
		controller.upperBoundary.getPoints().set(4,
				controller.chartBackground.getLayoutBounds().getWidth());

//		System.out.println("CH: " + controller.lineChart.getWidth() + " :: "
//				+ (controller.chartBackground.getLayoutBounds().getWidth()));
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
				double chartHeight = ((double) newSceneHeight);
				
				controller.appPane.setMinHeight((double) newSceneHeight);
//				controller.leftAnchor.setMinHeight(chartHeight);
//				controller.rightAnchor.setMinHeight(chartHeight);
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
