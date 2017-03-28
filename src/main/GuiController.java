package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class GuiController implements Initializable {
	/* Elements required for resizing the GUI correctly */
	@FXML
	Group root;
	@FXML
	Pane appPane;
	@FXML
	Label GraphingResultsLabel;
	// @FXML
	// AnchorPane rightAnchor;
	// @FXML
	// AnchorPane leftAnchor;

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
	@FXML
	private Button loadSavedData;
	@FXML
	private Label loadFileLabel;

	/* Elements relating to mask-testing */
	@FXML
	private Button maskTestingBtn;
	private boolean maskTestingSelected = false;
	@FXML
	private Line separatorLine;
	@FXML
	private Button importMaskBtn;
	@FXML
	private Button exportMaskBtn;

	/* Holds x and y coordinates of every data point (boundary/loaded data) */
	@FXML
	private Label plotCoordLabel;
	@FXML
	private Label xDataCoord;
	@FXML
	private Label yDataCoord;

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
	@FXML
	private Label setBoundaryLabel;
	@FXML
	private Label addBoundaryLabel;
	
	@FXML
	NumberAxis yAxis;
	@FXML
	NumberAxis xAxis;
	@FXML
	Slider xAxisSlider;

	/* Elements relating to the line chart. */
	@FXML
	LineChart<Number, Number> lineChart;

	Node chartBackground;

	private ArrayList<Number> coordinates = new ArrayList<>();

	@FXML
	Polygon upperBoundary;

	@FXML
	Polygon lowerBoundary;

	// Store last place of node
	Number lastPlaceX = 0;
	Number lastPlaceY = 0;

	/* Holds the upper and lower boundaries */
	XYChart.Series<Number, Number> upperSeries = new XYChart.Series<>();
	XYChart.Series<Number, Number> lowerSeries = new XYChart.Series<>();

	/* Holds the line chart */
	XYChart.Series<Number, Number> readingSeries = new XYChart.Series<>();
	private String yUnit = "";

	/* Holds x and y coordinates of mouse position. */
	@FXML
	private Label yCoordValues;
	@FXML
	private Label xCoordValues;
	@FXML
	private Label coordLabel;

	DecimalFormat oneDecimal = new DecimalFormat("0.0");

	/* Digital Multimeter components */
	@FXML
	private Button voltageBtn;
	@FXML
	private Button currentBtn;
	@FXML
	private Button resistanceBtn;
	@FXML
	private TextField multimeterDisplay;
	@FXML
	private Label modeLabel;
	@FXML
	private Button logicBtn;
	@FXML
	private Button continuityBtn;

	private static String FILE_FORMAT_EXTENSION = "*.csv";
	private static String FILE_FORMAT_TITLE = "Comma Separated Files";

	public GuiController() {
		System.out.println("Initialised GuiController");
	}

	@FXML
	private void measureVoltage() {
		System.out.println("I clicked on voltage");
	}

	@FXML
	private void measureCurrent() {
		System.out.println("I clicked on current");
	}

	@FXML
	private void measureResistance() {
		System.out.println("I clicked on resistance");
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

			// Enable connected components
			pauseBtn.setDisable(false);
			saveBtn.setDisable(false);
			discardBtn.setDisable(false);

			// Enable digital multimeter components
			voltageBtn.setDisable(false);
			currentBtn.setDisable(false);
			resistanceBtn.setDisable(false);
			multimeterDisplay.setDisable(false);
			modeLabel.setDisable(false);
			logicBtn.setDisable(false);
			continuityBtn.setDisable(false);

		} else if (!testConnection(connRBtn)) {
			System.out.println("WHHHHAT");

			disconnRBtn.setDisable(false);
		} else { // TODO: Assuming 'else' just covers when radio button is not selected.

			if (notifyUserConnected()) {
				System.out.println("CONNECTED MODE EXITED");

				disconnRBtn.setDisable(false);

				// Disable digital multimeter components
				voltageBtn.setDisable(true);
				currentBtn.setDisable(true);
				resistanceBtn.setDisable(true);
				multimeterDisplay.setDisable(true);
				modeLabel.setDisable(true);
				logicBtn.setDisable(true);
				continuityBtn.setDisable(true);

				// Enable connected components
				pauseBtn.setDisable(true);
				saveBtn.setDisable(true);
				discardBtn.setDisable(true);

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
			// pauseBtn.setDisable(false);
			// saveBtn.setDisable(false);
			// discardBtn.setDisable(false);
		} else {
			connRBtn.setDisable(true); // Leave the button disabled if no connection
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

			// Show other things
			loadSavedData.setDisable(false);
			loadFileLabel.setDisable(false);
			maskTestingBtn.setDisable(false);

		} else {
			if (notifyUserDisconnected()) {
				System.out.println("DISCONNECTED MODE EXITED");

				connRBtn.setDisable(false); // enable other radio button

				loadSavedData.setDisable(true);
				loadFileLabel.setDisable(true);
				maskTestingBtn.setDisable(true);
				
				// hide mask testing components
				maskTestingSelected = false;
				separatorLine.setVisible(false);
				plotCoordLabel.setVisible(false);
				xDataCoord.setVisible(false);
				yDataCoord.setVisible(false);
				importMaskBtn.setVisible(false);
				exportMaskBtn.setVisible(false);
				
				addBoundaryLabel.setVisible(false);
				highBoundaryCheckbox.setVisible(false);
				lowBoundaryCheckbox.setVisible(false);
				
				setHighBtn.setVisible(false);
				setLowBtn.setVisible(false);
				setBoundaryLabel.setVisible(false);
				
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

	@FXML
	private void loadFile() {
		// Clear data from list
		readingSeries.getData().clear();
		yAxis.setLabel("Measurements");

		// Set up new array
		ArrayList<Double> mockXValues = new ArrayList<>();
		ArrayList<Double> mockYValues = new ArrayList<>();

		// Load in data
		FileChooser loadFileOptions = new FileChooser();
		loadFileOptions.setTitle("Load Saved Data");

		// Select file
		loadFileOptions.getExtensionFilters()
				.add(new ExtensionFilter(FILE_FORMAT_TITLE, FILE_FORMAT_EXTENSION));

		File selectedFile = loadFileOptions.showOpenDialog(GuiView.getInstance().getStage());

		// Only if file exists extract information from it
		if (selectedFile != null) {
			System.out.println("NAME: " + selectedFile.getPath());
			// Read it in
			for (String s : GuiModel.getInstance().readColumnData(selectedFile.getPath(), 0)) {
				mockXValues.add(Double.parseDouble(s));
			}

			for (String s : GuiModel.getInstance().readColumnData(selectedFile.getPath(), 1)) {
				mockYValues.add(Double.parseDouble(s));
			}

			// TODO: MAKE MORE EFFICIENT.
			for (String s : GuiModel.getInstance().readColumnData(selectedFile.getPath(), 2)) {
				yUnit = s;
				yAxis.setLabel("Measurements [" + yUnit + "]");
				break;
			}

			// Add data to series
			for (int i = 0; i < mockXValues.size(); i++) {
				readingSeries.getData().add(
						new XYChart.Data<Number, Number>(mockXValues.get(i), mockYValues.get(i)));

				// Assign indexing to each node
				Data<Number, Number> dataPoint = readingSeries.getData().get(i);
				dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
						getDataXYValues(dataPoint, i));
				
				// Update chart bounds
				int dataBoundsRange = (int) Math.ceil(i / 2); //2 = SAMPLES/SECOND
				if (dataBoundsRange > 50) {
					xAxis.setLowerBound(dataBoundsRange - 50);
					xAxis.setUpperBound(dataBoundsRange);
				}
			}

			System.out.println("TEST X: " + mockXValues.size() + " " + mockXValues.toString());
			System.out.println("TEST Y: " + mockYValues.size() + " " + mockYValues.toString());
		}
	}

	/**
	 * Saves the currently acquired data if data acquisition is paused. An error message will pop up
	 * if the data has not been paused, and the data will not be saved.
	 * 
	 * @precondition readingSeries has data in it.
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
			// Save out data
			FileChooser saveFileOptions = new FileChooser();
			saveFileOptions.setTitle("Save Acquired Data");

			// Set file format
			saveFileOptions.getExtensionFilters()
					.add(new ExtensionFilter(FILE_FORMAT_TITLE, FILE_FORMAT_EXTENSION));

			File selectedFile = saveFileOptions.showSaveDialog(GuiView.getInstance().getStage());

			if (selectedFile != null) {
				// WRITE OUT DATA HERE
				System.out.println("Saving file name in directory: " + selectedFile.getPath());

				// Save the data to a file
				try (BufferedWriter bw = new BufferedWriter(
						new FileWriter(selectedFile.getPath()))) {
					GuiModel.getInstance().saveColumnData(bw, readingSeries, yUnit);
				} catch (IOException e) {
					e.printStackTrace();
				}

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

	/**
	 * An event handler which deals with deleting boundary data points when the user right clicks.
	 * 
	 * @param dataPoint
	 *            a single data point from the series.
	 * @param series
	 *            the specified upper or lower boundary series which holds all of the boundary data
	 *            points
	 * @return an event handler to deal with deleting elements from the series.
	 */
	// FIXME: THE DELTE NODE FROM APPEARS MORE THAN ONCE...
	// private EventHandler<MouseEvent> deleteData(XYChart.Data<Number, Number> dataPoint,
	// XYChart.Series<Number, Number> series) {
	// EventHandler<MouseEvent> removeSelectedDataPoints = new EventHandler<MouseEvent>() {
	//
	// @Override
	// public void handle(MouseEvent event) {
	// if (event.getButton() == MouseButton.SECONDARY) {
	// System.out.println("OLD SIZE: " + series.getData().size());
	// series.getData().remove(dataPoint);
	// System.out.println("DELETE NODE FROM: " + series.getName() + " NEW SIZE: "
	// + series.getData().size());
	// }
	// }
	// };
	// return removeSelectedDataPoints;
	// }
	private void deleteData(XYChart.Data<Number, Number> dataPoint,
			XYChart.Series<Number, Number> series) {
		dataPoint.getNode().setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					System.out.println("OLD SIZE: " + series.getData().size());
					series.getData().remove(dataPoint);
					System.out.println("DELETE NODE FROM: " + series.getName() + " NEW SIZE: "
							+ series.getData().size());
				}
			}
		});
	}

	/**
	 * 
	 * @param dataPoint
	 * @return
	 */
	private void moveData(XYChart.Data<Number, Number> dataPoint) {
		dataPoint.getNode().setOnMouseMoved(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (event.isShiftDown()) {

					// Change cursor
					dataPoint.getNode().setCursor(Cursor.HAND);

					// Change position to match the mouse coords.
					dataPoint.setXValue(getMouseChartCoords(event, true));
					dataPoint.setYValue(getMouseChartCoords(event, false));
				}
			}
		});
	}
	// private EventHandler<MouseEvent> moveData(XYChart.Data<Number, Number> dataPoint) {
	// EventHandler<MouseEvent> moveDataPoints = new EventHandler<MouseEvent>() {
	//
	// @Override
	// public void handle(MouseEvent event) {
	// if (event.isShiftDown()) {
	//
	// // Change cursor
	// dataPoint.getNode().setCursor(Cursor.HAND);
	//
	// // Change position to match the mouse coords.
	// dataPoint.setXValue(getMouseChartCoords(event, true));
	// dataPoint.setYValue(getMouseChartCoords(event, false));
	//
	// // System.out.println(oneDecimal.format(dataPoints.getXValue()) + " || "
	// // + i + " :: " + oneDecimal.format(dataPoints.getYValue()));
	// }
	// }
	// };
	// return moveDataPoints;
	// }

	private EventHandler<MouseEvent> changeCursor(XYChart.Data<Number, Number> dataPoint) {
		EventHandler<MouseEvent> changeCursorType = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				// Change cursor
				dataPoint.getNode().setCursor(Cursor.HAND);
			}
		};

		return changeCursorType;
	}

	/**
	 * displays the x,y values of the node when it's passed over
	 * 
	 * @param dataPoint
	 * @param index
	 * @return
	 */
	private EventHandler<MouseEvent> getDataXYValues(XYChart.Data<Number, Number> dataPoint,
			int index) {
		EventHandler<MouseEvent> getValues = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				// CHANGE TEXT HERE
				// IF LOADED FROM SD CARD -> USE ISO 8601 DURATION FORMAT
				// ELSE IF LOADED FROM REAL DATA -> USE ISO 8601 FORMAT
				xDataCoord.setText(
						"X: Sample: " + index + " :: " + oneDecimal.format(dataPoint.getXValue()));
				yDataCoord.setText("Y: " + oneDecimal.format(dataPoint.getYValue()));
			}
		};
		return getValues;
	}

	private EventHandler<MouseEvent> resetDataXYValues() {
		EventHandler<MouseEvent> getValues = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				xDataCoord.setText("X: ");
				yDataCoord.setText("Y: ");
			}
		};
		return getValues;
	}

	/**
	 * 
	 * @param series
	 * @param coordinates
	 * @param seriesName
	 */
	private void setUpBoundaries(XYChart.Series<Number, Number> series,
			ArrayList<Number> coordinates) {

		// Add data to specified series.
		series.getData()
				.add(new XYChart.Data<Number, Number>(coordinates.get(0), coordinates.get(1)));

		// Modified the for loop for IDing the line chart data points from:
		// https://gist.github.com/TheItachiUchiha/c0ae68ef8e6273a7ac10
		for (int i = 0; i < series.getData().size(); i++) {
			Data<Number, Number> dataPoint = series.getData().get(i);

			// Display x and y values of data points of each boundary point, and remove them when
			// leaving the node
			// FIXME: PUT THIS ALSO ELSEWHERE THIS IS FOR THE ACTUAL DATA POINTS I THINK
			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
					getDataXYValues(dataPoint, i));
			dataPoint.getNode().addEventFilter(MouseEvent.MOUSE_EXITED, resetDataXYValues());

			// Changes mouse cursor type
			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED, changeCursor(dataPoint));

			// Deletes the point that was clicked
			// dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED,
			// deleteData(dataPoint,series));
			deleteData(dataPoint, series);

			// Moves the point that was hovered over + changes mouse cursor type
			// dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_MOVED, moveData(dataPoint));
			moveData(dataPoint);
		}
	}

	// TODO: NEED A SELECTION OF WHETHER OR NOT IT'S UPPER/LOWER BOUNDARY
	/**
	 * Gets the values of the mouse within the line chart graph. Modified off:
	 * http://stackoverflow.com/questions/28562195/how-to-get-mouse-position-in-chart-space. To be
	 */
	private void setBoundaries(Node chartBackground) {

		chartBackground.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				// left mouse button and mask testing is selected
				if (event.getButton() == MouseButton.PRIMARY && maskTestingSelected) {

					// Remove for now the animation/autoscaling.
					lineChart.setAnimated(false);
					lineChart.getXAxis().setAutoRanging(false);
					lineChart.getYAxis().setAutoRanging(false);

					yAxis.setAnimated(false);
					yAxis.setForceZeroInRange(false);

					// Gets the coordinates
					coordinates = getMouseToChartCoords(event);

					if (highBoundaryCheckbox.isSelected()) {
						// removeLowBoundaries(lowerSeries);

						// Set up high boundary
						setUpBoundaries(upperSeries, coordinates);

					} else {
						// Set up low boundary
						setUpBoundaries(lowerSeries, coordinates);
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

			// show mask testing components
			separatorLine.setVisible(true);
			plotCoordLabel.setVisible(true);
			xDataCoord.setVisible(true);
			yDataCoord.setVisible(true);
			importMaskBtn.setVisible(true);
			exportMaskBtn.setVisible(true);
			
			addBoundaryLabel.setVisible(true);
			highBoundaryCheckbox.setVisible(true);
			lowBoundaryCheckbox.setVisible(true);
			
			setHighBtn.setVisible(true);
			setLowBtn.setVisible(true);
			setBoundaryLabel.setVisible(true);
			
			System.out.println("MASK TESTING SELECTED");
		} else {
			// Not Selected
			maskTestingSelected = false;
			
			// hide mask testing components
			separatorLine.setVisible(false);
			plotCoordLabel.setVisible(false);
			xDataCoord.setVisible(false);
			yDataCoord.setVisible(false);
			importMaskBtn.setVisible(false);
			exportMaskBtn.setVisible(false);
			
			addBoundaryLabel.setVisible(false);
			highBoundaryCheckbox.setVisible(false);
			lowBoundaryCheckbox.setVisible(false);
			
			setHighBtn.setVisible(false);
			setLowBtn.setVisible(false);
			setBoundaryLabel.setVisible(false);
			
			
			System.out.println("MASK TESTING DE-SELECTED");
		}
	}

	/**
	 * Sets the upperboundary to be selected
	 */
	@FXML
	private void setToHighBoundary() {
		highBoundaryCheckbox.setSelected(true);
		lowBoundaryCheckbox.setSelected(false);
		System.out.println("HIGH");
	}

	// FIXME: FOR SOME REASON IT DOESN"T WORK WITH REMOVEEVENTHANDLER...
	/**
	 * Private helper function to remove listeners
	 */
	private void removeAllListeners(XYChart.Series<Number, Number> series) {
		// chartBackground.setOnMouseClicked(null);

		for (XYChart.Data<Number, Number> dataPoint : series.getData()) {
			dataPoint.getNode().setOnMouseClicked(null); // remove deleting option
			dataPoint.getNode().setOnMouseMoved(null); // remove moving option
		}
	}

	// FIXME: remove all listeners to the series.
	@FXML
	private void setHighBoundary() {
		// Remove all data listeners
		removeAllListeners(upperSeries);
		addMask(upperSeries, upperBoundary, true);
	}

	// FIXME: MAKE EITHER A RADIO BTN
	@FXML
	private void setToLowBoundary() {
		highBoundaryCheckbox.setSelected(false);
		lowBoundaryCheckbox.setSelected(true);
		System.out.println("LOW");
	}

	// FIXME: make sure that the original points of the polygon match the expanded size
	/**
	 * Modifies the specified polygon's points (also adding points) to create a mask over the points
	 * specified from the specified series.
	 * 
	 * @param series
	 * @param mask
	 * @param upperMask
	 */
	private void addMask(XYChart.Series<Number, Number> series, Polygon mask, boolean upperMask) {
		System.out.println("SETTING UP MASK FOR: " + series.getName());
		System.out.println("SERIES SIZE: " + series.getData().size());

		int k = 0;
		System.out.println("DATA SIZE:" + series.getData().size());

		double maxChartBackgroundHeight = chartBackground.getLayoutBounds().getHeight();

		for (int j = 0; j < series.getData().size(); j++) {
			k = 2;
			// Each saved data point of the series
			Data<Number, Number> dataPoint = series.getData().get(j);

			// X and Y of each data pints
			double x = lineChart.getXAxis().getDisplayPosition(dataPoint.getXValue());
			double y = 0D;

			if (upperMask) {
				y = lineChart.getYAxis().getDisplayPosition(dataPoint.getYValue());
			} else {
				// reversed y for the lower boundary
				y = -1 * (maxChartBackgroundHeight
						+ (-lineChart.getYAxis().getDisplayPosition(dataPoint.getYValue())));
			}

			// double x1 = lineChart.getXAxis().getDisplayPosition(xAxis.getLowerBound());
			// double y1 = lineChart.getYAxis().getDisplayPosition(yAxis.getLowerBound());

			System.out.println("YO:" + x + " :: y2: " + y);

			if (j == 0) {
				// FIXME: maybe better way of doing this
				// upperBoundary.getPoints().set(1, y);
				mask.getPoints().add(2, x);
				mask.getPoints().add(3, y);

				// TODO: FIX ONE POINT SETTING
				// if (series.getData().size() == 1) {
				// mask.getPoints().set(5, y);
				// }
			} else {
				System.out.println("1 UP: " + k + " : J: " + j);
				// Move around the points as needed

				while ((x > mask.getPoints().get(k))) {// && (k <= series.getData().size())) {
														// //**********exception here
					System.out.println("2 UP: " + k + " : J: " + j);
					k += 2; // keep looking at x values.
				}

				mask.getPoints().add(k, x); // add x after the x,y pair
				mask.getPoints().add(k + 1, y); // add y after x,y pair. //ERROR
												// HERE...

				// Use closest boundary if start and end not supplied.
				if (j == series.getData().size() - 1) {
					// Set it to the last mask point and first mask point at y-axis level.
					Double lastMaskValue = mask.getPoints().get(1 + (2 * (j + 1)));
					Double firstMaskValue = mask.getPoints().get(3);

					mask.getPoints().set(1, firstMaskValue);
					mask.getPoints().set(3 + (2 * (j + 1)), lastMaskValue);
				}
			}
		}

		for (Double d : mask.getPoints()) {
			System.out.println("Point: " + d);
		}
	}

	///////////// b
	/**
	 * FIXME: set up initial position of polygon boundaries to match the maximised/expanded GUI.
	 */
	@FXML
	private void setLowBoundary() {
		removeAllListeners(lowerSeries);
		System.out.println("LB1: " + lowerBoundary.getPoints());
		addMask(lowerSeries, lowerBoundary, false);
		System.out.println("LB2: " + lowerBoundary.getPoints());
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

		upperSeries.setName("UpperBoundary");
		lowerSeries.setName("LowerBoundary");

		// Add to lineChart
		lineChart.getData().add(upperSeries);
		lineChart.getData().add(lowerSeries);
		lineChart.getData().add(readingSeries);

		// the lookup function found from https://gist.github.com/avitry/5598699
		// in order to get the chart background.
		chartBackground = lineChart.lookup(".chart-plot-background");

		// Bounds chartAreaBounds =
		// chartBackground.localToScene(chartBackground.getBoundsInLocal());

		// System.out.println("X: " + chartAreaBounds.getMaxX());
		// System.out.println("Y: " + chartAreaBounds.getMaxY());
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

// FIXME: lines cannot be drawn over...
