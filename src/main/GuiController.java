package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.fazecast.jSerialComm.SerialPort;
import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.Point2D;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class GuiController implements Initializable {
	EventHandlers event = new EventHandlers();

	/* Components required for resizing the GUI when maximising or resizing */
	@FXML
	Pane appPane;
	@FXML
	AnchorPane rightAnchor;
	@FXML
	AnchorPane midAnchor;
	@FXML
	AnchorPane leftAnchor;
	@FXML
	AnchorPane graphLabelAnchor;
	@FXML
	Label graphingResultsLabel;
	@FXML
	GridPane chartGrid;

	/* Components to display dummy data */
	private int dataPlotPosition = 0;
	public volatile boolean resistance = false;
	public volatile boolean voltage = false;
	public volatile boolean current = false;

	/* Components relating to the 'connected' mode */
	@FXML
	private RadioButton connRBtn;
	@FXML
	private Button pauseBtn;
	private boolean isPaused = false; // Flag for if pauseBtn has been clicked
	@FXML
	private Button saveBtn;
	@FXML
	Button discardBtn;

	/* Components relating to the 'disconnected' mode */
	@FXML
	private RadioButton disconnRBtn;
	@FXML
	private Button loadSavedData;
	@FXML
	private Label loadFileLabel;

	// To display plotted data points (mask boundary and read data)
	@FXML
	private Label plotCoordLabel;
	@FXML
	protected Label xDataCoord;
	@FXML
	protected Label yDataCoord;

	// Holds x & y coords of mouse position relative to linechart background
	@FXML
	Label yCoordValues;
	@FXML
	Label xCoordValues;

	/* Components relating to mask-testing */
	@FXML
	private Button maskTestingBtn;
	private boolean maskTestingSelected = false; // Flag for if maskTestingBtn has been clicked
	@FXML
	private Line separatorLine;
	@FXML
	private Button importMaskBtn;
	@FXML
	private Button exportMaskBtn;
	@FXML
	private Button setHighBtn;
	private boolean isHighBtnSelected = false; // Flag for if setHighBtn has been clicked
	@FXML
	private Button setLowBtn;
	private boolean isLowBtnSelected = false; // Flag for if setLowBtn has been clicked
	@FXML
	private Label createMaskLabel;
	@FXML
	private Button setMaskBtn;
	@FXML
	private Button runMaskBtn;
	private boolean retry = false;

	// To keep track of the previous mask boundary point
	private int lowCounter = 0;
	private int highCounter = 0;

	@FXML
	private Label maskStatusLabel;

	/* Line chart components */
	NumberAxis xAxis = new NumberAxis();
	NumberAxis yAxis = new NumberAxis();
	ModifiedLineChart lineChart;
	Node chartBackground; // Handle on chart background for getting lineChart coords

	// Holds the upper & lower boundary points and read data
	XYChart.Series<Number, Number> highMaskBoundarySeries = new XYChart.Series<>();
	XYChart.Series<Number, Number> lowMaskBoundarySeries = new XYChart.Series<>();
	XYChart.Series<Number, Number> readingSeries = new XYChart.Series<>();
	ArrayList<String> yUnit = new ArrayList<>(); // The y-unit displayed

	// Holds x and y values of potential mask plot points
	private ArrayList<Number> coordinates = new ArrayList<>();

	// Components for shifting the line chart x-axis left and right
	@FXML
	private Button leftBtn;
	@FXML
	private Button rightBtn;

	/* Components mirroring the LED multimeter from hardware */
	@FXML
	private Button voltageBtn;
	@FXML
	private Button currentBtn;
	@FXML
	private Button resistanceBtn;
	@FXML
	private TextArea multimeterDisplay;
	@FXML
	private Label modeLabel;
	@FXML
	private Button logicBtn;
	@FXML
	private Button continuityBtn;

	/* Constants */
	private static final DecimalFormat MEASUREMENT_DECIMAL = new DecimalFormat("0.000");
	private static final DecimalFormat TIME_DECIMAL = new DecimalFormat("0.0");

	private static final String FILE_FORMAT_EXTENSION = "*.csv";
	private static final String FILE_FORMAT_TITLE = "Comma Separated Files";
	private static final String FILE_DIR = "./Saved Data/";

	private static final double X_UPPER_BOUND = 10D;
	private static final double X_LOWER_BOUND = 0D;
	private static final double Y_UPPER_BOUND = -50D;
	private static final double Y_LOWER_BOUND = -10D;

	public static final double SAMPLES_PER_SECOND = 2D;

	public static GuiController instance;

	public GuiController() {
		// I am empty :(
		instance = this;
	}

	/**
	 * Decreases the upper and lower bounds of the x-axis. To let the user see the whole data plot
	 * instead of just a snapshot. Zero is the farthest the user can move the plot left.
	 */
	@FXML
	private void moveXAxisLeft() {

		double newAxisUpperValue = xAxis.getUpperBound() - 1;
		double newAxisLowerValue = xAxis.getLowerBound() - 1;

		if (newAxisLowerValue >= 0) {
			xAxis.setUpperBound(newAxisUpperValue);
			xAxis.setLowerBound(newAxisLowerValue);
		}

	}

	/**
	 * Increases the upper and lower bounds of the x-axis. To let the user see the whole data plot
	 * instead of just a snapshot.
	 */
	@FXML
	private void moveXAxisRight() {
		double newAxisUpperValue = xAxis.getUpperBound() + 1;
		double newAxisLowerValue = xAxis.getLowerBound() + 1;

		xAxis.setUpperBound(newAxisUpperValue);
		xAxis.setLowerBound(newAxisLowerValue);
	}

	/**
	 * Gets dummy data of voltage values and displays it. FIXME: convert to serial.
	 */
	@FXML
	private void measureVoltage() {
		System.out.println("I clicked on voltage");
		voltage = true;
		resistance = false;
		current = false;

		// Reset the plot data
		resetXAxis();
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

	/**
	 * Gets dummy data of current values and displays it. FIXME: convert to serial.
	 */
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

	/**
	 * Gets dummy data of resistance values and displays it. FIXME: Convert to serial
	 */
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
	 * Updates the displayed dummy data FIXME: convert to serial.
	 * 
	 * @param multimeterReading
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

	/**
	 * A private helper function for displaying dummy data on chart. FIXME: convert to serial.
	 * 
	 * @param multimeterReading
	 */
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
		autoRangeYUnit(multimeterReading);

		multimeterDisplay.setText(Character.toString((char) 177) + multimeterReading.toString());

		readingSeries.getData().add(new XYChart.Data<Number, Number>(
				dataPlotPosition / SAMPLES_PER_SECOND, multimeterReading));

		// Display dummy data plot
		readingSeries.getData().get(dataPlotPosition).getNode().addEventHandler(
				MouseEvent.MOUSE_ENTERED,
				event.getDataXYValues(readingSeries.getData().get(dataPlotPosition),
						dataPlotPosition, xDataCoord, yDataCoord));

		dataPlotPosition++;

		// Update chart
		int dataBoundsRange = (int) Math.ceil(dataPlotPosition / SAMPLES_PER_SECOND);

		if (dataBoundsRange > X_UPPER_BOUND) {
			xAxis.setLowerBound(dataBoundsRange - X_UPPER_BOUND);
			xAxis.setUpperBound(dataBoundsRange);
		}

	}

	/**
	 * If the units of the y-values change, then reset the axes bounds.
	 */
	private void resetXAxis() {
		if (voltage) {
			dataPlotPosition = 0;
			xAxis.setLowerBound(X_LOWER_BOUND);
			xAxis.setUpperBound(X_UPPER_BOUND);
		} else if (current) {
			dataPlotPosition = 0;
			xAxis.setLowerBound(X_LOWER_BOUND);
			xAxis.setUpperBound(X_UPPER_BOUND);
		} else if (resistance) {
			dataPlotPosition = 0;
			xAxis.setLowerBound(X_LOWER_BOUND);
			xAxis.setUpperBound(X_UPPER_BOUND);
		}
	}

	// TODO: change the data values to reflect where it should be.
	/**
	 * Changes the y-axis label if the units change + the units' range.
	 * 
	 * @param dataValue
	 *            the y-axis value.
	 */
	private void autoRangeYUnit(Double dataValue) {
		String ohmSymbol = Character.toString((char) 8486);

		if (voltage) {
			yUnit.add("V");
		} else if (current) {
			yUnit.add("mA");
		} else if (resistance) {
			yUnit.add("Ohm");
			if (dataValue < 1000) {
				yAxis.setLabel("Measurements [" + ohmSymbol + "]");
			} else if (dataValue >= 1000 && dataValue < 1000000) {
				yAxis.setLabel("Measurements [" + "k" + ohmSymbol + "]");
			} else if (dataValue >= 1000000) {
				yAxis.setLabel("Measurements [" + "M" + ohmSymbol + "]");
			}
		}

	}

	/**
	 * Selects the connected mode of the GUI if there is a connection, otherwise it's disabled.
	 */
	@FXML
	private void selectConnected() {

		// If there a connection and the radio button is selected
		if (connRBtn.isSelected() && testConnection(connRBtn)) {
			System.out.println("CONNECTED MODE INITIATED");
			System.out.println("//-------------------//");

			setupConnectedComponents();

		} else if (!testConnection(connRBtn)) {
			System.out.println("There is no test connection");

			disconnRBtn.setDisable(false);
		} else { // Assuming 'else' just covers when radio button is not selected. TODO: check.

			if (notifyUserConnected()) {
				System.out.println("CONNECTED MODE EXITED");

				disconnRBtn.setDisable(false);

				setupMoreConnectedComponents();

			} else {
				System.out.println("CONNECTED MODE STAYING");

				connRBtn.setSelected(true);
			}
		}
	}

	// TODO: setup connection test.
	// TODO: make sure we can tell if there is a oneway/twoway connection
	/**
	 * A private helper function to selectConnected. This function is called to determine if there
	 * is a connection (optical link).
	 * 
	 * @param connRBtn
	 *            the radio button which controls the
	 * @return true if the connection was successful, false otherwise
	 */
	private boolean testConnection(RadioButton connRBtn) {

		boolean connectionTestSuccess = true;

		// Enable the button.
		if (connectionTestSuccess) {
			System.out.println("CONNECTION ESTABLISHED");
			connRBtn.setDisable(false);
		} else {
			connRBtn.setDisable(true); // Leave the button disabled if no connection
		}
		return connectionTestSuccess;
	}

	// FIXME: make sure that the closed/reset stuff is done properly.
	/**
	 * A private helper function to 'selectConnected' which modifies the status of related
	 * components.
	 */
	private void setupConnectedComponents() {

		// Disable the disconnected mode from being editable during
		// connected mode
		disconnRBtn.setDisable(true);

		// Enable connected components
		pauseBtn.setDisable(false);
		saveBtn.setDisable(false);
		discardBtn.setDisable(false);

		// Enable digital multimeter components
		// FIXME: enable only if there is a two way connection
		voltageBtn.setDisable(false);
		currentBtn.setDisable(false);
		resistanceBtn.setDisable(false);
		multimeterDisplay.setDisable(false);
		modeLabel.setDisable(false);
		logicBtn.setDisable(false);
		continuityBtn.setDisable(false);
	}

	/**
	 * A private helper function to 'selectConnected'. Displays a pop-up message asking the user if
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

	// FIXME: make sure that the closed/reset stuff is done properly.
	/**
	 * A private helper function to 'selectConnected' which modifies the status of related
	 * components.
	 */
	private void setupMoreConnectedComponents() {
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
		// TODO: Close open connections.
		RecordedResults.shutdownRecordedResultsThread();

		// Reset the plot data
		readingSeries.getData().clear();

		dataPlotPosition = 0;
		xAxis.setLowerBound(X_LOWER_BOUND);
		xAxis.setUpperBound(X_UPPER_BOUND);

		// FIXME: Something funky here
		yAxis.setLowerBound(Y_LOWER_BOUND);
		yAxis.setUpperBound(Y_UPPER_BOUND);

		yUnit.clear();
		xDataCoord.setText("X: ");
		yDataCoord.setText("Y: ");
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
				runMaskBtn.setVisible(false);
				runMaskBtn.setDisable(true);

				setHighBtn.setVisible(false);
				setLowBtn.setVisible(false);
				setMaskBtn.setVisible(false);
				createMaskLabel.setVisible(false);

				// FIXME: make sure this is accurate CLEAR EVERYTHING
				readingSeries.getData().clear();
				highMaskBoundarySeries.getData().clear();
				lowMaskBoundarySeries.getData().clear();

			} else {
				System.out.println("DISCONNECTED MODE STAYING");

				disconnRBtn.setSelected(true);
			}
		}
	}

	/**
	 * A private helper function to 'selectDisconnected'. Displays a pop-up message asking the user
	 * if they wish to exit disconnected mode.
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

		if (result.get() == ButtonType.OK) { // User was OK exiting disconnected mode

			// Reset the plot data
			// TODO: Clear all data related things.
			readingSeries.getData().clear();
			yUnit.clear();

			return true;
		} else { // User was not OK exiting disconnected mode (cancelled or closed dialog box)

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

	/**
	 * Loads the saved data.
	 */
	@FXML
	private void loadFile() {
		// Clear data from list TODO: do I need this?
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

			readDataFromFile(selectedFile, mockXValues, mockYValues);
			addDataToSeries(mockXValues, mockYValues);
		}
	}

	/**
	 * A private helper function for 'loadFile' which reads in data from a given file.
	 * 
	 * @param selectedFile
	 *            file to read data from.
	 * @param mockXValues
	 *            temporary placeholder for x values.
	 * @param mockYValues
	 *            temporary placeholder for y values.
	 */
	private void readDataFromFile(File selectedFile, ArrayList<Double> mockXValues,
			ArrayList<Double> mockYValues) {

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
			convertMeasurementYUnit(yUnit.get(0));
			break;
		}
	}

	/**
	 * Sets the y-axis label
	 * 
	 * @param value
	 *            the y-unit value.
	 */
	private void convertMeasurementYUnit(String value) {
		String displayedYUnit = value;
		System.out.println(displayedYUnit);

		// Account for Ohm stuff.
		if (value.equals("Ohm")) {
			displayedYUnit = Character.toString((char) 8486);
		}

		yAxis.setLabel("Measurements [" + displayedYUnit + "]");
	}

	/**
	 * A private helper function for 'loadFile' which adds the x and y values to the line chart
	 * series.
	 * 
	 * @param mockXValues
	 *            holds all the x values.
	 * @param mockYValues
	 *            holds all the y values.
	 */
	private void addDataToSeries(ArrayList<Double> mockXValues, ArrayList<Double> mockYValues) {

		// Add data to series
		for (int i = 0; i < mockXValues.size(); i++) {
			readingSeries.getData()
					.add(new XYChart.Data<Number, Number>(mockXValues.get(i), mockYValues.get(i)));

			// Assign indexing to each node
			Data<Number, Number> dataPoint = readingSeries.getData().get(i);
			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
					event.getDataXYValues(dataPoint, i, xDataCoord, yDataCoord));

			// Update chart bounds if line chart exceeds them.
			int dataBoundsRange = (int) Math.ceil(i / SAMPLES_PER_SECOND);
			if (dataBoundsRange > X_UPPER_BOUND) {
				xAxis.setLowerBound(dataBoundsRange - X_UPPER_BOUND);
				xAxis.setUpperBound(dataBoundsRange);
			}
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
		} else { // Save out data

			// TODO: CHECK I NEED THIS RecordedResults.shutdownRecordedResultsThread();
			FileChooser saveFileOptions = new FileChooser();
			saveFileOptions.setTitle("Save Acquired Data");

			// Set file format
			saveFileOptions.getExtensionFilters()
					.add(new ExtensionFilter(FILE_FORMAT_TITLE, FILE_FORMAT_EXTENSION));

			File selectedFile = saveFileOptions.showSaveDialog(GuiView.getInstance().getStage());

			if (selectedFile != null) {
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
	 * Discards all saved data and starts the plot again.
	 */
	@FXML
	private void discardData() {
		if (notifyDiscardingData()) {
			RecordedResults.shutdownRecordedResultsThread();

			// Reset the plot data
			readingSeries.getData().clear();
			dataPlotPosition = 0;

			xAxis.setLowerBound(X_LOWER_BOUND);
			xAxis.setUpperBound(X_UPPER_BOUND);

			yAxis.setLowerBound(Y_LOWER_BOUND);
			yAxis.setUpperBound(Y_UPPER_BOUND);

			yUnit.clear();
			xDataCoord.setText("X: ");
			yDataCoord.setText("Y: ");

			// TODO: MAKE SURE discard and clear all data HAPPENS

			System.out.println("DATA DISCARDED");
		} else {
			System.out.println("DATA NOT DISCARDED");
		}
	}

	/**
	 * A private helper function to 'discardData'. Displays a pop-up message notifying the user that
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
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Gets the mouse coordinates relative to the chart background.
	 * 
	 * @param event
	 *            the mouse event to attach this to.
	 * @return the stored values of both coords.
	 */
	private ArrayList<Number> getMouseToChartCoords(MouseEvent event) {
		ArrayList<Number> foundCoordinates = new ArrayList<>();

		foundCoordinates.add(getMouseChartCoords(event, true)); // add x
		foundCoordinates.add(getMouseChartCoords(event, false)); // add y

		return foundCoordinates;
	}

	/**
	 * A private helper function to 'getMouseToChartCoords' that gets the x and y
	 * 
	 * @param event
	 *            the mouse event to attach this to.
	 * @param isX
	 *            whether the function should look for x or y values.
	 * @return the x or y coordinate value of the mouse.
	 */
	protected Number getMouseChartCoords(MouseEvent event, boolean isX) {
		Number returnedCoord = 0;

		// Get x coordinate
		if (isX) {
			double x = lineChart.getXAxis().sceneToLocal(event.getSceneX(), event.getSceneY())
					.getX();

			returnedCoord = lineChart.getXAxis().getValueForDisplay(x);

		} else {
			// Get y coordinate
			double y = lineChart.getYAxis().sceneToLocal(event.getSceneX(), event.getSceneY())
					.getY();

			returnedCoord = lineChart.getYAxis().getValueForDisplay(y);
		}

		return returnedCoord;
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

		// SORT IN INCREASING ORDER..
		series.getData().sort(sortChart());

		// Modified the for loop for IDing the line chart data points from:
		// https://gist.github.com/TheItachiUchiha/c0ae68ef8e6273a7ac10
		for (int i = 0; i < series.getData().size(); i++) {
			Data<Number, Number> dataPoint = series.getData().get(i);

			// Display x and y values of data points of each boundary point, and remove them when
			// leaving the node
			// TODO: CHECK IF I NEED THIS FOR THE MASK NODES
			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
					event.getDataXYValues(dataPoint, i, xDataCoord, yDataCoord));
			dataPoint.getNode().addEventFilter(MouseEvent.MOUSE_EXITED,
					event.resetDataXYValues(xDataCoord, yDataCoord));

			// Changes mouse cursor type
			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
					event.changeCursor(dataPoint));

			// Deletes the point that was clicked
			event.deleteData(dataPoint, series);

			// Moves the point that was hovered over + changes mouse cursor type
			moveData(dataPoint, series);
		}
	}

	// TODO: MAKE MORE EFFICIENT
	private boolean testOverlap(XYChart.Series<Number, Number> newSeries,
			XYChart.Series<Number, Number> existingSeries) {

		if (existingSeries.getData().size() > 1 && newSeries.getData().size() > 1) {
			for (int i = 0; i < existingSeries.getData().size() - 1; i++) {
				for (int j = 0; j < newSeries.getData().size() - 1; j++) {
					Data<Number, Number> currentNDataPoint = newSeries.getData().get(j);
					Data<Number, Number> nextNDataPoint = newSeries.getData().get(j + 1);

					Line2D checkIntersection = new Line2D();

					checkIntersection.setLine(
							new Point2D(currentNDataPoint.getXValue().floatValue(),
									currentNDataPoint.getYValue().floatValue()),
							new Point2D(nextNDataPoint.getXValue().floatValue(),
									nextNDataPoint.getYValue().floatValue()));

					Data<Number, Number> currentDataPoint = existingSeries.getData().get(i);
					Data<Number, Number> nextDataPoint = existingSeries.getData().get(i + 1);

					// Overlaps
					if (checkIntersection.intersectsLine(new Line2D(
							new Point2D(currentDataPoint.getXValue().floatValue(),
									currentDataPoint.getYValue().floatValue()),
							new Point2D(nextDataPoint.getXValue().floatValue(),
									nextDataPoint.getYValue().floatValue())))) {

						GuiView.getInstance().illegalMaskPoint();
						return false;
					}
				}
			}
		}
		return true;
	}

	//////////
	/**
	 * When the data-point is moved by the mouse, make sure the user cannot drag it into the other
	 * mask area.
	 * 
	 * @param dataPoint
	 *            the data point that is moved.
	 * @param series
	 *            the series the data-point comes from (high/low).
	 */
	protected void moveData(XYChart.Data<Number, Number> dataPoint,
			XYChart.Series<Number, Number> series) {
		highMaskBoundarySeries.getNode().setOnMouseEntered(event.testOverlap(dataPoint));

		// Keep tabs on the original data
		double originX = dataPoint.getXValue().doubleValue();
		double originY = dataPoint.getYValue().doubleValue();

		dataPoint.getNode().setOnMouseMoved(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (event.isShiftDown()) {

					// Change cursor
					dataPoint.getNode().setCursor(Cursor.HAND);

					// Change position to match the mouse coords.
					dataPoint.setXValue(getMouseChartCoords(event, true));
					dataPoint.setYValue(getMouseChartCoords(event, false));

					if (!testOverlap(lowMaskBoundarySeries, highMaskBoundarySeries)) {
						dataPoint.setXValue(originX);
						dataPoint.setYValue(originY);
					}

					// Update the x/y coordinate value display
					updateXYCoordinates(event);

					// Make sure series is sorted.
					series.getData().sort(sortChart());
					// TODO: REMOVE
					// System.out.println(series.getData().toString());
				}
			}

		});
	}

	private void updateXYCoordinates(MouseEvent event) {
		xCoordValues.setText("X: " + TIME_DECIMAL.format(getMouseChartCoords(event, true)));
		yCoordValues.setText("Y: " + MEASUREMENT_DECIMAL.format(getMouseChartCoords(event, false)));
	}

	// TODO: MAKE THIS SO THREADS CAN"T FUCK IT UP
	/**
	 * Gets the values of the mouse within the line chart graph. Modified off:
	 * http://stackoverflow.com/questions/28562195/how-to-get-mouse-position-in-chart-space. To be
	 */
	protected void createHighLowBoundaryAreas(Node chartBackground) {
		chartBackground.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				// Left mouse button and at least one of the add mask buttons
				if (event.getButton() == MouseButton.PRIMARY
						&& (isHighBtnSelected || isLowBtnSelected)) {

					restrictLineChart();

					// Gets the coordinates
					coordinates = getMouseToChartCoords(event);

					if (isHighBtnSelected) { // Set up high boundary

						// Check that no overlap before adding new points
						if (checkOverlap(coordinates, highMaskBoundarySeries, lowMaskBoundarySeries,
								highCounter)) {

							setUpBoundaries(highMaskBoundarySeries, coordinates);
							highCounter++;
						}
					} else {// Set up low boundary
						// Check that no overlap before adding new points
						if (checkOverlap(coordinates, lowMaskBoundarySeries, highMaskBoundarySeries,
								lowCounter)) {

							setUpBoundaries(lowMaskBoundarySeries, coordinates);

							lowCounter++;

							if (retry) {
								lowCounter--;
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Disables animation and ranging of y-axis and line-chart so that when selecting areas in the
	 * line chart, there is no movement.
	 */
	private void restrictLineChart() {
		lineChart.setAnimated(false);
		lineChart.getXAxis().setAutoRanging(false);
		lineChart.getYAxis().setAutoRanging(false);

		yAxis.setAnimated(false);
		yAxis.setForceZeroInRange(false);
	}

	/**
	 * Determines if the mask point to be added will not overlap over areas of the existing mask.
	 * 
	 * @param coordinates
	 *            the point to add.
	 * @param newSeries
	 *            the series the point belongs to.
	 * @param existingSeries
	 *            the mask boundary series that already exists on the chart
	 * @param counter
	 *            keeps track of how many times a point has been added.
	 * @return true if there is no overlap. false otherwise.
	 */
	private boolean checkOverlap(ArrayList<Number> coordinates,
			XYChart.Series<Number, Number> newSeries, XYChart.Series<Number, Number> existingSeries,
			int counter) {

		// Values of new points
		float tempX = coordinates.get(0).floatValue();
		float tempY = coordinates.get(1).floatValue();

		if (existingSeries.getData().size() > 0 && newSeries.getData().size() >= 1) {
			float existingX = newSeries.getData().get(counter - 1).getXValue().floatValue();
			float existingY = newSeries.getData().get(counter - 1).getYValue().floatValue();

			Line2D checkIntersection = new Line2D();
			checkIntersection.setLine(new Point2D(existingX, existingY), new Point2D(tempX, tempY));

			for (int i = 0; i < existingSeries.getData().size() - 1; i++) {

				// Points of opposite mask area
				Data<Number, Number> currentDataPoint = existingSeries.getData().get(i);
				Data<Number, Number> nextDataPoint = existingSeries.getData().get(i + 1);

				// Overlaps
				if (checkIntersection.intersectsLine(new Line2D(
						new Point2D(currentDataPoint.getXValue().floatValue(),
								currentDataPoint.getYValue().floatValue()),
						new Point2D(nextDataPoint.getXValue().floatValue(),
								nextDataPoint.getYValue().floatValue())))) {

					// Warning message
					GuiView.getInstance().illegalMaskPoint();
					return false;
				}
			}
		}
		return true;
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

			setHighBtn.setVisible(true);
			setLowBtn.setVisible(true);
			setMaskBtn.setVisible(true);
			createMaskLabel.setVisible(true);

			runMaskBtn.setVisible(true);
			System.out.println("MASK TESTING SELECTED");
		} else {
			// Not Selected
			maskTestingSelected = false;

			// hide mask testing components
			separatorLine.setVisible(false);
			importMaskBtn.setVisible(false);
			exportMaskBtn.setVisible(false);

			setHighBtn.setVisible(false);
			setLowBtn.setVisible(false);
			setMaskBtn.setVisible(false);
			createMaskLabel.setVisible(false);

			runMaskBtn.setVisible(false);
			System.out.println("MASK TESTING DE-SELECTED");
		}
	}

	/**
	 * A comparator to help determine the order of sorting the data points by increasing x-value.
	 * 
	 * @return a comparator
	 */
	private Comparator<XYChart.Data<Number, Number>> sortChart() {
		// Comparator.
		final Comparator<XYChart.Data<Number, Number>> CHART_ORDER = new Comparator<XYChart.Data<Number, Number>>() {
			public int compare(XYChart.Data<Number, Number> e1, XYChart.Data<Number, Number> e2) {

				if (e2.getXValue().doubleValue() >= e1.getXValue().doubleValue()) {
					return -1;
				} else {
					return 1;
				}
			}
		};

		return CHART_ORDER;
	}

	// FIXME: Make sure that these added points don't collide.
	/**
	 * Orders the specified series data-points by increasing x-axis values. If a first and last
	 * boundary point (i.e. x = 0, x = 50) haven't been specified, they are created.
	 * 
	 * @param series
	 *            the series to sort.
	 */
	private boolean orderAndAddBoundaryPoints(XYChart.Series<Number, Number> series,
			XYChart.Series<Number, Number> currentSeries) {

		// Determining if all good.
		boolean finalBoundarySuccess = false;
		boolean initialBoundarySuccess = false;
		boolean value = false;

		// Sort in increasing order by x values
		series.getData().sort(sortChart());

		// Add first and last points
		Data<Number, Number> initialBoundaryPoint = new Data<>();
		initialBoundaryPoint.setXValue(xAxis.getLowerBound());
		initialBoundaryPoint.setYValue(series.getData().get(0).getYValue());

		Data<Number, Number> finalBoundaryPoint = new Data<>();
		finalBoundaryPoint.setXValue(xAxis.getUpperBound());
		finalBoundaryPoint.setYValue(series.getData().get(series.getData().size() - 1).getYValue());

		double lowerXAxisBound = series.getData().get(0).getXValue().doubleValue();
		double upperXAxisBound = series.getData().get(series.getData().size() - 1).getXValue()
				.doubleValue();
		double upperBound = xAxis.getUpperBound();

		// Add initial boundary point
		if (addingBoundaryPoints(initialBoundaryPoint, lowerXAxisBound, 0.0D, currentSeries, series,
				0, 0, 1, true)) {

			initialBoundarySuccess = true;

			if (series.getName().equals("low"))
				lowCounter = series.getData().size();
		}

		// Add final boundary point
		if (addingBoundaryPoints(finalBoundaryPoint, upperXAxisBound, upperBound, currentSeries,
				series, series.getData().size(), series.getData().size() - 1,
				series.getData().size() - 2, false)) {

			finalBoundarySuccess = true;

			// Make sure the counter doesn't increase.
			if (series.getName().equals("low")) {
				lowCounter = 1;
				retry = true;
			}
		}

		// Return true if both boundary points didn't overlap.
		if (initialBoundarySuccess && finalBoundarySuccess) {
			value = true;
			retry = false;
		}

		return value;
	}

	// FIXME: Make sure that the last digits are good. since the one before the last one is not
	// around
	/**
	 * 
	 * @param xAxisBound
	 * @param start
	 * @param end
	 * @param currentSeries
	 * @param series
	 * @param position
	 * @param newPos
	 * @param finalPos
	 * @param whichWay
	 * @return
	 */
	private boolean addingBoundaryPoints(XYChart.Data<Number, Number> xAxisBound, double start,
			double end, XYChart.Series<Number, Number> currentSeries,
			XYChart.Series<Number, Number> series, int position, int newPos, int finalPos,
			boolean whichWay) {

		if (start != end) {
			// Make sure that you can't add incorrect first elements
			if ((currentSeries.getData().size() > 0)) {
				if (getMax(series.getData().get(newPos), currentSeries, whichWay)) {

					series.getData().add(position, xAxisBound);
					series.getData().get(position).getNode().setVisible(false);
				} else {

					GuiView.getInstance().illegalMaskPoint();
					return false;
				}
			} else {
				series.getData().add(position, xAxisBound);
				series.getData().get(position).getNode().setVisible(false);
			}
		} else {
			series.getData().get(newPos).setYValue(series.getData().get(finalPos).getYValue());
		}
		return true;
	}

	/**
	 * A private helper function for 'orderAndAddBoundaryPoint' which determins if there is overlap.
	 * 
	 * @param dataPointY
	 *            the boundary data point to add.
	 * @param series
	 *            the series it's comparing to.
	 * @return true if there is no 'collision'. false otherwise
	 */
	private boolean getMax(XYChart.Data<Number, Number> dataPoint,
			XYChart.Series<Number, Number> series, boolean whichWay) {
		ArrayList<XYChart.Data<Number, Number>> subList = new ArrayList<>();

		System.out.println(dataPoint.toString());

		// Only deal with points to the left of the first series data point
		for (int i = 0; i < series.getData().size(); i++) {

			if (dataPoint.getXValue().doubleValue() < series.getData().get(i).getXValue()
					.doubleValue()) {
				if (whichWay) {
					subList.addAll(series.getData().subList(0, i));
				} else {
					subList.addAll(series.getData().subList(i, series.getData().size() - 1));
				}

				break;
			}
		}

		// Check if there will be a collision
		if (subList.size() > 0) {
			for (XYChart.Data<Number, Number> d : subList) {
				if (dataPoint.getYValue().doubleValue() >= d.getYValue().doubleValue()) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Locks in the current boundary mask (high/low). If both mask boundary areas have been
	 * selected, then enable the running of the mask test to occur.
	 */
	@FXML
	private void setMaskBoundary() {
		if (isHighBtnSelected && !isLowBtnSelected
				&& (highMaskBoundarySeries.getData().size() > 0)) {
			if (orderAndAddBoundaryPoints(highMaskBoundarySeries, lowMaskBoundarySeries)) {
				event.removeAllListeners(highMaskBoundarySeries);

				setHighBtn.setDisable(true);
				isHighBtnSelected = false;
				lineChart.setHighBoundarySelected(false);

				// FIXME: Make sure this happens everywhere else.
				setLowBtn.setDisable(false);
			}

		} else if (isLowBtnSelected && !isHighBtnSelected
				&& (lowMaskBoundarySeries.getData().size() > 0)) {

			if (orderAndAddBoundaryPoints(lowMaskBoundarySeries, highMaskBoundarySeries)) {
				event.removeAllListeners(lowMaskBoundarySeries);

				setLowBtn.setDisable(true);
				isLowBtnSelected = false;
				lineChart.setLowBoundarySelected(false);
			} else {
				System.out.println("NOT YET");
			}
		}

		// Enable running of mask-testing
		if (setHighBtn.isDisabled() && setLowBtn.isDisabled()) {
			runMaskBtn.setDisable(false);
		}
	}

	/**
	 * Flags whether the current mask boundary is the higher bound.
	 */
	@FXML
	private void setHighBoundary() {
		isHighBtnSelected = true;
		isLowBtnSelected = false;

		lineChart.setLowBoundarySelected(false);
		lineChart.setHighBoundarySelected(true);

		System.out.println("high was selected");
	}

	/**
	 * Flags whether the current mask boundary is the lower bound.
	 */
	@FXML
	private void setLowBoundary() {
		isHighBtnSelected = false;
		isLowBtnSelected = true;

		lineChart.setLowBoundarySelected(true);
		lineChart.setHighBoundarySelected(false);

		System.out.println("low was selected");
	}

	// TODO: DISABLE THE SET MASK BUTTON
	/**
	 * Runs the mask-test and records whether or not it fails.
	 */
	@FXML
	private void runMaskTest() {
		System.out.println("RUN MASK TESTING");
		int counter = 0;
		int errorCounter = 0;
		
		if ((highMaskBoundarySeries.getData().size() > 0)
				&& (lowMaskBoundarySeries.getData().size() > 0)
				&& (readingSeries.getData().size() > 0)) {
			// System.out.println("THIS: " + ((highMaskBoundarySeries.getData().size() > 0) &&
			// (lowMaskBoundarySeries.getData().size() > 0)));

			for (int i = 0; i < readingSeries.getData().size() - 1; i++) {
				// Test lower
				errorCounter += maskRunOutcome(readingSeries.getData().get(i),
						readingSeries.getData().get(i + 1), lowMaskBoundarySeries);
				// Test upper
				errorCounter += maskRunOutcome(readingSeries.getData().get(i),
						readingSeries.getData().get(i + 1), highMaskBoundarySeries);
			}

			counter = checkForMaskAreaOverlap(counter);

			// Set outcome to pass or fail
			if ((errorCounter > 0) || (counter > 0)) {
				maskStatusLabel.setText("FAIL");
			} else {
				maskStatusLabel.setText("PASS");
			}
		} else {
			System.out.println("Either high/low/reading isn't loaded properly");
		}

	}

	/**
	 * A private helper function for '' which determines if any of the loaded points overlaps the
	 * mask area below the line.
	 */
	private int checkForMaskAreaOverlap(int counter) {
		for (XYChart.Data<Number, Number> dataPoint : readingSeries.getData()) {
			counter += lineChart.maskTestOverlapCheck(
					lineChart.getPolygonArray(highMaskBoundarySeries), dataPoint); // high

			counter += lineChart.maskTestOverlapCheck(
					lineChart.getPolygonArray(lowMaskBoundarySeries), dataPoint); // low
		}

		System.out.println("C: " + counter);
		return counter;
	}

	/**
	 * Checks if there have been collisions and counts them.
	 * 
	 * @param current
	 *            the current point in the series.
	 * @param next
	 *            the next point in the series.
	 * @param existingSeries
	 *            the series it's checking against.
	 */
	private int maskRunOutcome(Data<Number, Number> current, Data<Number, Number> next,
			XYChart.Series<Number, Number> existingSeries) {

		// Error tracker
		int errorCounter = 0;

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
		return errorCounter;
	}

	// FIXME: error only if yunit has a value.
	/**
	 * Loads the mask data file. If the yUnit doesn't match it errors and doesn't load.
	 */
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
			for (String[] column : GuiModel.getInstance().readMaskData(selectedFile.getPath())) {

				// yUnit.add(0, "V");

				// FIXME: do a check that ensures the loaded in data matches
				if (readingSeries.getData().size() > 0) {
					if (column[3].equals(yUnit.get(0))) {
						System.out.println("MATCHES");
						addMaskDataPoints(column);

						runMaskBtn.setDisable(false);
					} else {
						System.out.println("INVALID MASK FILE");
						errorMessageInvalidMask();
						return;
					}
				} else { // Don't need to worry about what type the mask is.. FIXME: sort this out
					addMaskDataPoints(column);
					convertMeasurementYUnit(column[3]);
					runMaskBtn.setDisable(false);
				}
			}
		}

		System.out.println(highMaskBoundarySeries.getData().size());
		System.out.println(lowMaskBoundarySeries.getData().size());
	}

	private void errorMessageInvalidMask() {
		String title = "Error! Attempting To Load Mask Incorrectly";
		String warning = "Mask loaded must have the same y-units as the current data.";
		String errorType = "modena/dialog-error.png";
		AlertType alertType = AlertType.ERROR;

		// Notify User of incorrect saving procedure.
		GuiView.getInstance().alertUser(title, warning, errorType, alertType).showAndWait();
	}

	/**
	 * A private helper function to 'importMaskData' which adds the data to the mask series.
	 * 
	 * @param column
	 */
	private void addMaskDataPoints(String[] column) {
		if (column[0].equals("high")) {
			XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(
					Double.parseDouble(column[1]), Double.parseDouble(column[2]));
			highMaskBoundarySeries.getData().add(dataPoint);
		} else {
			XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(
					Double.parseDouble(column[1]), Double.parseDouble(column[2]));
			lowMaskBoundarySeries.getData().add(dataPoint);
		}
	}

	/**
	 * Exports the current mask data to a file in .csv format.
	 */
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
				// Only need one element from yUnit saveMaskData
				GuiModel.getInstance().saveMaskData(bw, highMaskBoundarySeries, yUnit.get(0));
				GuiModel.getInstance().saveMaskData(bw, lowMaskBoundarySeries, yUnit.get(0));
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

		// Setup initial multimeter display
		String multValue = "5";
		multimeterDisplay.setText("Voltage (" + Character.toString((char) 177) + " range)" + "\n"
				+ "V: " + multValue + "V");

		runMaskBtn.setDisable(true);
		highMaskBoundarySeries.setName("high");
		lowMaskBoundarySeries.setName("low");
		readingSeries.setName("data");

		// refreshSelectablePortsList();
	}

	/**
	 * Refreshes the existing ports list to include any new ports detected.
	 */
	public static void refreshSelectablePortsList() {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					refreshSelectablePortsList();
				}
			});
			return;
		}

		SerialPort[] ports = SerialPort.getCommPorts();

		// isOpen -> whether port is closed/ready to communicate
		for (SerialPort serialPort : ports) {
			if (serialPort.isOpen()) {
				System.out.println("PORT IS OPEN");
			} else {
				System.out.println("PORT IS CLOSED: " + serialPort.getSystemPortName());
			}

			System.out.println("PORTS: " + serialPort.getSystemPortName());
		}
	}

}
