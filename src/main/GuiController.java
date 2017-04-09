package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.Point2D;

import javafx.application.Platform;
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
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
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
	/* Components required for resizing the GUI when maximising or resizing */
	@FXML
	Group root;
	@FXML
	Pane appPane;
	@FXML
	Label GraphingResultsLabel;
	@FXML
	AnchorPane rightAnchor;

	// @FXML
	// AnchorPane leftAnchor;
	@FXML
	private Button runMTBtn;

	// Components to display dummy data
	private int dataPlotPosition = 0;
	public volatile boolean resistance = false;
	public volatile boolean voltage = false;
	public volatile boolean current = false;

	/* Components relating to the connected mode. */
	@FXML
	private RadioButton connRBtn;
	@FXML
	private Button pauseBtn;
	private boolean isPaused = false; // flag for if pauseBtn has been clicked
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
	private boolean maskTestingSelected = false; // flag for if maskTestingBtn has been clicked
	@FXML
	private Line separatorLine;
	@FXML
	private Button importMaskBtn;
	@FXML
	private Button exportMaskBtn;
	@FXML
	private Label maskStatusLabel;

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

	/* Components of the mask */
	// Holds the upper and lower boundary points
	XYChart.Series<Number, Number> upperSeries = new XYChart.Series<>();
	XYChart.Series<Number, Number> lowerSeries = new XYChart.Series<>();

	// The upper and lower boundary area
	@FXML
	Polygon upperBoundary;
	@FXML
	Polygon lowerBoundary;

	/* Elements relating to the line chart. */
	@FXML
	LineChart<Number, Number> lineChart;
	@FXML
	NumberAxis yAxis;
	@FXML
	NumberAxis xAxis;
	Node chartBackground; // Handle on chart background for getting lineChart coords
	private ArrayList<Number> coordinates = new ArrayList<>(); // ???

	// Components for shifting left and right the x-axis
	@FXML
	private Button leftBtn;
	@FXML
	private Button rightBtn;

	/* Holds the line chart */
	XYChart.Series<Number, Number> readingSeries = new XYChart.Series<>();
	ArrayList<String> yUnit = new ArrayList<>(); // The y-unit displayed

	/* Holds x and y coordinates of mouse position. */
	@FXML
	private Label yCoordValues;
	@FXML
	private Label xCoordValues;
	@FXML
	private Label coordLabel;

	DecimalFormat oneDecimal = new DecimalFormat("0.000");
	DecimalFormat timeDecimal = new DecimalFormat("0.0");

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

	private int clickCounterLower = 0;
	private int clickCounterHigh = 0;

	private int errorCounter = 0;

	/* Constants */
	private static final String FILE_FORMAT_EXTENSION = "*.csv";
	private static final String FILE_FORMAT_TITLE = "Comma Separated Files";
	private static final String FILE_DIR = "./Saved Data/";
	private static final int UPPER_BOUND = 10;
	public static final double SAMPLES_PER_SECOND = 2D;
	private static GuiController instance;

	public GuiController() {
		instance = this;
	}

	public static GuiController getInstance() {
		if (instance == null) {
			instance = new GuiController();
			System.out.println("Initialised GuiController[SINGLETON]");
		}
		return instance;
	}

	// Decrease the upper and lower bounds of the xaxis
	@FXML
	private void moveXAxisLeft() {

		double newAxisUpperValue = xAxis.getUpperBound() - 1;
		double newAxisLowerValue = xAxis.getLowerBound() - 1;

		if (newAxisLowerValue >= 0) {
			xAxis.setUpperBound(newAxisUpperValue);
			xAxis.setLowerBound(newAxisLowerValue);
		}

	}

	// Increase the upper and lower bounds of the xaxis
	@FXML
	private void moveXAxisRight() {
		double newAxisUpperValue = xAxis.getUpperBound() + 1;
		double newAxisLowerValue = xAxis.getLowerBound() + 1;

		xAxis.setUpperBound(newAxisUpperValue);
		xAxis.setLowerBound(newAxisLowerValue);
	}

	@FXML
	private void measureVoltage() {
		System.out.println("I clicked on voltage");
		voltage = true;
		resistance = false;
		current = false;

		resetXAxis();
		// Reset the plot data
		readingSeries.getData().clear();
		yUnit.clear();

		yAxis.setLabel("Measurements [V]");
		// Run thread here with 2nd column of data
		RecordedResults.shutdownRecordedResultsThread();

		String file = FILE_DIR + "voltage.csv";
		System.out.println(file);
		RecordedResults.PlaybackData container = new RecordedResults.PlaybackData(file);
		Thread thread = new Thread(container);
		RecordedResults.dataPlaybackContainer = container;
		thread.start();
	}

	@FXML
	private void measureCurrent() {
		System.out.println("I clicked on current");
		voltage = false;
		resistance = false;
		current = true;

		resetXAxis();

		// Reset the plot data
		readingSeries.getData().clear();
		yUnit.clear();

		yAxis.setLabel("Measurements [mA]");

		String file = FILE_DIR + "current.csv";
		// Run thread here with 2nd column of data
		RecordedResults.shutdownRecordedResultsThread();
		RecordedResults.PlaybackData container = new RecordedResults.PlaybackData(file);
		Thread thread = new Thread(container);
		RecordedResults.dataPlaybackContainer = container;
		thread.start();
	}

	@FXML
	private void measureResistance() {
		System.out.println("I clicked on resistance");
		// Run thread here with 2nd column of data
		resistance = true;
		voltage = false;
		current = false;

		resetXAxis();
		// Reset the plot data
		readingSeries.getData().clear();
		yUnit.clear();

		String file = FILE_DIR + "resistance.csv";
		RecordedResults.shutdownRecordedResultsThread();

		RecordedResults.PlaybackData container = new RecordedResults.PlaybackData(file);
		Thread thread = new Thread(container);
		RecordedResults.dataPlaybackContainer = container;
		thread.start();
	}

	/**
	 * Updates the data displayed on the analog, digital and strip chart displays.
	 */
	public void recordAndDisplayDummyData(double multimeterReading) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					recordAndDisplayDummyData(multimeterReading);
				}
			});
			return;
		}

		// Update all software displays
		updateDisplay(multimeterReading);
	}

	private void updateDisplay(Double multimeterReading) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					updateDisplay(multimeterReading);
				}
			});
			return;
		}

		// change units when ranges are overcome
		autoRangeUnit(multimeterReading);

		multimeterDisplay.setText(Character.toString((char) 177) + multimeterReading.toString());

		readingSeries.getData().add(new XYChart.Data<Number, Number>(
				dataPlotPosition / SAMPLES_PER_SECOND, multimeterReading));

		double secs = dataPlotPosition / SAMPLES_PER_SECOND;

		// displays plot
		readingSeries.getData().get(dataPlotPosition).getNode().addEventHandler(
				MouseEvent.MOUSE_ENTERED,
				getDataXYValues(readingSeries.getData().get(dataPlotPosition), dataPlotPosition));
		// reset the values displayed
		// readingSeries.getData().get(dataPlotPosition).getNode().addEventFilter(MouseEvent.MOUSE_EXITED,
		// resetDataXYValues());

		dataPlotPosition++;

		// Update chart
		int dataBoundsRange = (int) Math.ceil(dataPlotPosition / SAMPLES_PER_SECOND);

		if (dataBoundsRange > UPPER_BOUND) {
			xAxis.setLowerBound(dataBoundsRange - UPPER_BOUND);
			xAxis.setUpperBound(dataBoundsRange);
		}

	}

	private void resetXAxis() {
		if (voltage) {
			dataPlotPosition = 0;
			xAxis.setLowerBound(0);
			xAxis.setUpperBound(UPPER_BOUND);
		} else if (current) {
			dataPlotPosition = 0;
			xAxis.setLowerBound(0);
			xAxis.setUpperBound(UPPER_BOUND);
		} else if (resistance) {
			dataPlotPosition = 0;
			xAxis.setLowerBound(0);
			xAxis.setUpperBound(UPPER_BOUND);
		}
	}

	public void autoRangeUnit(Double dataValue) {
		String ohmSymbol = Character.toString((char) 8486);

		if (voltage) {
			yUnit.add("V");
		} else if (current) {
			yUnit.add("mA");
		} else if (resistance) {
			if (dataValue < 1000) {
				yUnit.add(ohmSymbol);
				yAxis.setLabel("Measurements [" + ohmSymbol + "]");
			} else if (dataValue >= 1000 && dataValue < 1000000) {
				yUnit.add("k" + ohmSymbol);
				yAxis.setLabel("Measurements [" + "k" + ohmSymbol + "]");
			} else if (dataValue >= 1000000) {
				yUnit.add("M" + ohmSymbol);
				yAxis.setLabel("Measurements [" + "M" + ohmSymbol + "]");
			}
		}

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
				RecordedResults.shutdownRecordedResultsThread();
				// Reset the plot data
				readingSeries.getData().clear();

				dataPlotPosition = 0;
				xAxis.setLowerBound(0);
				xAxis.setUpperBound(UPPER_BOUND);
				// TODO: CHANGE
				yAxis.setLowerBound(-10);
				yAxis.setUpperBound(50);

				yUnit.clear();
				xDataCoord.setText("X: ");
				yDataCoord.setText("Y: ");
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
				importMaskBtn.setVisible(false);
				exportMaskBtn.setVisible(false);
				runMTBtn.setVisible(false);
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
			// Reset the plot data
			readingSeries.getData().clear();
			yUnit.clear();
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

			// Enable digital multimeter components
			voltageBtn.setDisable(true);
			currentBtn.setDisable(true);
			resistanceBtn.setDisable(true);
			multimeterDisplay.setDisable(true);
			modeLabel.setDisable(true);
			logicBtn.setDisable(true);
			continuityBtn.setDisable(true);

			RecordedResults.pauseRecordedResultsThread(true);
			System.out.println("DATA IS PAUSED");
		} else {// TODO: check for any yUnit changes
			isPaused = false;
			pauseBtn.setText("Pause");

			// Enable digital multimeter components
			voltageBtn.setDisable(false);
			currentBtn.setDisable(false);
			resistanceBtn.setDisable(false);
			multimeterDisplay.setDisable(false);
			modeLabel.setDisable(false);
			logicBtn.setDisable(false);
			continuityBtn.setDisable(false);

			RecordedResults.pauseRecordedResultsThread(false);
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
				yUnit.add(s);
				yAxis.setLabel("Measurements [" + yUnit.get(0) + "]");
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

				// FIXME: RESET BOUNDS TO 0 IF IT CHANGES
				// Update chart bounds
				int dataBoundsRange = (int) Math.ceil(i / SAMPLES_PER_SECOND); // 2 = SAMPLES/SECOND
				if (dataBoundsRange > UPPER_BOUND) {
					xAxis.setLowerBound(dataBoundsRange - UPPER_BOUND);
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
			// RecordedResults.shutdownRecordedResultsThread();
			// reset maybe?
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
			RecordedResults.shutdownRecordedResultsThread();
			// Reset the plot data
			readingSeries.getData().clear();

			dataPlotPosition = 0;
			xAxis.setLowerBound(0);
			xAxis.setUpperBound(UPPER_BOUND);
			// TODO: CHANGE
			yAxis.setLowerBound(-15);
			yAxis.setUpperBound(55);

			yUnit.clear();
			xDataCoord.setText("X: ");
			yDataCoord.setText("Y: ");

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
	private void moveData(XYChart.Data<Number, Number> dataPoint,
			XYChart.Series<Number, Number> series) {

		dataPoint.getNode().setOnMouseMoved(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (event.isShiftDown()) {

					// Change cursor
					dataPoint.getNode().setCursor(Cursor.HAND);

					// FIXME: ENSURE YOU CANNOT MOVE IT INTO INVALID AREA
					if (series.equals(lowerSeries)) {
						if (checkOverlap(getMouseToChartCoords(event), lowerSeries, upperSeries,
								clickCounterLower)) {

							// Change position to match the mouse coords.
							dataPoint.setXValue(getMouseChartCoords(event, true));
							dataPoint.setYValue(getMouseChartCoords(event, false));
						}
					} else {
						if (checkOverlap(getMouseToChartCoords(event), upperSeries, lowerSeries,
								clickCounterHigh)) {

							// Change position to match the mouse coords.
							dataPoint.setXValue(getMouseChartCoords(event, true));
							dataPoint.setYValue(getMouseChartCoords(event, false));
						}
					}

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
				String isoDurationFormat = "PT" + (index / SAMPLES_PER_SECOND) + "S";
				// IF LOADED FROM SD CARD -> USE ISO 8601 DURATION FORMAT
				// ELSE IF LOADED FROM REAL DATA -> USE ISO 8601 FORMAT
				xDataCoord.setText("X: Sample: " + (index + 1) + " || Duration: "
						+ Duration.parse(isoDurationFormat)); // oneDecimal.format(dataPoint.getXValue()));
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
			// TODO: CHECK IF I NEED THIS FOR THE MASK NODES
			// dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
			// getDataXYValues(dataPoint, i));
			// dataPoint.getNode().addEventFilter(MouseEvent.MOUSE_EXITED, resetDataXYValues());

			// Changes mouse cursor type
			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED, changeCursor(dataPoint));

			// Deletes the point that was clicked
			// dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED,
			// deleteData(dataPoint,series));
			deleteData(dataPoint, series);

			// Moves the point that was hovered over + changes mouse cursor type
			// dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_MOVED, moveData(dataPoint));
			moveData(dataPoint, series);
		}
	}

	// TODO: NEED A SELECTION OF WHETHER OR NOT IT'S UPPER/LOWER BOUNDARY
	/**
	 * Gets the values of the mouse within the line chart graph. Modified off:
	 * http://stackoverflow.com/questions/28562195/how-to-get-mouse-position-in-chart-space. To be
	 */
	private void setBoundaries(Node chartBackground) {
		// TODO: MAKE THIS SO THREADS CAN"T FUCK IT UP

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
						// Check that no overlap before adding new points
						if (checkOverlap(coordinates, upperSeries, lowerSeries, clickCounterHigh)) {
							// Set up low boundary
							setUpBoundaries(upperSeries, coordinates);

							clickCounterHigh++;
						}

					} else {

						// Check that no overlap before adding new points
						if (checkOverlap(coordinates, lowerSeries, upperSeries,
								clickCounterLower)) {
							// Set up low boundary
							setUpBoundaries(lowerSeries, coordinates);

							clickCounterLower++;
						}
					}

				}
			}
		});

	}

	////////////
	private boolean checkOverlap(ArrayList<Number> coordinates,
			XYChart.Series<Number, Number> newSeries, XYChart.Series<Number, Number> existingSeries,
			int counter) {
		System.out.println("CC: " + counter);
		System.out.println("SIZE: " + existingSeries.getData().size());

		// Values of new points
		float tempX = coordinates.get(0).floatValue();
		float tempY = coordinates.get(1).floatValue();

		// TODO: make sure the first point cannot start within the opposite side
		if (existingSeries.getData().size() > 0 && newSeries.getData().size() >= 1) { // >= 1
			float orX = newSeries.getData().get(counter - 1).getXValue().floatValue();
			float orY = newSeries.getData().get(counter - 1).getYValue().floatValue();

			Line2D checkIntersection = new Line2D();
			checkIntersection.setLine(new Point2D(orX, orY), new Point2D(tempX, tempY));

			for (int i = 0; i < existingSeries.getData().size() - 1; i++) {
				// Next points
				Data<Number, Number> dataPoint = existingSeries.getData().get(i);
				Data<Number, Number> dataPoint2 = existingSeries.getData().get(i + 1);

				// Overlaps
				if (checkIntersection.intersectsLine(new Line2D(
						new Point2D(dataPoint.getXValue().floatValue(),
								dataPoint.getYValue().floatValue()),
						new Point2D(dataPoint2.getXValue().floatValue(),
								dataPoint2.getYValue().floatValue())))) {
					// WARNING MESSAGE
					illegalMaskPoint();
					return false;
				}
			}

		}
		return true;
	}

	// Warning to user that you cannot overlap regions
	private void illegalMaskPoint() {
		// Add a warning pop up
		String title = "Illegal Region Selection";
		String warning = "The lower and upper bounds cannot overlap!";
		String errorType = "modena/dialog-warning.png";
		AlertType alertType = AlertType.WARNING;
		// Notify User of existing file.
		Optional<ButtonType> result = GuiView.getInstance()
				.alertUser(title, warning, errorType, alertType).showAndWait();

		if (result.get() == ButtonType.OK) {
			return;
		}
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
			importMaskBtn.setVisible(true);
			exportMaskBtn.setVisible(true);

			addBoundaryLabel.setVisible(true);
			highBoundaryCheckbox.setVisible(true);
			lowBoundaryCheckbox.setVisible(true);

			setHighBtn.setVisible(true);
			setLowBtn.setVisible(true);
			setBoundaryLabel.setVisible(true);

			runMTBtn.setVisible(true);
			System.out.println("MASK TESTING SELECTED");
		} else {
			// Not Selected
			maskTestingSelected = false;

			// hide mask testing components
			separatorLine.setVisible(false);
			importMaskBtn.setVisible(false);
			exportMaskBtn.setVisible(false);

			addBoundaryLabel.setVisible(false);
			highBoundaryCheckbox.setVisible(false);
			lowBoundaryCheckbox.setVisible(false);

			setHighBtn.setVisible(false);
			setLowBtn.setVisible(false);
			setBoundaryLabel.setVisible(false);

			runMTBtn.setVisible(false);
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

	private Comparator<XYChart.Data<Number, Number>> sortChart() {
		// Comparator.
		final Comparator<XYChart.Data<Number, Number>> CHART_ORDER = new Comparator<XYChart.Data<Number, Number>>() {
			public int compare(XYChart.Data<Number, Number> e1, XYChart.Data<Number, Number> e2) {
				if (e2.getXValue().doubleValue() > e1.getXValue().doubleValue()) {
					return -1;
				} else if (e2.getXValue().doubleValue() < e1.getXValue().doubleValue()) {
					return 1;
				} else {
					return 0;
				}
			}
		};
		return CHART_ORDER;
	}

	private void orderAndAddBoundaryPoints(XYChart.Series<Number, Number> series) {
		// sort in increasing order by x values
		series.getData().sort(sortChart());

		// add first and last points
		Data<Number, Number> initialPoint = new Data<>();
		initialPoint.setXValue(xAxis.getLowerBound());
		initialPoint.setYValue(series.getData().get(0).getYValue());

		Data<Number, Number> finalPoint = new Data<>();
		finalPoint.setXValue(xAxis.getUpperBound());
		finalPoint.setYValue(series.getData().get(series.getData().size() - 1).getYValue());

		// FIXME: CHECK IT WORKS: ADDING FIRST AND LAST POINTS IF THERE IT HASN"T BEEN DONE
		// FIXME: ERRORS IF I CLICK MORE THAN ONCE THE SET BUTTON
		if ((series.getData().get(0).getXValue().doubleValue() != xAxis.getLowerBound())
				|| series.getData().get(series.getData().size()).getXValue().doubleValue() != xAxis
						.getUpperBound()) {
			// Add and hide first point
			series.getData().add(0, initialPoint);
			series.getData().get(0).getNode().setVisible(false);

			// Add and hide final point
			series.getData().add(series.getData().size(), finalPoint);
			series.getData().get(series.getData().size() - 1).getNode().setVisible(false);
		}

		System.out.println("SERIES DATA: " + series.getData().toString());
	}

	// FIXME: remove all listeners to the series.
	@FXML
	private void setHighBoundary() {
		// Remove all data listeners
		removeAllListeners(upperSeries);
		System.out.println("SET UPPER");

		// Order list in ascending order by xvalues and add boundary points if needed
		orderAndAddBoundaryPoints(upperSeries);

		addMask(upperSeries, upperBoundary, true);
	}

	// FIXME: MAKE EITHER A RADIO BTN
	@FXML
	private void setToLowBoundary() {
		highBoundaryCheckbox.setSelected(false);
		lowBoundaryCheckbox.setSelected(true);
		System.out.println("LOW");
	}

	private double modifiedSeriesY(Number dataPoint, boolean upperMask) {
		double maxChartBackgroundHeight = chartBackground.getLayoutBounds().getHeight();

		if (upperMask) {
			return lineChart.getYAxis().getDisplayPosition(dataPoint.doubleValue());
		} else {
			// reversed y for the lower boundary
			return -1 * (maxChartBackgroundHeight
					+ (-lineChart.getYAxis().getDisplayPosition(dataPoint.doubleValue())));
		}
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

		for (Double d : mask.getPoints()) {
			System.out.println("Point: " + d);
		}

		// Set up lower x-axis boundary
		double x = lineChart.getXAxis()
				.getDisplayPosition(series.getData().get(0).getXValue().doubleValue());
		double y = modifiedSeriesY(series.getData().get(0).getYValue(), upperMask);

		mask.getPoints().set(0, x);
		mask.getPoints().set(1, y);

		// Only deal with the points within the series, not the boundary point
		// start with opposite end and work way to the front

		// LAST POINT IS WEIRD
		for (int i = series.getData().size() - 2; i > 0; i--) {

			// Each main data point of the series
			Data<Number, Number> dataPoint = series.getData().get(i);
			// X and Y of each data pints
			// double x1 = lineChart.getXAxis().getDisplayPosition(xAxis.getLowerBound());
			// double y1 = lineChart.getYAxis().getDisplayPosition(yAxis.getLowerBound());

			x = lineChart.getXAxis().getDisplayPosition(dataPoint.getXValue());
			y = modifiedSeriesY(dataPoint.getYValue(), upperMask);

			System.out.println("DP: " + dataPoint.toString() + " :: " + x + ", " + y);

			mask.getPoints().add(2, x);
			mask.getPoints().add(3, y);
		}
		// Add last boundary in
		int boundaryOffset = series.getData().size() - 2; // - 2 because of 2 boundary points
		int finalBoundaryPoint = series.getData().size() - 1;

		x = lineChart.getXAxis().getDisplayPosition(
				series.getData().get(finalBoundaryPoint).getXValue().doubleValue());
		y = modifiedSeriesY(series.getData().get(finalBoundaryPoint).getYValue(), upperMask);

		mask.getPoints().set(2 * (boundaryOffset + 1), x);
		mask.getPoints().set(1 + (2 * (boundaryOffset + 1)), y);

		// Set second last to boundary
		mask.getPoints().set(mask.getPoints().size() - 4, x);
	}

	/**
	 * FIXME: set up initial position of polygon boundaries to match the maximised/expanded GUI.
	 */
	@FXML
	private void setLowBoundary() {
		removeAllListeners(lowerSeries);
		System.out.println("LOWER SET");
		// Order list in ascending order by xvalues and add boundary points if needed
		orderAndAddBoundaryPoints(lowerSeries);

		addMask(lowerSeries, lowerBoundary, false);
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

	private void displayPlotCoordinates() {
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

	// FIXME: REQUIRE THAT FILE DATA HAS BEEN LOADED IN
	@FXML
	private void runMaskTest() {
		System.out.println("RUN MASK TESTING");

		// readingSeries
		for (int i = 0; i < readingSeries.getData().size() - 1; i++) {
			// test lower
			checkOverlaping(readingSeries.getData().get(i), readingSeries.getData().get(i + 1),
					lowerSeries);
			// test upper
			checkOverlaping(readingSeries.getData().get(i), readingSeries.getData().get(i + 1),
					upperSeries);
		}

		// Set outcome to pass or fail
		if (errorCounter > 0) {
			maskStatusLabel.setText("FAIL");
		} else {
			maskStatusLabel.setText("PASS");
		}
	}

	// FIXME: NEED TO ACCOUNT FOR THE AREA UNDERNEATH THE MASK...
	private void checkOverlaping(Data<Number, Number> current, Data<Number, Number> next,
			XYChart.Series<Number, Number> existingSeries) {

		// Values of new points
		float tempX = current.getXValue().floatValue();
		float tempY = current.getYValue().floatValue();

		float nextX = next.getXValue().floatValue();
		float nextY = next.getYValue().floatValue();

		Line2D checkIntersection = new Line2D();
		checkIntersection.setLine(new Point2D(tempX, tempY), new Point2D(nextX, nextY));

		if (existingSeries.getData().size() > 0) {
			for (int i = 0; i < existingSeries.getData().size() - 1; i++) {
				// Next points
				Data<Number, Number> dataPoint = existingSeries.getData().get(i);
				Data<Number, Number> dataPoint2 = existingSeries.getData().get(i + 1);

				// Overlaps
				if (checkIntersection.intersectsLine(new Line2D(
						new Point2D(dataPoint.getXValue().floatValue(),
								dataPoint.getYValue().floatValue()),
						new Point2D(dataPoint2.getXValue().floatValue(),
								dataPoint2.getYValue().floatValue())))) {

					errorCounter++;
					System.out.println("COUNTER: " + errorCounter + " || overlap on: "
							+ existingSeries.getName());
					System.out.println(
							"origin" + tempX + " | " + tempY + " | " + nextX + " | " + nextY);

					System.out.println("mask: " + dataPoint.getXValue().floatValue() + " | "
							+ dataPoint.getYValue().floatValue() + " | "
							+ dataPoint2.getXValue().floatValue() + " | "
							+ dataPoint2.getYValue().floatValue());
				}
			}
		}
	}

	@FXML
	private void importMaskData() {

		// Load in data
		FileChooser loadFileOptions = new FileChooser();
		loadFileOptions.setTitle("Load Mask Data");

		// Select file
		loadFileOptions.getExtensionFilters()
				.add(new ExtensionFilter(FILE_FORMAT_TITLE, FILE_FORMAT_EXTENSION));

		File selectedFile = loadFileOptions.showOpenDialog(GuiView.getInstance().getStage());

		// Only if file exists extract information from it
		if (selectedFile != null) {
			System.out.println("NAME: " + selectedFile.getPath());
			// Read it in
			for (String[] name : GuiModel.getInstance().readMaskData(selectedFile.getPath())) {
				
				yUnit.add(0,  "V");
				//FIXME: do a check that ensures the loaded in data matches
				if (name[3].equals(yUnit.get(0))) {
					System.out.println("SWEET");
					
				} else {
					System.out.println("NOT GOOD");
					break;
				}
				
				if (name[0].equals("high")) {

					XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(
							Double.parseDouble(name[1]), Double.parseDouble(name[2]));
					upperSeries.getData().add(dataPoint);
				} else {

					XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(
							Double.parseDouble(name[1]), Double.parseDouble(name[2]));
					lowerSeries.getData().add(dataPoint);

				}
			}
		}
		System.out.println(upperSeries.getData().size());
		System.out.println(lowerSeries.getData().size());
	}

	@FXML
	private void exportMaskData() {
		FileChooser saveFileOptions = new FileChooser();
		saveFileOptions.setTitle("Save Mask Data");

		// Set file format
		saveFileOptions.getExtensionFilters()
				.add(new ExtensionFilter(FILE_FORMAT_TITLE, FILE_FORMAT_EXTENSION));

		File selectedFile = saveFileOptions.showSaveDialog(GuiView.getInstance().getStage());

		if (selectedFile != null) {
			// Save the data to a file
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile.getPath()))) {
				// Only need one element from yUnit
				GuiModel.getInstance().saveMaskData(bw, upperSeries, yUnit.get(0));
				GuiModel.getInstance().saveMaskData(bw, lowerSeries, yUnit.get(0));
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("Saving Data");
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO: DO A PING TEST TO SEE IF THERE'S A CONNECTION.
		testConnection(connRBtn);

		upperSeries.setName("high");// "UpperBoundary");
		lowerSeries.setName("low");// "LowerBoundary");

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

		// display coordinates on the screen
		displayPlotCoordinates();

	}
}

// FIXME: lines cannot be drawn over...
