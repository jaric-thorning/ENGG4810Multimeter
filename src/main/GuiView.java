package main;

import java.text.DecimalFormat;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
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

		// Set up linechart stlying + behaviour
		setupLineChart(controller.xAxis1, controller.yAxis1, controller);
		scene.getStylesheets().add(getClass().getResource("/chartstyle.css").toExternalForm());

		// Display coordinates on the screen
		displayPlotCoordinates(controller);
		// Set window title
		primaryStage.setTitle(GuiTitle);

		// Display the GUI
		primaryStage.show();
	}

	void displayPlotCoordinates(GuiController controller) {
		DecimalFormat oneDecimal = new DecimalFormat("0.000");

		// Display mouse coordinates. FEATURE OF MASK-TESTING ONLY
		controller.chartBackground.setOnMouseMoved(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				// Get coordinates
				controller.xCoordValues.setText(
						"X: " + oneDecimal.format(controller.getMouseChartCoords(event, true)));
				controller.yCoordValues.setText(
						"Y: " + oneDecimal.format(controller.getMouseChartCoords(event, false)));
			}
		});
	}

	private void setupLineChart(NumberAxis xAxis, NumberAxis yAxis, GuiController controller) {
		// set up axes
		setupAxes(xAxis, yAxis);

		// creating the chart
		controller.lineChart1 = new ModifiedLineChart(xAxis, yAxis);

		// add line chart to grid pane
		controller.chartGrid.add(controller.lineChart1, 0, 0);
		GridPane.setValignment(controller.lineChart1, VPos.BOTTOM);

		controller.lineChart1.getData().add(controller.lowerSeries);
		controller.lineChart1.getData().add(controller.upperSeries);

		controller.chartBackground = controller.lineChart1.lookup(".chart-plot-background");
		controller.chartBackground.setCursor(Cursor.CROSSHAIR);

		controller.setBoundaries(controller.chartBackground);
	}

	/**
	 * A private function which sets up the necessary modifies on the x and y axes of the line chart
	 * 
	 * @param xAxis
	 * @param yAxis
	 */
	private void setupAxes(NumberAxis xAxis, NumberAxis yAxis) {
		xAxis.setLabel("Time (seconds)");
		xAxis.setLowerBound(0D);
		xAxis.setUpperBound(10D);
		xAxis.setForceZeroInRange(false);
		xAxis.setAutoRanging(false);
		xAxis.setAnimated(false);
		xAxis.setMinorTickCount(2);
		xAxis.setTickUnit(1D);
		xAxis.setTickLabelFill(Color.WHITE);

		yAxis.setLabel("Measurements");
		yAxis.setUpperBound(50D);
		yAxis.setLowerBound(-10D);
		yAxis.setForceZeroInRange(false);
		yAxis.setAutoRanging(true);
		yAxis.setAnimated(false);
		yAxis.setMinorTickCount(5);
		yAxis.setTickUnit(5D);
		yAxis.setTickLabelFill(Color.WHITE);
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

				// FIXME: resizing doesn't work if too fast, it's a bit iffy
				controller.appPane.setMinWidth((double) newSceneWidth);
				// controller.lineChart.setMinWidth((double) newSceneWidth - 451D - 100D);
				// controller.rightAnchor.setMinWidth((double) newSceneWidth - 451D);

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
