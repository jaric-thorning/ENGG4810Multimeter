package main;

import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class GuiController implements Initializable {
	/* Elements required for resizing the GUI correctly */
	@FXML
	Group root;
	@FXML
	Pane appPane;
	@FXML
	AnchorPane rightAnchor = new AnchorPane();
	@FXML
	Label measurementsLabel = new Label();
	@FXML
	AnchorPane leftAnchor = new AnchorPane();

	/* Elements relating to the connected mode. */
	@FXML
	private RadioButton connRBtn;
	@FXML
	private Button pauseBtn;
	private boolean isPaused = false;
	@FXML
	private Button saveBtn;
	@FXML
	Button discardBtn;

	/* Elements relating to the disconnected mode. */
	@FXML
	private RadioButton disconnRBtn;

	/* Elements relating to mask-testing */
	@FXML
	private Button maskTestingBtn;
	private boolean maskTestingSelected = false;

	/* Higher Boundary */
	@FXML
	private Button setHighBtn;
	@FXML
	private CheckBox highBoundaryCheckbox;

	/* Lower Boundary */
	@FXML
	private Button setLowBtn;
	@FXML
	private CheckBox lowBoundaryCheckbox;

	/* Elements relating to the line chart. */
	@FXML
	LineChart<Number, Number> lineChart;
	private ArrayList<Number> coordinates = new ArrayList<>();
	int i = 0;

	// Store last place of node
	Number lastPlaceX = 0;
	Number lastPlaceY = 0;

	/* Holds the upper and lower boundaries */
	XYChart.Series<Number, Number> upperSeries = new XYChart.Series<>();
	XYChart.Series<Number, Number> lowerSeries = new XYChart.Series<>();

	/* Holds x and y values of mouse position. */
	@FXML
	private Label yCoordValues;
	@FXML
	private Label xCoordValues;
	@FXML
	private Label coordLabel;

	DecimalFormat oneDecimal = new DecimalFormat("0.0");

	public GuiController() {
		System.out.println("Initialised GuiController");
	}

	// TODO: Change around the test connection, either call it
	// everytime, or just once at the start.
	// Make sure you can't execute things, when it's disabled
	/**
	 * Selects the connected mode of the GUI if there is a connection, otherwise it's disabled.
	 */
	@FXML
	private void selectConnected() {
		// If there a connection and the radio button is selected
		if (connRBtn.isSelected() && testConnection(connRBtn)) {
			System.out.println("CONNECTED MODE INITIATED");
			System.out.println("//-------------------//");

			// Disable the disconnected mode from being editable during
			// connected mode
			disconnRBtn.setDisable(true);
		} else if (!testConnection(connRBtn)) {
			System.out.println("WHHHHAT");

			disconnRBtn.setDisable(false);
		} else { // TODO: Assuming 'else' just covers when radio button is not selected.

			if (notifyUserConnected()) {
				System.out.println("CONNECTED MODE EXITED");

				disconnRBtn.setDisable(false);
				// Clear all data related things.
				// Close open connections.
			} else {
				System.out.println("CONNECTED MODE STAYING");

				connRBtn.setSelected(true);
			}
		}
	}

	/**
	 * A private helper function to selectConnected. Displays a pop-up message asking the user if
	 * they wish to exit connected mode.
	 * 
	 * @return true if the user decides to exit connected mode, false otherwise.
	 */
	private boolean notifyUserConnected() {
		// Add a warning pop up
		String title = "Exit Connected Mode";
		String warning = "Are you sure you want to exit connected mode?";
		String errorType = "modena/dialog-warning.png";
		AlertType alertType = AlertType.CONFIRMATION;

		// Notify User of existing file.
		Optional<ButtonType> result = GuiView.getInstance()
				.alertUser(title, warning, errorType, alertType).showAndWait();

		if (result.get() == ButtonType.OK) {
			// User was OK exiting connected mode
			return true;
		} else {
			// User was not OK exiting connected mode (cancelled or closed dialog box)
			return false;
		}
	}

	/**
	 * A private helper function to selectConnected. This function is called to determine if there
	 * is a connection (optical link).
	 * 
	 * @param connRBtn
	 *            the radio button which controls the
	 * @return true if the connection was successful, false otherwise
	 */
	private boolean testConnection(RadioButton connRBtn) {
		/* TODO: setup connection test. */
		boolean connectionTestSuccess = true;

		// Enable the button.
		if (connectionTestSuccess) {
			System.out.println("CONNECTION ESTABLISHED");
			connRBtn.setDisable(false);
			pauseBtn.setDisable(false);
			saveBtn.setDisable(false);
			discardBtn.setDisable(false);
		} else {
			connRBtn.setDisable(true); // Leave the button disabled if no connection
			pauseBtn.setDisable(true);
			saveBtn.setDisable(true);
			discardBtn.setDisable(true);
		}
		return connectionTestSuccess;
	}

	/**
	 * Selects the disconnected mode of the GUI.
	 */
	@FXML
	private void selectDisconnected() {
		// If disconnected button is selected
		if (disconnRBtn.isSelected()) {
			System.out.println("DISCONNECTED MODE INITIATED");
			System.out.println("//-------------------//");

			// Disable the connected mode from being editable during disconnected mode
			connRBtn.setDisable(true);
			pauseBtn.setDisable(true);
			saveBtn.setDisable(true);
			discardBtn.setDisable(true);
		} else {
			if (notifyUserDisconnected()) {
				System.out.println("DISCONNECTED MODE EXITED");

				connRBtn.setDisable(false); // enable other radio button
				pauseBtn.setDisable(false);
				saveBtn.setDisable(false);
				discardBtn.setDisable(false);
			} else {
				System.out.println("DISCONNECTED MODE STAYING");

				disconnRBtn.setSelected(true);
			}
		}
	}

	/**
	 * A private helper function to selectDisconnected. Displays a pop-up message asking the user if
	 * they wish to exit disconnected mode.
	 * 
	 * @return true if the user decides to exit disconnected mode, false otherwise.
	 */
	private boolean notifyUserDisconnected() {
		// Add a warning pop up
		String title = "Exit Disconnected Mode";
		String warning = "Are you sure you want to exit disconnected mode?";
		String errorType = "modena/dialog-warning.png";
		AlertType alertType = AlertType.CONFIRMATION;
		// Notify User of existing file.
		Optional<ButtonType> result = GuiView.getInstance()
				.alertUser(title, warning, errorType, alertType).showAndWait();

		if (result.get() == ButtonType.OK) {
			// User was OK exiting disconnected mode
			return true;

			// Clear all data related things.
		} else {
			// User was not OK exiting disconnected mode (cancelled or closed dialog box)
			return false;
		}
	}

	/**
	 * Pauses the displayed acquired data.
	 */
	@FXML
	private void pauseDataAcquisition() {
		/*
		 * TODO: NUT OUT THE ACQUISITION THING. Keep the connection open, but pause the thread that
		 * displays the data on the line chart???
		 */

		if (!isPaused) {
			isPaused = true;
			pauseBtn.setText("Unpause");
			System.out.println("DATA IS PAUSED");
		} else {
			isPaused = false;
			pauseBtn.setText("Pause");
			System.out.println("DATA IS UNPAUSED");
		}

	}

	/**
	 * Saves the currently acquired data if data acquisition is paused. An error message will pop up
	 * if the data has not been paused, and the data will not be saved.
	 */
	@FXML
	private void saveDataAcquisition() {

		// Check that it's paused.
		if (!isPaused) {
			String title = "Error! Attempting To Save Data Incorrectly";
			String warning = "Data acquisition must be paused before saving the current data.";
			String errorType = "modena/dialog-error.png";
			AlertType alertType = AlertType.ERROR;

			// Notify User of incorrect saving procedure.
			GuiView.getInstance().alertUser(title, warning, errorType, alertType).showAndWait();
		} else {
			// TODO: check which file formats are required.
			String fileFormatExtension = "*.csv";
			String fileFormatTitle = "Comma Separated Files";

			// Save out data
			FileChooser saveFileOptions = new FileChooser();
			saveFileOptions.setTitle("Save Acquired Data");

			// Set file format
			saveFileOptions.getExtensionFilters()
					.add(new ExtensionFilter(fileFormatTitle, fileFormatExtension));

			File selectedFile = saveFileOptions.showSaveDialog(GuiView.getInstance().getStage());

			if (selectedFile != null) {
				// WRITE OUT DATA HERE
				System.out.println("Saving Data");
			}
		}
	}

	/**
	 * A private helper function to discardData. Displays a pop-up message notifying the user that
	 * data will be discarded if they continue.
	 * 
	 * @return true when data is to be retained, false otherwise.
	 */
	private boolean notifyDiscardingData() {
		// Add a warning pop up
		String title = "Discard Data";
		String warning = "Are you sure you want to discard all of the data? This action is irreversible.";
		String errorType = "modena/dialog-warning.png";
		AlertType alertType = AlertType.CONFIRMATION;

		// Notify User of discarding data
		Optional<ButtonType> result = GuiView.getInstance()
				.alertUser(title, warning, errorType, alertType).showAndWait();

		if (result.get() == ButtonType.OK) {
			// User was OK with clearing data
			return true;
		} else {
			// User was not OK clearing data
			return false;
		}
	}

	/**
	 * Discards all saved data and starts the plot again.
	 */
	@FXML
	private void discardData() {
		if (notifyDiscardingData()) {
			// TODO: discard and clear all data
			// TODO: restart the plot again.
			System.out.println("DATA DISCARDED");
		} else {
			System.out.println("DATA NOT DISCARDED");
		}
	}

	/**
	 * A private helper function to getMouseCoordsInChart. Gets the right coordinates.
	 * 
	 * @param event
	 *            the mouse event to attach this to.
	 * @return the stored values of both coords.
	 */
	private ArrayList<Number> getMouseToChartCoords(MouseEvent event) {
		ArrayList<Number> foundCoordinates = new ArrayList<>();

		foundCoordinates.add(getMouseChartCoords(event, true)); // add x
		foundCoordinates.add(getMouseChartCoords(event, false)); // add y

		// System.out.println("X3: " + oneDecimal.format(foundCoordinates.get(0)) + " " + "Y3: "
		// + oneDecimal.format(foundCoordinates.get(1)));
		return foundCoordinates;
	}

	private EventHandler<MouseEvent> deleteData(XYChart.Data<Number, Number> dataPoints,
			XYChart.Series<Number, Number> series) {
		EventHandler<MouseEvent> removeSelectedDataPoints = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				System.out.println("DELETE NODE FROM: " + series);
				series.getData().remove(dataPoints);
			}
		};
		return removeSelectedDataPoints;
	}

	/**
	 * 
	 * @param dataPoints
	 * @return
	 */
	private EventHandler<MouseEvent> moveData(XYChart.Data<Number, Number> dataPoints) {
		EventHandler<MouseEvent> moveDataPoints = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				dataPoints.setXValue(getMouseChartCoords(event, true));
				dataPoints.setYValue(getMouseChartCoords(event, false));

				// System.out.println(oneDecimal.format(dataPoints.getXValue()) + " || "
				// + i + " :: " + oneDecimal.format(dataPoints.getYValue()));
			}
		};
		return moveDataPoints;
	}

	/**
	 * 
	 * @param series
	 * @param coordinates
	 * @param seriesName
	 */
	private void setUpBoundaries(XYChart.Series<Number, Number> series,
			ArrayList<Number> coordinates, String seriesName) {

		// Add data to specified series.
		series.setName(seriesName);
		series.getData()
				.add(new XYChart.Data<Number, Number>(coordinates.get(0), coordinates.get(1)));

		// Modified the for loop for IDing the line chart data points from:
		// https://gist.github.com/TheItachiUchiha/c0ae68ef8e6273a7ac10
		System.out.println("SIZE: " + series.getData().size());

		// TODO: MAKE SURE i IS OK.
		for (i = 0; i < series.getData().size(); i++) {
			Data<Number, Number> dataPoints = series.getData().get(i);

			// Display x and y values.
			dataPoints.getNode().setOnMouseEntered(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					System.out.println(oneDecimal.format(dataPoints.getXValue()) + " || " + i
							+ " :: " + oneDecimal.format(dataPoints.getYValue()));
				}
			});

			// Deletes the point that was clicked
			// TODO: CHANGE THE MOUSE EVENT TO SOMETHING LESS COMMON.
			// FIXME: something wrong with deleting dataPoints.
			dataPoints.getNode().addEventFilter(MouseEvent.MOUSE_CLICKED,
					deleteData(dataPoints, series));

			// Moves the point that was hovered over
			dataPoints.getNode().addEventHandler(MouseEvent.MOUSE_MOVED, moveData(dataPoints));
		}
	}

	//TODO: NOT WORKING
//	private void removeLowBoundaries(XYChart.Series<Number, Number> series) {
//		for (XYChart.Data<Number, Number> data : series.getData()) {
//			data.getNode().removeEventHandler(MouseEvent.MOUSE_MOVED, moveData(data));
//			data.getNode().removeEventHandler(MouseEvent.MOUSE_CLICKED,
//					deleteData(data, lowerSeries));
//		}
//	}

	////////////
	// TODO: NEED A SELECTION OF WHETHER OR NOT IT'S UPPER/LOWER BOUNDARY
	/**
	 * Gets the values of the mouse within the line chart graph. Modified off:
	 * http://stackoverflow.com/questions/28562195/how-to-get-mouse-position-in-chart-space. To be
	 */
	private void setBoundaries(Node chartBackground) {

		chartBackground.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (maskTestingSelected) {

					// Remove for now the animation/autoscaling.
					lineChart.setAnimated(false);
					lineChart.getXAxis().setAutoRanging(false);
					lineChart.getYAxis().setAutoRanging(false);

					// Gets the coordinates
					coordinates = getMouseToChartCoords(event);

					if (highBoundaryCheckbox.isSelected()) {
						// removeLowBoundaries(lowerSeries);
						// Set up high boundary
						setUpBoundaries(upperSeries, coordinates, "Upper Boundary");
					} else {
						// Set up low boundary
						setUpBoundaries(lowerSeries, coordinates, "Lower Boundary");
					}

				}
			}
		});
	}

	/**
	 * Displays mask-testing options
	 */
	@FXML
	private void editMask() {
		// Selected
		if (!maskTestingSelected) {
			maskTestingSelected = true;
			xCoordValues.setDisable(false);
			yCoordValues.setDisable(false);
			coordLabel.setVisible(true);
			xCoordValues.setVisible(true);
			yCoordValues.setVisible(true);
			System.out.println("MASK TESTING SELECTED");
		} else {
			// Not Selected
			maskTestingSelected = false;
			xCoordValues.setText("X:");
			yCoordValues.setText("Y:");
			xCoordValues.setDisable(true);
			yCoordValues.setDisable(true);

			coordLabel.setVisible(false);
			xCoordValues.setVisible(false);
			yCoordValues.setVisible(false);
			System.out.println("MASK TESTING DE-SELECTED");
		}
	}

	/**
	 * 
	 */
	@FXML
	private void setToHighBoundary() {
		highBoundaryCheckbox.setSelected(true);
		lowBoundaryCheckbox.setSelected(false);
		System.out.println("HIGH");
	}

	@FXML
	private void setHighBoundary() {
		System.out.println("SETTING HIGH BOUNDARY");
		System.out.println(upperSeries.getData().size());

		// for (Data<Number, Number> dataPoints : upperSeries.getData()) {
		//
		//// System.out.println(oneDecimal.format(dataPoints.getXValue()) + "::"
		//// + oneDecimal.format(dataPoints.getYValue()));
		// }
	}

	// TODO: MAKE SURE ONLY ONE OR THE OTHER CAN BE SELECTED.
	// FIXME: CAN ONLY HAVE ONE OR THE OTHER IN TERMS OF MASK UPPER OR LOWER NOT BOTH
	@FXML
	private void setToLowBoundary() {
		highBoundaryCheckbox.setSelected(false);
		lowBoundaryCheckbox.setSelected(true);
		System.out.println("LOW");
	}

	@FXML
	private void setLowBoundary() {
		System.out.println("SETTING LOW BOUNDARY");
	}

	private Number getMouseChartCoords(MouseEvent event, boolean isX) {
		Number returnedCoord = 0;

		// return x coord
		if (isX) {
			double x = lineChart.getXAxis().sceneToLocal(event.getSceneX(), event.getSceneY())
					.getX();

			returnedCoord = lineChart.getXAxis().getValueForDisplay(x);

		} else {
			// return y coord
			double y = lineChart.getYAxis().sceneToLocal(event.getSceneX(), event.getSceneY())
					.getY();

			returnedCoord = lineChart.getYAxis().getValueForDisplay(y);
		}
		return returnedCoord;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO: DO A PING TEST TO SEE IF THERE'S A CONNECTION.
		testConnection(connRBtn);

		// Add to lineChart
		lineChart.getData().add(upperSeries);
		lineChart.getData().add(lowerSeries);

		// the lookup function found from https://gist.github.com/avitry/5598699
		Node chartBackground = lineChart.lookup(".chart-plot-background");
		chartBackground.setCursor(Cursor.CROSSHAIR);

		// set the upper and lower boundary coordinates
		setBoundaries(chartBackground);

		// Display mouse coordinates. FEATURE OF MASK-TESTING ONLY
		chartBackground.setOnMouseMoved(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				// Get coordinates
				xCoordValues.setText("X: " + oneDecimal.format(getMouseChartCoords(event, true)));
				yCoordValues.setText("Y: " + oneDecimal.format(getMouseChartCoords(event, false)));
			}
		});
	}

}
