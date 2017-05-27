package main;

import java.text.DecimalFormat;
import java.util.Optional;

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
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * The GuiView class represents the View of the Model-View-Controller pattern.
 * 
 * @author dayakern
 *
 */
public class GuiView extends Application {
	DataEvents event = new DataEvents();

	private static final DecimalFormat MEASUREMENT_DECIMAL = new DecimalFormat("0.000");
	private static final DecimalFormat TIME_DECIMAL = new DecimalFormat("0.0");
	private static final Double STAGE_WIDTH = 1097D;
	private static final Double STAGE_HEIGHT = 625D;

	private String fxmlFileName = "/redesigned_gui.fxml";
	private String GuiTitle = "Digital Multimeter Mark 9001";
	private Stage stage = new Stage();

	private static GuiView instance;
	private GuiController controller;

	public GuiView() {
		instance = this;
		System.out.println("Initialised GuiView");
	}

	/**
	 * Enables other classes to access methods within this class, once the program has been launched.
	 * 
	 * @return an instance of the GuiView class
	 */
	public static GuiView getInstance() {
		return instance;
	}

	/**
	 * Gets the Stage object for other classes to modify.
	 * 
	 * @return the Stage object
	 */
	public Stage getStage() {
		return this.stage;
	}

	@Override
	public void stop() throws Exception {

		// Close any open ports
		controller.quit();

		// Shuts down any threads
		super.stop();
	}

	/**
	 * Loads the GUI.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;

		// Load the FXML file
		FXMLLoader loader = new FXMLLoader(this.getClass().getResource(fxmlFileName));

		// Set up the scene and add it to the stage
		Scene scene = new Scene(loader.load());
		primaryStage.setScene(scene);
		primaryStage.setResizable(true); // Enable maximisation of screen

		// Set stage to original dimensions of stage
		primaryStage.setMinWidth(STAGE_WIDTH);
		primaryStage.setMinHeight(STAGE_HEIGHT);

		// Get access to the GUI controller
		controller = loader.getController();

		// Add width/height listeners
		sceneWidthChange(scene, controller);
		sceneHeightChange(scene, controller);

		// Set up line chart styling & behaviour
		setupLineChart(controller.xAxis, controller.yAxis, controller);
		scene.getStylesheets().add(getClass().getResource("/chartstyle.css").toExternalForm());

		// Display coordinates on the screen
		displayPlotCoordinates(controller);

		// Default to disconnected mode tab & set up tab changes
		controller.modeOptions.getSelectionModel().select(1);
		setupTabChange(controller);

		// Set window title
		primaryStage.setTitle(GuiTitle);

		// Display the GUI
		primaryStage.show();
	}

	/**
	 * Determines what happens to the current tab's components when the other tab is selected.
	 * 
	 * @param controller
	 *            the GUI Controller with components to access/modify
	 */
	private void setupTabChange(GuiController controller) {

		controller.modeOptions.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {

			@Override
			public void changed(ObservableValue<? extends Tab> observable, Tab otherTab, Tab newlySelectedTab) {
				if (newlySelectedTab.equals(controller.connectedTab)) {
					System.out.println("LEAVING DISCON"); // TODO:REMOVE

					controller.yAxis.setAutoRanging(true);

					// Reset disconnected components
					controller.revertMaskTestingComponents();
				}

				if (newlySelectedTab.equals(controller.disconnectedTab)) {
					System.out.println("LEAVING CONN"); // TODO:REMOVE

					controller.yAxis.setAutoRanging(false);

					// Reset connected components
					controller.revertConnectedComponents();
				}
			}
		});
	}

	/**
	 * Displays the mouse coordinates relative to the chart background.
	 * 
	 * @param controller
	 *            the GUI Controller with components to access/modify
	 */
	private void displayPlotCoordinates(GuiController controller) {
		controller.chartBackground.setOnMouseMoved(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				controller.xCoordValues
						.setText("X: " + TIME_DECIMAL.format(controller.getMouseChartCoords(event, true)));
				controller.yCoordValues
						.setText("Y: " + MEASUREMENT_DECIMAL.format(controller.getMouseChartCoords(event, false)));
			}
		});
	}

	/**
	 * Setup the line chart to display and behave accordingly.
	 * 
	 * @param xAxis
	 *            the x-axis to be modified
	 * @param yAxis
	 *            the y-axis to be modified
	 * @param controller
	 *            the GUI Controller with components to access/modify
	 */
	private void setupLineChart(NumberAxis xAxis, NumberAxis yAxis, GuiController controller) {

		// Setup axes
		setupAxes(xAxis, yAxis);

		// Add line chart to the GUI application
		controller.lineChart = new ModifiedLineChart(xAxis, yAxis);

		controller.chartGrid.add(controller.lineChart, 0, 1);
		GridPane.setValignment(controller.lineChart, VPos.TOP);

		// Add series to chart
		controller.lineChart.getData().add(controller.getLowSeries());
		controller.getLowSeries().setName("low boundary");
		controller.lineChart.getData().add(controller.getHighSeries());
		controller.getHighSeries().setName("high boundary");
		controller.lineChart.getData().add(controller.getDataSeries());
		controller.getDataSeries().setName("multimeter data");

		// Setup chart plot background
		controller.chartBackground = controller.lineChart.lookup(".chart-plot-background");
		controller.chartBackground.setCursor(Cursor.CROSSHAIR);

		controller.createMaskAreas(controller.chartBackground);
	}

	/**
	 * A private function to 'setupLineChart' which sets up the necessary modifiers on the x and y axes of the line
	 * chart.
	 * 
	 * @param xAxis
	 *            the x-axis to be modified
	 * @param yAxis
	 *            the y-axis to be modified
	 */
	private void setupAxes(NumberAxis xAxis, NumberAxis yAxis) {
		xAxis.setLabel("Time (seconds)");
		xAxis.setLowerBound(0D);
		xAxis.setUpperBound(20D);
		xAxis.setForceZeroInRange(false);
		xAxis.setAutoRanging(false);
		xAxis.setAnimated(false);
		xAxis.setMinorTickCount(2);
		xAxis.setTickUnit(1D);
		xAxis.setTickLabelFill(Color.WHITE);

		yAxis.setLabel("Measurements");
		yAxis.setUpperBound(50D);
		yAxis.setLowerBound(-20D);
		yAxis.setForceZeroInRange(false);
		yAxis.setAutoRanging(true);
		yAxis.setAnimated(false);
		yAxis.setMinorTickCount(5);
		yAxis.setTickUnit(5D);
		yAxis.setTickLabelFill(Color.WHITE);
	}

	/**
	 * Adds a listener to the width property of the scene, and to some of the contained elements.
	 * 
	 * @param scene
	 *            the scene which has the width listener attached to it
	 * @param controller
	 *            the GUI Controller with components to access/modify
	 */
	private void sceneWidthChange(Scene scene, GuiController controller) {
		scene.widthProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldSceneWidth,
					Number newSceneWidth) {
				double rightAnchorWidth = ((double) newSceneWidth) - 335D;

				controller.appPane.setMinWidth((double) newSceneWidth);
				controller.rightAnchor.setMinWidth(rightAnchorWidth);
				controller.graphLabelAnchor.setMinWidth(rightAnchorWidth);
			}
		});
	}

	/**
	 * Adds a listener to the height property of the scene, and to some of the contained elements.
	 * 
	 * @param scene
	 *            the scene which has the height listener attached to it
	 * @param controller
	 *            the GUI Controller with components to access/modify
	 */
	private void sceneHeightChange(Scene scene, GuiController controller) {
		scene.heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldSceneHeight,
					Number newSceneHeight) {

				controller.appPane.setMinHeight((double) newSceneHeight);
				controller.rightAnchor.setMinHeight(((double) newSceneHeight) - 45);
				controller.modeOptions.setMinHeight(((double) newSceneHeight) - 10);
			}
		});
	}

	/**
	 * Displays customised pop-up dialog box to the user (i.e. when they are about to exit connected or disconnected
	 * mode or save a file). Dialog box structure modified from TP1 code, which was modified from:
	 * http://code.makery.ch/blog/javafx-dialogs-official/.
	 * 
	 * @param title
	 *            the name of the alert box
	 * @param context
	 *            the message displayed in the alert box
	 * @param errorType
	 *            if the message should be a warning or error type
	 * @param alertType
	 *            the actual type of the alert box, (i.e. only displaying OK for an error alert)
	 * @return the status of the User's selection
	 */
	public Alert alertUser(String title, String context, String errorType, AlertType alertType) {
		ImageView image = new ImageView(
				new Image(getClass().getResourceAsStream("/com/sun/javafx/scene/control/skin/" + errorType)));
		Node graphic = image;
		Alert alert = new Alert(alertType);

		// Changes the graphic displayed to match the error type
		alert.setGraphic(graphic);

		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(context);

		// Keeps alert box on the screen when stage is maximised
		alert.initOwner(GuiView.getInstance().getStage());

		// Link CSS style sheet
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getStylesheets().add(getClass().getResource("/dialogstyle.css").toExternalForm());

		return alert;
	}

	/**
	 * Launches a warning message box to the user. This is to inform them that they cannot add the latest point to the
	 * high/low mask boundaries.
	 */
	public void illegalMaskPoint() {

		String title = "Illegal Region Selection";
		String warning = "The lower and upper bounds cannot overlap!";

		// Add a warning pop up
		String errorType = "modena/dialog-warning.png";
		AlertType alertType = AlertType.WARNING;

		// Notify User
		Optional<ButtonType> result = this.alertUser(title, warning, errorType, alertType).showAndWait();

		// Do nothing if user selected 'OK'
		if (result.get() == ButtonType.OK) {
			return;
		}
	}
}
