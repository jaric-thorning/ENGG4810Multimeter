package main;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
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
	 * Hooks onto GUI shutdown event and does relevant tasks (i.e. close any open ports, shutdown
	 * threads).
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

		// Original dimensions of stage
		primaryStage.setMinWidth(1096D); // 1274D
		primaryStage.setMinHeight(600D);

		// Get access to the GUI controller
		GuiController controller = loader.getController();

		// Add width/height listeners.
		sceneWidthChange(stage, scene, controller);
		sceneHeightChange(stage, scene, controller);

		testingMask(controller);
		updateMaskDimensions(controller);
		
		// Set window title
		primaryStage.setTitle(GuiTitle);

		// Display the GUI
		primaryStage.show();
	}

	private void testingMask(GuiController controller) {
		System.out.println("Y");
		System.out.println(controller.upperSeries.dataProperty().get().toString());

		controller.upperSeries.getData()
				.addListener(new ListChangeListener<Data<Number, Number>>() {

					@Override
					public void onChanged(
							javafx.collections.ListChangeListener.Change<? extends Data<Number, Number>> c) {
						System.out.println("TEST");

					}

				});

		controller.upperSeries.dataProperty()
				.addListener(new ChangeListener<ObservableList<XYChart.Data<Number, Number>>>() {

					@Override
					public void changed(
							ObservableValue<? extends ObservableList<Data<Number, Number>>> observable,
							ObservableList<Data<Number, Number>> oldValue,
							ObservableList<Data<Number, Number>> newValue) {

						System.out.println("X");
						for (XYChart.Data<Number, Number> d : newValue) {
							System.out.println(
									"NEW X: " + d.getXValue() + " NEW Y: " + d.getYValue());
						}

					}

				});
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
	private void sceneWidthChange(Stage stage, Scene scene, GuiController controller) {
		// Set up scene width listener
		scene.widthProperty().addListener(new ChangeListener<Number>() {

			// 23D is the padding...
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldSceneWidth,
					Number newSceneWidth) {
				// double chartWidth = ((double) newSceneWidth) - 450D;

				/* THIS IS TO RESIZE THE MASK */
				// && stage.isFullScreen()
				/* FIXME */
				//updateMaskDimensions(controller);

				if ((controller.upperSeries.getData().size() > 0)) {

					for (int i = 0; i < controller.upperSeries.getData().size(); i++) {
						XYChart.Data<Number, Number> dataPoints = controller.upperSeries.getData()
								.get(i);
						Double x = dataPoints.getXValue().doubleValue();
						Double xx = controller.lineChart.getXAxis().getDisplayPosition(x);

						controller.upperBoundary.getPoints().set(2, xx);

						System.out.println("OS: " + x.toString() + " || XX:" + xx);
					}
				}

				//FIXME: resizing doesn't work if too fast
				controller.appPane.setMinWidth((double) newSceneWidth);
				//controller.lineChart.setMinWidth((double) newSceneWidth - 451D - 100D);
				//controller.rightAnchor.setMinWidth((double) newSceneWidth - 451D);
				
				// FIXME: THE CHART BACKGROUND DOESN'T
				Double testWidthOld = oldSceneWidth.doubleValue();
				Double testWidthNew = newSceneWidth.doubleValue();

				testHeight(controller, testWidthOld, testWidthNew);
				// System.out.println("CB: " +
				// controller.lineChart.lookup(".chart-plot-background").getLayoutX());

				// controller.GraphingResultsLabel.setMinWidth(chartWidth);
			}

		});
	}

	private void testHeight(GuiController controller, Double testWidthOld, Double testWidthNew) {
		System.out.println("ACK: " + testWidthNew);
		System.out.println("ACK OLD: " + testWidthOld);
		System.out.println(
				"ACK LINECHART: " + controller.chartBackground.getLayoutBounds().getWidth());
	}

	// FIXME: ERRORS IF I MODIFY HERE THE POLYGON POINTS
	private void updateMaskDimensions(GuiController controller) {
		// SET THE UPPER AND LOWER
		// lower
		controller.lowerBoundary.getPoints().set(2,
				controller.chartBackground.getLayoutBounds().getWidth());
		controller.lowerBoundary.getPoints().set(4,
				controller.chartBackground.getLayoutBounds().getWidth());

		// Upper
		controller.upperBoundary.getPoints().set(2,
				controller.chartBackground.getLayoutBounds().getWidth());
		controller.upperBoundary.getPoints().set(4,
				controller.chartBackground.getLayoutBounds().getWidth());

		// System.out.println("CW: " + controller.lineChart.getWidth() + " CH: " +
		// controller.lineChart.getHeight());
		// System.out.println("CH: " + controller.lineChart.getWidth() + " :: "
		// + (controller.chartBackground.getLayoutBounds().getWidth()));
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
	private void sceneHeightChange(Stage stage, Scene scene, GuiController controller) {
		// Set up scene height listener
		scene.heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldSceneHeight,
					Number newSceneHeight) {
				double chartHeight = ((double) newSceneHeight);

				controller.appPane.setMinHeight((double) newSceneHeight);
				
				// System.out.println("MM: " + stage.isFullScreen());
				// controller.lineChart.setMinHeight(chartHeight);
				// controller.leftAnchor.setMinHeight(chartHeight);
				// controller.rightAnchor.setMinHeight(chartHeight);

				if ((controller.upperSeries.getData().size() > 0)) {
					// myGrid.cellWidthProperty().bind(columnWidthSlider.valueProperty());

					for (int i = 0; i < controller.upperSeries.getData().size(); i++) {
						XYChart.Data<Number, Number> dataPoints = controller.upperSeries.getData()
								.get(i);
						Double y = dataPoints.getYValue().doubleValue();
						Double yy = controller.lineChart.getYAxis().getDisplayPosition(y);

						controller.upperBoundary.getPoints().set(3, yy);
					}
				}
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
