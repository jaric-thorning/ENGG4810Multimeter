package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.fazecast.jSerialComm.SerialPort;
import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.Point2D;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.ComboBox;
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
	GuiModel model = new GuiModel();
	EventHandlers event = new EventHandlers();
	Comparators compare = new Comparators();
	IsoTime startTime;
	IsoTime endTime;

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
	private volatile boolean isContinuityMode = false;

	/* Components relating to the 'connected' mode */
	@FXML
	private RadioButton connRBtn;
	@FXML
	private Button pauseBtn;
	private volatile boolean isPaused = false; // Flag for if pauseBtn has been clicked
	private volatile boolean isResumed = false; // Flag for de-clicking pauseBtn
	private volatile boolean isChanged = false; // Flag for if measurement units changed values
	@FXML
	private Button saveBtn;
	@FXML
	protected Button discardBtn;

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

	@FXML
	protected Label recordTimeLabel;

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
	@FXML
	protected TextArea maskTestResults;
	private List<Line2D> overlappedIntervals = new ArrayList<>();

	// To keep track of the previous mask boundary point
	int lowCounter = 0;

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

	// Components for shifting the line chart x-axis left and right
	@FXML
	private Button leftBtn;
	@FXML
	private Button rightBtn;

	/* Components mirroring the LED multimeter from hardware */
	@FXML
	protected Button voltageBtn;
	@FXML
	protected Button currentBtn;
	@FXML
	protected Button resistanceBtn;
	@FXML
	protected TextArea multimeterDisplay;
	@FXML
	private Label modeLabel;
	@FXML
	private Button logicBtn;
	@FXML
	private Button continuityBtn;

	// Components to switch between AC and DC
	@FXML
	private RadioButton dcRBtn;
	private boolean isDC = false;

	@FXML
	private ComboBox<String> sampleRate;
	private ObservableList<String> sampleRates = FXCollections.observableArrayList();

	/* Constants */
	private static final DecimalFormat MEASUREMENT_DECIMAL = new DecimalFormat("0.000");
	private static final DecimalFormat TIME_DECIMAL = new DecimalFormat("0.0");

	private static final String FILE_FORMAT_EXTENSION = "*.csv";
	private static final String FILE_FORMAT_TITLE = "Comma Separated Files";
	private static final String FILE_DIR = "./Saved Data/";

	private static final double X_UPPER_BOUND = 10D;
	private static final double X_LOWER_BOUND = 0D;
	private static final double Y_UPPER_BOUND = 50D;
	private static final double Y_LOWER_BOUND = -10D;

	public static double SAMPLES = 2D;
	public static double PER_TIMEFRAME = 1D; // default second

	private static final String OHM_SYMBOL = Character.toString((char) 8486);
	private static final String PLUS_MINUS_SYMBOL = Character.toString((char) 177);

	public static GuiController instance;

	// TODO: SORT
	ArrayList<Data<Number, Number>> pausedAquisitionData = new ArrayList<>();

	public GuiController() {
		// I am empty :(
		instance = this;
	}

	/**
	 * Getter method for if the intervals of the mask are overlapped TODO: COMMENT
	 * 
	 * @return
	 */
	protected List<Line2D> getOverlappedIntervals() {
		return overlappedIntervals;
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
	 * Switches the voltage and current to DC when selected and back to AC when deselected
	 */
	@FXML
	private void switchACDC() {
		if (dcRBtn.isSelected()) {
			// Change to DC
			isDC = true;
			dcMode();
		} else {
			// Change to AC
			isDC = false;
			acMode();
		}
	}

	/**
	 * A private helper function to 'switchACDC' to switch the text displayed to DC.
	 */
	private void dcMode() {
		voltageBtn.setText("V [DC]");
		currentBtn.setText("mA [DC]");

		// TODO: SEND IN DC MODE TOKEN
	}

	/**
	 * A private helper function to 'switchACDC' to switch the text displayed to back to AC.
	 */
	private void acMode() {
		voltageBtn.setText("V [AC]");
		currentBtn.setText("mA [AC]");

		// TODO: SEND IN AC MODE TOKEN
	}

	/**
	 * Enters Continuity Mode
	 */
	@FXML
	private void selectContinuityMode() {
		if (!isContinuityMode) {
			isContinuityMode = true;
			graphingResultsLabel.setText("Graphing Results - Continuity Mode");
			lineChart.setContinuityMode();

			System.out.println("I clicked on continuity mode");

			// FIXME: MODULARISE, Basically resistance mode with a difference.
			// Run thread here with 2nd column of data
			resistance = false;
			voltage = false;
			current = false;
			resistanceBtn.setDisable(true);
			voltageBtn.setDisable(true);
			currentBtn.setDisable(true);

			resetXAxis();

			// Reset the plot data
			readingSeries.getData().clear();
			yUnit.clear();

			String file = FILE_DIR + "resistance2.csv";
			RecordedResults.shutdownRecordedResultsThread();

			RecordedResults.PlaybackData container = new RecordedResults.PlaybackData(file);
			Thread thread = new Thread(container);
			RecordedResults.dataPlaybackContainer = container;
			thread.start();
		} else {
			RecordedResults.shutdownRecordedResultsThread();
			readingSeries.getData().clear();

			isContinuityMode = false;
			resistanceBtn.setDisable(false);
			voltageBtn.setDisable(false);
			currentBtn.setDisable(false);

			graphingResultsLabel.setText("Graphing Results");
			lineChart.revertContinuityMode();
		}
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
		// resetXAxis();
		// readingSeries.getData().clear();
		// yUnit.clear();

		// yAxis.setLabel("Measurements [V]");

		// Run thread here with 2nd column of data
		RecordedResults.shutdownRecordedResultsThread();

		String file = FILE_DIR + "mixedBag.csv";// "voltage.csv";
		System.out.println(file);
		RecordedResults.PlaybackData container = new RecordedResults.PlaybackData(file);
		Thread thread = new Thread(container);
		RecordedResults.dataPlaybackContainer = container;
		thread.start();
	}

	private String getVoltageRange(double dataValue) {
		if (dataValue >= -1 && dataValue <= 1) { // +- 1 range
			return "1";
		} else if (dataValue >= -5 && dataValue <= 5) { // +- 5 range
			return "5";
		} else if (dataValue >= -12 && dataValue <= 12) { // +- 12 range
			return "12";
		} else if (dataValue < -12 && dataValue > 12) { // FIXME: MAKE IT SO EVERYTHING IS LIKE THAT
			return "OL";
		} else {
			return "";
		}
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

		// yAxis.setLabel("Measurements [mA]");

		String file = FILE_DIR + "current.csv";

		// Run thread here with 2nd column of data
		RecordedResults.shutdownRecordedResultsThread();
		RecordedResults.PlaybackData container = new RecordedResults.PlaybackData(file);
		Thread thread = new Thread(container);
		RecordedResults.dataPlaybackContainer = container;
		thread.start();
	}

	private String getCurrentRange(double dataValue) {
		if (dataValue >= -10 && dataValue <= 10) { // +- 10 mA range
			return "10";
		} else if (dataValue >= -200 && dataValue <= 200) { // +- 200 mA range
			return "200";
		} else if (dataValue < -200 && dataValue > 200) { // FIXME: MAKE WHOLE MULTIMETER SET-TEXT
			return "OL";
		} else {
			return "";
		}
	}

	/**
	 * Gets dummy data of resistance values and displays it. FIXME: Convert to serial
	 */
	@FXML
	private void measureResistance() {
		System.out.println("I clicked on resistance");

		resistance = true;

		displayResistanceData();
	}

	/**
	 * A private helper function to 'measureResistance' and 'continuityMode' which displays the
	 * resistance data readings
	 */
	private void displayResistanceData() {
		voltage = false;
		current = false;

		// Reset the plot data
		resetXAxis();
		readingSeries.getData().clear();
		yUnit.clear();

		// FIXME: Do I need to change to KOhm and MOhm
		// yAxis.setLabel("Measurements [" + OHM_SYMBOL + "]");

		String file = FILE_DIR + "resistance2.csv";
		RecordedResults.shutdownRecordedResultsThread();

		// Run thread here with 2nd column of data
		RecordedResults.PlaybackData container = new RecordedResults.PlaybackData(file);
		Thread thread = new Thread(container);
		RecordedResults.dataPlaybackContainer = container;
		thread.start();
	}

	// TODO: keep this just for updating the multi-meter range stuff.
	/**
	 * Changes the y-axis label if the units change + the units' range.
	 * 
	 * @param dataValue
	 *            the y-axis value.
	 */
	private String getResistanceRange(double dataValue) {
		if (dataValue >= 0 && dataValue <= 1000) { // 0 - 1 kOhm range (1 Ohm = 0.001 kOhm)
			return "0 - 1k" + OHM_SYMBOL;
		} else if (dataValue > 1000 && dataValue <= 1000000) { // 0 - 1 MOhm range (1 kOhm = 0.001
																// MOhm)
			return "0 - 1M" + OHM_SYMBOL;
		} else if (dataValue > 1000000) { // FIXME: MAKE WHOLE MULTIMETER SET-TEXT
			return "OL";
		} else {
			return "";
		}
	}

	private String convertRange(Double dataValue) {
		if (dataValue < 1000) {
			String kOhm = "k" + OHM_SYMBOL;
			// yAxis.setLabel("Measurements [" + OHM_SYMBOL + "]");
			return kOhm + ": " + (dataValue /= 1000).toString() + kOhm;
			// } else if (dataValue >= 1000 && dataValue <= 1000000) {
			// yAxis.setLabel("Measurements [" + "k" + OHM_SYMBOL + "]");
			// return (dataValue /= 1000);
		} else if (dataValue >= 1000 && dataValue <= 1000000) {
			String MOhm = "M" + OHM_SYMBOL;
			// yAxis.setLabel("Measurements [" + "M" + OHM_SYMBOL + "]");
			return MOhm + ": " + (dataValue /= 1000000).toString() + MOhm;
		} else {
			return "";
		}
	}

	/**
	 * Provide the list of the required configurable sample rates.
	 */
	private void initialiseSampleRate() {
		sampleRates.add("2 meas. per sec");
		sampleRates.add("1 meas. per sec");
		sampleRates.add("1 meas. every 2 secs");
		sampleRates.add("1 meas. every 5 secs");
		sampleRates.add("1 meas. every 10 secs");
		sampleRates.add("1 meas. every min");
		sampleRates.add("1 meas. every 2 mins");
		sampleRates.add("1 meas. every 5 mins");
		sampleRates.add("1 meas. every 10 mins");

		sampleRate.setItems(sampleRates);
		sampleRate.setStyle("-fx-font: 11px \"System\";");
	}

	@FXML
	private void selectSampleRate() {
		if (sampleRate.getSelectionModel().getSelectedItem().contains("2 meas.per")) {
			SAMPLES = 2;
			PER_TIMEFRAME = 1;
		} else if (sampleRate.getSelectionModel().getSelectedItem().contains("1 meas. per")) {
			SAMPLES = 1;
			PER_TIMEFRAME = 1;
		} else if (sampleRate.getSelectionModel().getSelectedItem().contains("2 secs")) {
			SAMPLES = 1;
			PER_TIMEFRAME = 2;
		} else if (sampleRate.getSelectionModel().getSelectedItem().contains("5 secs")) {
			SAMPLES = 1;
			PER_TIMEFRAME = 5;
		} else if (sampleRate.getSelectionModel().getSelectedItem().contains("10 secs")) {
			SAMPLES = 1;
			PER_TIMEFRAME = 10;
		} else if (sampleRate.getSelectionModel().getSelectedItem().contains("every min")) {
			SAMPLES = 1;
			PER_TIMEFRAME = 1 * 60; // 60 seconds are a minute
		} else if (sampleRate.getSelectionModel().getSelectedItem().contains("2 mins")) {
			SAMPLES = 1;
			PER_TIMEFRAME = 2 * 60;
		} else if (sampleRate.getSelectionModel().getSelectedItem().contains("5 mins")) {
			SAMPLES = 1;
			PER_TIMEFRAME = 5 * 60;
		} else if (sampleRate.getSelectionModel().getSelectedItem().contains("10 mins")) {
			SAMPLES = 1;
			PER_TIMEFRAME = 10 * 60;
		}

		System.out.println("S: " + SAMPLES + ", PT: " + PER_TIMEFRAME);
	}

	/**
	 * Updates the displayed dummy data FIXME: convert to serial.
	 * 
	 * @param multimeterReading
	 */
	public void recordAndDisplayDummyData(double multimeterReading, String unit) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					recordAndDisplayDummyData(multimeterReading, unit);
				}
			});
			return;
		}

		// Update all software displays
		updateDisplay(multimeterReading, unit);
	}

	/**
	 * A private helper function for displaying dummy data on chart. FIXME: convert to serial.
	 * 
	 * @param multimeterReading
	 */
	private void updateDisplay(Double multimeterReading, String unit) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					updateDisplay(multimeterReading, unit);
				}
			});
			return;
		}

		// Update y-axis unit value.
		yUnit.add(unit);

		// modify graph if units have changed.
		validateYAxisUnit(unit);

		// Change multimeter text display according to ranges and values.
		updateYAxisLabel(multimeterReading, unit);
		
		// sort out isoTime
		establishIsoTime();

		if (!isPaused) {

			// Add the acquired data to the reading series after it's been paused
			addAllAcquiredDataAfterPaused();

			// Display dummy data plot
			readingSeries.getData().add(new XYChart.Data<Number, Number>(
					dataPlotPosition / (SAMPLES / PER_TIMEFRAME), multimeterReading));

			// Add listener. TODO: check we need this behaviour in connected mode
			System.out.println("	ST: " + startTime.toString());
			
			readingSeries.getData().get(dataPlotPosition).getNode().addEventHandler(
					MouseEvent.MOUSE_ENTERED,
					event.getDataXYValues(readingSeries.getData().get(dataPlotPosition),
							dataPlotPosition, xDataCoord, yDataCoord, startTime, endTime));

			// Update chart
			int dataBoundsRange = (int) Math.ceil(dataPlotPosition / (SAMPLES / PER_TIMEFRAME));
			if (dataBoundsRange > X_UPPER_BOUND) {
				xAxis.setLowerBound(dataBoundsRange - X_UPPER_BOUND);
				xAxis.setUpperBound(dataBoundsRange);
			}
		} else { // User selected the pause button.

			// Redirect serial values to a temporary list.
			pausedAquisitionData.add(new XYChart.Data<Number, Number>(
					dataPlotPosition / (SAMPLES / PER_TIMEFRAME), multimeterReading));
		}

		dataPlotPosition++;
	}

	/**
	 * Calculates the ISO 8601 time interval for the first and last point.
	 */
	private void establishIsoTime() {
		if (dataPlotPosition == 0) { // Get the start time
			LocalDateTime local = LocalDateTime.now();
			startTime = new IsoTime(local, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			endTime = startTime;
			System.out.println("StartL:" + local);
		} else if (dataPlotPosition == readingSeries.getData().size()) { // - 1
			LocalDateTime local = LocalDateTime.now(); // Get the end time
			endTime = new IsoTime(local, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			//System.out.println("EndL: " + local);
		}
		recordTimeLabel.setText(startTime + "/" + endTime);
	}

	/**
	 * A private helper function to 'updateDisplay' which modifies the plot after it has been
	 * resumed from a paused state.
	 */
	private void addAllAcquiredDataAfterPaused() {
		if (isResumed) {
			if (isChanged) { // Measurements have changed at some point
				readingSeries.getData().clear();
				resetXAxis();
			}

			// Add all acquired data to the display
			readingSeries.getData().addAll(pausedAquisitionData);

			// Add listener to add aquired data when it was paused.
			for (int i = 0; i < readingSeries.getData().size(); i++) {
				// FIXME
				readingSeries.getData().get(i).getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
						event.getDataXYValues(readingSeries.getData().get(i), i, xDataCoord,
								yDataCoord, startTime, endTime));
			}

			isResumed = false;
		}
	}

	/**
	 * A private helper function to 'updateDisplay' which modifies the plot if the measurement units
	 * are modified during data acquisition.
	 * 
	 * @param unit
	 *            the y-axis measurement unit.
	 */
	private void validateYAxisUnit(String unit) {
		// Set y-unit measurement flags and modify plot
		if (unit.equals("V") && !voltage) {

			voltage = true;
			current = false;
			resistance = false;

			modifyPlotParts();
		} else if (unit.equals("mA") && !current) {
			System.out.println("mA");
			voltage = false;
			current = true;
			resistance = false;

			modifyPlotParts();
		} else if (unit.equals("Ohm") && !resistance) {

			voltage = false;
			current = false;
			resistance = true;

			modifyPlotParts();
		}
	}

	/**
	 * A private helper function to 'validateYAxisUnit' which does the modifying of the plot and
	 * sets certain flags.
	 */
	private void modifyPlotParts() {
		isChanged = true; // y-unit measurement has changed

		// Reset the plot data
		dataPlotPosition = 0;

		if (isPaused) { // Only clear acquiring data, not displayed
			pausedAquisitionData.clear();
			establishIsoTime();
		} else { // Clear displayed data
			resetXAxis();
			yUnit.clear();
			readingSeries.getData().clear();
			pausedAquisitionData.clear();
		}
	}

	// FIXME: does this need to be called everytime.
	/**
	 * A private helper function to 'updateDisplay' which updates the multimeter text display to
	 * match the data coming through the serial channel.
	 * 
	 * @param multimeterReading
	 *            the area which displays the multimeter readings
	 * @param unit
	 *            the unit of the y-axis values.
	 */
	private void updateYAxisLabel(Double multimeterReading, String unit) {
		if (!isPaused) {
			if (voltage) {
				multimeterDisplay.setText(getUnit(unit) + " ( " + PLUS_MINUS_SYMBOL
						+ getVoltageRange(multimeterReading) + " )" + "\n" + unit + ": "
						+ multimeterReading.toString() + unit);

				yAxis.setLabel("Measurements [V]");
			} else if (current) {
				multimeterDisplay.setText(getUnit(unit) + " ( " + PLUS_MINUS_SYMBOL
						+ getCurrentRange(multimeterReading) + " )" + "\n" + unit + ": "
						+ multimeterReading.toString() + unit);

				yAxis.setLabel("Measurements [mA]");
			} else if (resistance) {
				multimeterDisplay
						.setText(getUnit(unit) + " ( " + getResistanceRange(multimeterReading)
								+ " )" + "\n" + convertRange(multimeterReading));

				yAxis.setLabel("Measurements [" + OHM_SYMBOL + "]");
			}
		}
	}

	// FIXME: other values??
	/**
	 * A private helper function to 'updateYAxiesLabel' which modifies text for displaying the
	 * multimeter values.
	 * 
	 * @param unit
	 *            the abbreviated forms of voltage, current and resistance.
	 * @return an extended string version of the unit.
	 */
	private String getUnit(String unit) {
		if (unit.equals("V")) {
			return "Voltage";
		} else if (unit.equals("mA")) { // need to convert to milliamps
			return "milliAmp";
		} else if (unit.equals("Ohm")) {
			return "Ohm";
		} else {
			return "";
		}
	}

	/**
	 * If the units of the y-values change, then reset the axes bounds.
	 */
	private void resetXAxis() {
		if (voltage || current || resistance || isContinuityMode) {
			// dataPlotPosition = 0;
			xAxis.setLowerBound(X_LOWER_BOUND);
			xAxis.setUpperBound(X_UPPER_BOUND);
		}
	}

	// // TODO: change the data values to reflect where it should be.
	// /**
	// * Changes the y-axis label if the units change + the units' range.
	// *
	// * @param dataValue
	// * the y-axis value.
	// */
	// private void autoRangeYUnit(Double dataValue) {
	// String ohmSymbol = Character.toString((char) 8486);
	//
	// if (voltage) {
	// yUnit.add("V");
	// } else if (current) {
	// yUnit.add("mA");
	// } else if (resistance) {
	// yUnit.add("Ohm");
	// if (dataValue < 1000) {
	// yAxis.setLabel("Measurements [" + ohmSymbol + "]");
	// } else if (dataValue >= 1000 && dataValue < 1000000) {
	// yAxis.setLabel("Measurements [" + "k" + ohmSymbol + "]");
	// } else if (dataValue >= 1000000) {
	// yAxis.setLabel("Measurements [" + "M" + ohmSymbol + "]");
	// }
	// }
	//
	// }

	/**
	 * Selects the connected mode of the GUI if there is a connection, otherwise it's disabled.
	 */
	@FXML
	private void selectConnected() {

		// If there a connection and the radio button is selected
		if (connRBtn.isSelected() && testConnection(connRBtn)) {
			System.out.println("CONNECTED MODE INITIATED");
			System.out.println("//-------------------//");
			yAxis.setAutoRanging(true);

			setupConnectedComponents();

		} else if (!testConnection(connRBtn)) {
			System.out.println("There is no test connection");

			disconnRBtn.setDisable(false);
		} else { // Assuming 'else' just covers when radio button is not selected. TODO: check.

			if (notifyUserConnected()) {
				System.out.println("CONNECTED MODE EXITED");

				disconnRBtn.setDisable(false);

				revertConnectedComponents();

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
		sampleRate.setDisable(false);
		pauseBtn.setDisable(false);
		saveBtn.setDisable(false);
		discardBtn.setDisable(false);

		// Enable digital multimeter components
		// FIXME: enable only if there is a two way connection
		multimeterDisplay.setDisable(false);
		voltageBtn.setDisable(false);
		currentBtn.setDisable(false);
		resistanceBtn.setDisable(false);
		dcRBtn.setDisable(false);
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
	private void revertConnectedComponents() {

		// Disable digital multimeter components
		multimeterDisplay.setDisable(true);
		multimeterDisplay.setText("");
		voltageBtn.setDisable(true);
		currentBtn.setDisable(true);
		resistanceBtn.setDisable(true);
		dcRBtn.setDisable(true);
		multimeterDisplay.setDisable(true);
		modeLabel.setDisable(true);
		logicBtn.setDisable(true);
		continuityBtn.setDisable(true);

		// Disable connected components
		sampleRate.setDisable(true);
		// TODO: Check that this is needed to be reset.
		// sampleRate.setValue(sampleRate.getPromptText());
		// SAMPLES = 2D;
		// PER_TIMEFRAME = 1D;

		pauseBtn.setDisable(true);
		isPaused = false;
		isResumed = false;
		isChanged = false;
		pausedAquisitionData = new ArrayList<>();

		pauseBtn.setText("Pause");

		saveBtn.setDisable(true);
		discardBtn.setDisable(true);

		dcRBtn.setSelected(false);
		isDC = false;

		resistance = false;
		voltage = false;
		current = false;
		isContinuityMode = false;
		graphingResultsLabel.setText("Graphing Results");

		// Clear all data related things.
		// TODO: Close open connections.
		RecordedResults.shutdownRecordedResultsThread();

		// Reset the plot data
		readingSeries.getData().clear();

		dataPlotPosition = 0;
		resetAxes();
		yUnit.clear();

		// No listener apparently. TODO: ATTACH LISTENER
		xDataCoord.setText("X: ");
		yDataCoord.setText("Y: ");
		recordTimeLabel.setText("");
	}

	/**
	 * Selects the disconnected mode of the GUI.
	 */
	@FXML
	private void selectDisconnected() {
		if (disconnRBtn.isSelected()) {
			System.out.println("DISCONNECTED MODE INITIATED");
			System.out.println("//-------------------//");

			// Disable the connected mode from being editable during disconnected mode
			connRBtn.setDisable(true);

			// Enable components
			loadSavedData.setDisable(false);
			loadFileLabel.setDisable(false);
			maskTestingBtn.setDisable(false);

			importMaskBtn.setDisable(false);
			setHighBtn.setDisable(false);
			setMaskBtn.setDisable(false);
		} else {
			if (notifyUserDisconnected()) {
				System.out.println("DISCONNECTED MODE EXITED");

				resetAxes(); // Reset the x and y axis bounds

				connRBtn.setDisable(false); // Enable other radio button

				loadSavedData.setDisable(true);
				loadFileLabel.setDisable(true);
				maskTestingBtn.setDisable(true);

				// Hide mask testing components
				separatorLine.setVisible(false);
				importMaskBtn.setVisible(false);
				exportMaskBtn.setVisible(false);
				runMaskBtn.setVisible(false);
				maskTestResults.setVisible(false);
				setHighBtn.setVisible(false);
				setLowBtn.setVisible(false);
				setMaskBtn.setVisible(false);
				createMaskLabel.setVisible(false);

				// Disable what needs to be disabled & reset
				maskTestingSelected = false;
				isHighBtnSelected = false;
				isLowBtnSelected = false;
				lineChart.setHighBoundarySelected(false);
				lineChart.setLowBoundarySelected(false);

				lowCounter = 0;
				maskStatusLabel.setText("FAIL");
				maskTestResults.clear();

				runMaskBtn.setDisable(true);
				maskTestResults.setDisable(true);
				exportMaskBtn.setDisable(true);
				setLowBtn.setDisable(true);

				highMaskBoundarySeries.getData().clear();
				lowMaskBoundarySeries.getData().clear();
				readingSeries.getData().clear();
				overlappedIntervals.clear();
				yUnit.clear();
			} else {
				System.out.println("DISCONNECTED MODE STAYING");

				disconnRBtn.setSelected(true);
			}
		}
	}

	/**
	 * Resets the axes to their original upper and lower boundaries.
	 */
	private void resetAxes() {
		xAxis.setLowerBound(X_LOWER_BOUND);
		xAxis.setUpperBound(X_UPPER_BOUND);

		yAxis.setLowerBound(Y_LOWER_BOUND);
		yAxis.setUpperBound(Y_UPPER_BOUND);
		yAxis.setLabel("Measurements");
		yAxis.setAutoRanging(true);

		// Also reset the displayed chart coord values.
		xCoordValues.setText("X: ");
		yCoordValues.setText("Y: ");
		recordTimeLabel.setText("");
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
			System.out.println("DATA IS PAUSED");

			isPaused = true;
			pauseBtn.setText("Unpause");

			// Enable digital multimeter components
			multimeterDisplay.setDisable(true);
			voltageBtn.setDisable(true);
			currentBtn.setDisable(true);
			resistanceBtn.setDisable(true);
			dcRBtn.setDisable(true);
			multimeterDisplay.setDisable(true);
			modeLabel.setDisable(true);
			logicBtn.setDisable(true);
			continuityBtn.setDisable(true);

			// TODO: data aquisition
			isResumed = true;
			pausedAquisitionData = new ArrayList<>();

			// RecordedResults.pauseRecordedResultsThread(true);

		} else {// TODO: check for any yUnit changes
			isPaused = false;
			pauseBtn.setText("Pause");
			// No justPaused here, because want to make it stop after it's loaded everything

			// Enable digital multimeter components
			multimeterDisplay.setDisable(false);
			voltageBtn.setDisable(false);
			currentBtn.setDisable(false);
			resistanceBtn.setDisable(false);
			dcRBtn.setDisable(false);
			multimeterDisplay.setDisable(false);
			modeLabel.setDisable(false);
			logicBtn.setDisable(false);
			continuityBtn.setDisable(false);

			// RecordedResults.pauseRecordedResultsThread(false);
			System.out.println("DATA IS UNPAUSED");
		}
	}

	/**
	 * Loads the saved recorded input data.
	 */
	@FXML
	private void loadFile() {

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

			// Clear data from list if reloaded multiple times
			readingSeries.getData().clear();
			yUnit.clear();
			yAxis.setLabel("Measurements");

			// Set up new array
			ArrayList<Double> inputDataXValues = new ArrayList<>();
			ArrayList<Double> inputDataYValues = new ArrayList<>();
			ArrayList<String> inputDataYUnits = new ArrayList<>();

			// Read from file
			readDataFromFile(selectedFile, inputDataXValues, inputDataYValues, inputDataYUnits);

			// Modify y-units
			yUnit.addAll(inputDataYUnits);
			if (yUnit.size() > 0) {
				convertMeasurementYUnit(yUnit.get(0));
			}

			// FIXME: make sure autoranging is set true/false in right places
			// MAKE SURE THAT THERE ARE ACTUALLY DATA TO BE HAD
			yAxis.setAutoRanging(true);
			addDataToSeries(inputDataXValues, inputDataYValues);
		} else {
			System.out.println("YO");
		}
	}

	/**
	 * A private helper function for 'loadFile' which reads in data from a given file.
	 * 
	 * @param selectedFile
	 *            file to read data from.
	 * @param inputDataXValues
	 *            temporary placeholder for x values.
	 * @param inputDataYValues
	 *            temporary placeholder for y values.
	 */
	private void readDataFromFile(File selectedFile, ArrayList<Double> inputDataXValues,
			ArrayList<Double> inputDataYValues, ArrayList<String> inputDataYUnits) {

		// Read in x-values
		for (String s : model.readColumnData(selectedFile.getPath(), 0)) {
			inputDataXValues.add(Double.parseDouble(s));
		}

		// Read in y-values
		for (String s : model.readColumnData(selectedFile.getPath(), 1)) {
			inputDataYValues.add(Double.parseDouble(s));
		}

		// Read in y-unit (perform any necessary conversions)
		for (String s : model.readColumnData(selectedFile.getPath(), 2)) {
			inputDataYUnits.add(s);
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
		System.out.println("Displayed Unit: " + displayedYUnit);

		// Convert Ohm to Ohm symbol.
		if (value.equals("Ohm")) {
			displayedYUnit = OHM_SYMBOL;
		}

		yAxis.setLabel("Measurements [" + displayedYUnit + "]");
	}

	/**
	 * A private helper function for 'loadFile' which adds the x and y values to the line chart
	 * series.
	 * 
	 * @param inputDataXValues
	 *            holds all the x values.
	 * @param inputDataYValues
	 *            holds all the y values.
	 */
	private void addDataToSeries(ArrayList<Double> inputDataXValues,
			ArrayList<Double> inputDataYValues) {

		double firstPointXValue = inputDataXValues.get(0);

		// Add data to series
		for (int i = 0; i < inputDataXValues.size(); i++) {
			double inputXDataValue = inputDataXValues.get(i);
			double inputYDataValue = inputDataYValues.get(i);

			readingSeries.getData()
					.add(new XYChart.Data<Number, Number>(inputXDataValue, inputYDataValue));

			// Assign indexing to each node.
			Data<Number, Number> dataPoint = readingSeries.getData().get(i);

			// TODO: save the start and end time into the file, to load in
			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED, event
					.getDataXYValues(dataPoint, i, xDataCoord, yDataCoord, startTime, endTime));

			dataPoint.getNode().addEventFilter(MouseEvent.MOUSE_EXITED,
					event.resetDataXYValues(xDataCoord, yDataCoord));

			// Update chart bounds if line chart exceeds them.
			int dataBoundsRange = (int) Math.ceil(i / SAMPLES);
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
					model.saveColumnData(bw, readingSeries, yUnit);
				} catch (IOException e) {
					e.printStackTrace();
				}

				System.out.println("Saving Data");
			}
		}
	}

	// FIXME: make sure that everything gets reset.
	/**
	 * Discards all saved data and starts the plot again.
	 */
	@FXML
	private void discardData() {
		if (notifyDiscardingData()) {
			// Reset the plot data
			revert();

			readingSeries.getData().clear();
			System.out.println("DATA DISCARDED");
		} else {
			System.out.println("DATA NOT DISCARDED");
		}
	}

	// FIXME: MODULARSE THIS WITH REVERTCONNECTEDCOMPONENTS
	private void revert() {

		// TODO: Close open connections.
		RecordedResults.shutdownRecordedResultsThread();

		// FIXME: not sure if I control these
		// -------------------------
		dcRBtn.setSelected(false);
		isDC = false;
		// -------------------------

		// Clear all things
		multimeterDisplay.setText("");

		isPaused = false;
		isResumed = false;
		isChanged = false;
		pausedAquisitionData = new ArrayList<>();
		pauseBtn.setText("Pause");

		resistance = false;
		voltage = false;
		current = false;
		isContinuityMode = false;
		graphingResultsLabel.setText("Graphing Results");

		// Reset the plot data
		readingSeries.getData().clear();
		dataPlotPosition = 0;
		resetAxes();
		yUnit.clear();

		// TODO: make sure that I can remove this, add listener to the data.
		xDataCoord.setText("X: ");
		yDataCoord.setText("Y: ");
		recordTimeLabel.setText("");
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
	 * @param mouseEvent
	 *            the mouse event to attach this to.
	 * @return the stored values of both coords.
	 */
	private ArrayList<Number> getMouseToChartCoords(MouseEvent mouseEvent) {
		ArrayList<Number> foundCoordinates = new ArrayList<>();

		foundCoordinates.add(getMouseChartCoords(mouseEvent, true)); // add x
		foundCoordinates.add(getMouseChartCoords(mouseEvent, false)); // add y

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
	private void setUpBoundaries(XYChart.Series<Number, Number> series, Number coordX,
			Number coordY) {

		// Add data to specified series.
		series.getData().add(new XYChart.Data<Number, Number>(coordX, coordY));

		// Sort data by x-axis value
		series.getData().sort(compare.sortChart());

		// Modified the for loop for IDing the line chart data points from:
		// https://gist.github.com/TheItachiUchiha/c0ae68ef8e6273a7ac10
		for (int i = 0; i < series.getData().size(); i++) {
			Data<Number, Number> dataPoint = series.getData().get(i);

			// Changes mouse cursor type
			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
					event.changeCursor(dataPoint));

			// Deletes the point that was clicked
			event.deleteData(dataPoint, series);

			// Moves the point that was hovered over + changes mouse cursor type
			moveData(dataPoint, series);
		}
	}

	/**
	 * A private function helper for 'addMaskDataPoints' & 'moveData' which checks if overlap
	 * between the two mask boundaries has occurred.
	 * 
	 * @param newSeries
	 *            the mask boundary which has not been set yet. (low mask boundary)
	 * @param existingSeries
	 *            the mask boundary which has already been set. (high mask boundary)
	 * @return true if there is no overlap, false otherwise.
	 */
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
					series.getData().sort(compare.sortChart());
				}
			}

		});
	}

	/**
	 * A private helper function for 'moveData' which updates the displayed x and y positions of the
	 * cursor within the line-chart space.
	 * 
	 * @param mouseEvent
	 *            the mouse event to attach this to.
	 */
	private void updateXYCoordinates(MouseEvent mouseEvent) {
		xCoordValues.setText("X: " + TIME_DECIMAL.format(getMouseChartCoords(mouseEvent, true)));
		yCoordValues.setText(
				"Y: " + MEASUREMENT_DECIMAL.format(getMouseChartCoords(mouseEvent, false)));
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
					Number coordX = getMouseToChartCoords(event).get(0);
					Number coordY = getMouseToChartCoords(event).get(1);

					if (isHighBtnSelected) { // Set up high boundary
						// No need to check if it overlaps, as lower bound is compared to it.
						setUpBoundaries(highMaskBoundarySeries, coordX, coordY);
					} else {// Set up low boundary
						// Check that no overlap before adding new points
						if (checkOverlap(coordX, coordY, lowCounter)) {

							setUpBoundaries(lowMaskBoundarySeries, coordX, coordY);

							lowCounter++;

							System.out.println("LC: " + lowCounter);
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

		yAxis.setAutoRanging(false);
	}

	/**
	 * Determines if the mask point to be added to the lower mask boundary will not overlap over
	 * areas of the high mask boundary area.
	 * 
	 * @param coordX
	 *            the x-value of the point to be added.
	 * @param coordY
	 *            the v-value of the point to be added.
	 * @param counter
	 *            keeps track of where the new point is (before/after existing point)
	 * @return true if there is no overlap. false otherwise.
	 */
	private boolean checkOverlap(Number coordX, Number coordY, int counter) {

		// Values of new points
		float tempX = coordX.floatValue();
		float tempY = coordY.floatValue();

		if (lowMaskBoundarySeries.getData().size() > 0) {
			// ISSUE WITH ASSIGN EXISTING XVALUE
			System.out.println("SUCK ZERO: " + counter);
			System.out.println("SUCK ONE: "
					+ lowMaskBoundarySeries.getData().get(counter - 1).getXValue().floatValue());
			System.out.println("SUCK TWO: " + tempX);
			ArrayList<Float> existingValues = assignExistingXValue(lowMaskBoundarySeries, tempX,
					counter,
					lowMaskBoundarySeries.getData().get(counter - 1).getXValue().floatValue());

			float existingX = existingValues.get(0);
			float existingY = existingValues.get(1);

			Line2D lowBoundaryLineSegment = new Line2D();
			lowBoundaryLineSegment.setLine(new Point2D(existingX, existingY),
					new Point2D(tempX, tempY));

			System.out.println(highMaskBoundarySeries.getData().toString());
			return checkLineIntersection(lowBoundaryLineSegment, highMaskBoundarySeries);
		}

		return true;
	}

	/**
	 * A private helper function to 'checkOverlap' that determines if the point to be added would
	 * cause an overlap if added.
	 * 
	 * @param lowBoundaryLineSegment
	 *            the line segment to compare against.
	 * @return true if no collision, false otherwise.
	 */
	private boolean checkLineIntersection(Line2D lowBoundaryLineSegment,
			XYChart.Series<Number, Number> highMaskBoundarySeries) {
		for (int i = 0; i < highMaskBoundarySeries.getData().size() - 1; i++) {

			// Points of opposite mask area
			Data<Number, Number> currentDataPoint = highMaskBoundarySeries.getData().get(i);
			Data<Number, Number> nextDataPoint = highMaskBoundarySeries.getData().get(i + 1);

			Line2D test = new Line2D(
					new Point2D(currentDataPoint.getXValue().floatValue(),
							currentDataPoint.getYValue().floatValue()),
					new Point2D(nextDataPoint.getXValue().floatValue(),
							nextDataPoint.getYValue().floatValue()));

			// Overlaps
			if (lowBoundaryLineSegment.intersectsLine(test)) {

				// Warning message
				GuiView.getInstance().illegalMaskPoint();
				return false;
			}
		}

		return true;
	}

	/**
	 * A private helper function to 'checkOverlap' which determines which direction the line will be
	 * in (right to left, or left to right).
	 * 
	 * @param series
	 *            the low boundary series
	 * @param tempX
	 *            the x-value of the point to be added
	 * @param counter
	 *            where in the low boundary series the point is
	 * @param compareX
	 *            the x-value of an existing point
	 * @return a list that has the x and y value that needs to be compared
	 */
	private ArrayList<Float> assignExistingXValue(XYChart.Series<Number, Number> series,
			float tempX, int counter, float compareX) {
		ArrayList<Float> tempList = new ArrayList<>();

		for (int i = 0; i < series.getData().size() - 1; i++) {
			float currentX = series.getData().get(i).getXValue().floatValue();
			float currentY = series.getData().get(i).getYValue().floatValue();
			float nextX = series.getData().get(i + 1).getXValue().floatValue();

			// Add point between two points
			if ((tempX > currentX) && (tempX < nextX)) {
				System.out.println("in between: " + (i + 1) + ", " + (i + 2));
				tempList.add(currentX);
				tempList.add(currentY);
				return tempList;
			}
		}

		// Add point to the direct left (start of the list)
		if (tempX < compareX) {
			tempList.add(series.getData().get(0).getXValue().floatValue());
			tempList.add(series.getData().get(0).getYValue().floatValue());
		} else { // Add point to the direct right (end of the list)
			tempList.add(series.getData().get(counter - 1).getXValue().floatValue());
			tempList.add(series.getData().get(counter - 1).getYValue().floatValue());
		}

		return tempList;
	}

	/**
	 * Displays mask-testing options
	 */
	@FXML
	private void editMask() {

		// Selected
		if (!maskTestingSelected) {
			maskTestingSelected = true;

			// Show mask testing components
			separatorLine.setVisible(true);
			importMaskBtn.setVisible(true);
			exportMaskBtn.setVisible(true);

			setHighBtn.setVisible(true);
			setLowBtn.setVisible(true);
			setMaskBtn.setVisible(true);
			createMaskLabel.setVisible(true);

			runMaskBtn.setVisible(true);
			maskTestResults.setVisible(true);

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
			maskTestResults.setVisible(false);
			System.out.println("MASK TESTING DE-SELECTED");
		}
	}

	/**
	 * Orders the specified series data-points by increasing x-axis values. If a first and last
	 * boundary point (i.e. x = 0, x = 50) haven't been specified, they are created.
	 * 
	 * @param existingSeries
	 *            the series to sort.
	 * @param currentSeries
	 * @return
	 */
	private boolean orderAndAddBoundaryPoints(XYChart.Series<Number, Number> existingSeries,
			XYChart.Series<Number, Number> currentSeries) {

		// Determining if all good.
		boolean finalBoundarySuccess = false;
		boolean initialBoundarySuccess = false;
		boolean value = false;

		// Sort in increasing order by x values
		existingSeries.getData().sort(compare.sortChart());

		// Add first and last points
		Data<Number, Number> initialBoundaryPoint = new Data<>();
		initialBoundaryPoint.setXValue(0);
		initialBoundaryPoint.setYValue(existingSeries.getData().get(0).getYValue());

		Data<Number, Number> finalBoundaryPoint = new Data<>();
		finalBoundaryPoint.setXValue(xAxis.getUpperBound());
		finalBoundaryPoint.setYValue(
				existingSeries.getData().get(existingSeries.getData().size() - 1).getYValue());

		double lowerXAxisBound = existingSeries.getData().get(0).getXValue().doubleValue();
		double upperXAxisBound = existingSeries.getData().get(existingSeries.getData().size() - 1)
				.getXValue().doubleValue();
		double upperBound = xAxis.getUpperBound();

		// Add initial boundary point
		if (addingBoundaryPoints(initialBoundaryPoint, lowerXAxisBound, 0.0D, currentSeries,
				existingSeries, 0, 0, 1, true)) {

			initialBoundarySuccess = true;

			if (existingSeries.getName().contains("low"))
				lowCounter = existingSeries.getData().size();
		}

		// Add final boundary point
		if (addingBoundaryPoints(finalBoundaryPoint, upperXAxisBound, upperBound, currentSeries,
				existingSeries, existingSeries.getData().size(),
				existingSeries.getData().size() - 1, existingSeries.getData().size() - 2, false)) {

			finalBoundarySuccess = true;

			// Make sure the counter doesn't increase.
			if (existingSeries.getName().contains("low")) {
				lowCounter = 1;
			}
		}

		// Return true if both boundary points didn't overlap.
		if (initialBoundarySuccess && finalBoundarySuccess) {
			value = true;
		}

		return value;
	}

	// TODO: ADD COMMENTS, ITS TOO CONFUSING
	/**
	 * A private helper function to 'orderAndAddBoundaryPoints' which adds the first and last
	 * boundary points (x lower and x upper) to the mask series if they do not overlap when set.
	 * 
	 * @param boundaryPoint
	 *            the absolute first or last boundary point to be added (0 or x-axis upperbound)
	 * @param start
	 * @param end
	 * @param currentSeries
	 * @param exisistingSeries
	 * @param position
	 * @param newPos
	 * @param finalPos
	 * @param whichWay
	 * @return true if there is no overlap, false if the added point will overlap with existing mask
	 */
	private boolean addingBoundaryPoints(XYChart.Data<Number, Number> boundaryPoint, double start,
			double end, XYChart.Series<Number, Number> currentSeries,
			XYChart.Series<Number, Number> exisistingSeries, int position, int newPos, int finalPos,
			boolean whichWay) {

		if (start != end) {
			// Make sure that you can't add incorrect first elements
			if ((currentSeries.getData().size() > 0)) {
				if (getMax(exisistingSeries.getData().get(newPos), currentSeries, whichWay)) {

					exisistingSeries.getData().add(position, boundaryPoint);
					exisistingSeries.getData().get(position).getNode().setVisible(false);
				} else {

					GuiView.getInstance().illegalMaskPoint();
					return false;
				}
			} else {
				exisistingSeries.getData().add(position, boundaryPoint);
				exisistingSeries.getData().get(position).getNode().setVisible(false);
			}
		} else {
			exisistingSeries.getData().get(newPos)
					.setYValue(exisistingSeries.getData().get(finalPos).getYValue());
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
			maskTestResults.setDisable(false);
			exportMaskBtn.setDisable(false);
			setMaskBtn.setDisable(true);
		}
	}

	/**
	 * Flags whether the current mask boundary is the higher bound.
	 */
	@FXML
	private void setHighBoundary() {
		importMaskBtn.setDisable(true);

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
				maskTestResults.setText("------------------------------" + "\n");
				maskTestResults.appendText("TEST FAILED OVER INTERVALS: " + "\n");

				// Display failed intervals
				displayFailedIntervals();
			} else {
				maskStatusLabel.setText("PASS");
				maskTestResults.setText("TEST PASSED" + "\n");
			}
		} else {
			System.out.println("Either high/low/reading isn't loaded properly");
		}
	}

	/**
	 * Displays the intervals which failed + total time failed.
	 */
	private void displayFailedIntervals() {
		// Remove duplicates
		overlappedIntervals = overlappedIntervals.stream().distinct().collect(Collectors.toList());

		// Sort in ascending order x-axis
		overlappedIntervals.sort(compare.sortOverlap());

		// Set up text display of failed regions.
		String interval1 = "( - )";
		String interval2 = "( - )";
		int failedRegionStart = 0;

		for (Line2D l : overlappedIntervals) {
			String int1 = "(" + l.x1 + ", " + l.y1 + ") -";
			String int2 = " (" + l.x2 + ", " + l.y2 + ") ";

			System.out.println(int1 + int2);
		}

		// Display intervals where overlapping occurred (excluding final region).
		for (int i = 0; i < overlappedIntervals.size() - 1; i++) {
			if (overlappedIntervals.get(i).x2 < overlappedIntervals.get(i + 1).x1) {
				// Get a sublist of the directly failed region
				determineOverlapRegion(overlappedIntervals, failedRegionStart, (i + 1), interval1,
						interval2);

				// Change the start of the next overlapped interval
				failedRegionStart = (i + 1);
			}
		}
		// Display intervals where overlapping occurred (including final region).
		determineOverlapRegion(overlappedIntervals, failedRegionStart, overlappedIntervals.size(),
				interval1, interval2);

		maskTestResults.appendText("------------------------------" + "\n");
		maskTestResults.appendText("FAILED AMOUNT OF TIME: "
				+ determineFailedOverlapTime(overlappedIntervals) + "s\n");
		maskTestResults.appendText("------------------------------" + "\n");
	}

	/**
	 * A private helper function to 'displayFailedIntervals' which displays all the regions that are
	 * invalid.
	 * 
	 * @param overlappedIntervals
	 *            a list of line segments which failed (went into mask region).
	 * @param start
	 *            the first element where the sublist should start
	 * @param interval1
	 *            displays the first time interval of the failed region
	 * @param interval2
	 *            displays the final time interval of the failed region
	 */
	private void determineOverlapRegion(List<Line2D> overlappedIntervals, int start, int end,
			String interval1, String interval2) {
		// Sublist of the complete list of line segments which failed
		List<Line2D> subOverlappedIntervals = overlappedIntervals.subList(start, end);

		interval1 = "(" + subOverlappedIntervals.get(0).x1 + ", " + subOverlappedIntervals.get(0).y1
				+ ") -";
		interval2 = " (" + subOverlappedIntervals.get(subOverlappedIntervals.size() - 1).x2 + ", "
				+ subOverlappedIntervals.get(subOverlappedIntervals.size() - 1).y2 + ") ";

		maskTestResults.appendText(interval1 + interval2 + "\n");
	}

	/**
	 * A private helper function to 'displayFailedIntervals' which calculates the total amount of
	 * time spent in the failed regions.
	 * 
	 * @param overlappedIntervals
	 *            a list of line segments which failed (went into mask region).
	 * @return the total amount of time (in seconds) spent in the failed regions
	 */
	private double determineFailedOverlapTime(List<Line2D> overlappedIntervals) {
		double totalOverlapTime = 0;

		// Calculate total time in failed region.
		for (int i = 0; i < overlappedIntervals.size(); i++) {
			totalOverlapTime += (overlappedIntervals.get(i).x2 - overlappedIntervals.get(i).x1);
		}

		return totalOverlapTime;
	}

	/**
	 * A private helper function for 'runMaskTest' which determines if any of the loaded points
	 * overlaps the mask area below the line.
	 */
	private int checkForMaskAreaOverlap(int counter) {
		for (int i = 0; i < readingSeries.getData().size() - 1; i++) {
			XYChart.Data<Number, Number> currentDataPoint = readingSeries.getData().get(i);
			XYChart.Data<Number, Number> nextDataPoint = readingSeries.getData().get(i + 1);

			counter += lineChart.maskTestOverlapCheck(
					lineChart.getPolygonArray(highMaskBoundarySeries), currentDataPoint,
					nextDataPoint); // high

			counter += lineChart.maskTestOverlapCheck(
					lineChart.getPolygonArray(lowMaskBoundarySeries), currentDataPoint,
					nextDataPoint); // low
		}

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
					Line2D segment = new Line2D();

					segment.setLine(new com.sun.javafx.geom.Point2D(tempX, tempY),
							new com.sun.javafx.geom.Point2D(nextX, nextY));
					overlappedIntervals.add(segment);

					errorCounter++;
				}
			}
		}
		return errorCounter;
	}

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

			setHighBtn.setDisable(true);
			setMaskBtn.setDisable(true);

			String yUnitValue = "";

			// Check if there is a clash of units.
			if (yUnit.size() > 0) {
				for (String[] column : model.readMaskData(selectedFile.getPath())) {
					yUnitValue = column[3];
					if (yUnitValue.equals(yUnit.get(0))) {
						convertMeasurementYUnit(yUnitValue);
						break;
					} else {
						errorMessageInvalidMask();
						return;
					}
				}
			}

			addMaskDataPoints(selectedFile);

			// Enable running of mask test
			runMaskBtn.setDisable(false);
			maskTestResults.setDisable(false);
		}
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
	 * @param selectedFile
	 *            the file to get imported mask data from
	 */
	private void addMaskDataPoints(File selectedFile) {
		XYChart.Series<Number, Number> tempHighMaskBoundarySeries = new XYChart.Series<>();
		XYChart.Series<Number, Number> tempLowMaskBoundarySeries = new XYChart.Series<>();

		for (String[] column : model.readMaskData(selectedFile.getPath())) {
			if (column[0].equals("high")) {
				XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(
						Double.parseDouble(column[1]), Double.parseDouble(column[2]));
				tempHighMaskBoundarySeries.getData().add(dataPoint);
			} else {
				XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(
						Double.parseDouble(column[1]), Double.parseDouble(column[2]));
				tempLowMaskBoundarySeries.getData().add(dataPoint);
			}
		}

		// Sort unordered masks.
		tempHighMaskBoundarySeries.getData().sort(compare.sortChart());
		tempLowMaskBoundarySeries.getData().sort(compare.sortChart());

		// Check if overlap occurs //TODO: check that flipping high and low to low and high is fine
		if (testOverlap(tempLowMaskBoundarySeries, tempHighMaskBoundarySeries)) {
			highMaskBoundarySeries.getData().addAll(tempHighMaskBoundarySeries.getData());
			lowMaskBoundarySeries.getData().addAll(tempLowMaskBoundarySeries.getData());
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
				model.saveMaskData(bw, highMaskBoundarySeries, yUnit.get(0));
				model.saveMaskData(bw, lowMaskBoundarySeries, yUnit.get(0));
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

		initialiseSampleRate();

		// CHANGE low and high to something else, more meaningful
		highMaskBoundarySeries.setName("high boundary");
		lowMaskBoundarySeries.setName("low boundary");
		readingSeries.setName("multimeter data");

		// SerialFramework.changePorts();
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
			// if (serialPort.isOpen()) {
			// System.out.println("PORT IS OPEN");
			// } else {
			// System.out.println("PORT IS CLOSED: " + serialPort.getSystemPortName());
			// }
			if (serialPort.getSystemPortName().contains("tty.usbmodem0E21B171")) { // ->
																					// /dev/tty.usbmodem0E21B171
				serialPort.openPort();
				serialPort.setBaudRate(115200); // baudrate

				System.out.println(
						"Binding to Serial Port " + serialPort.getSystemPortName() + "...");
				if (SerialFramework.bindListen(serialPort)) {
					System.out.println("Success.");
				} else {
					System.out.println("Failed to bind to Serial.");
					refreshSelectablePortsList();
				}

				break;
			}

			System.out.println("PORTS: " + serialPort.getSystemPortName());
		}
	}

}
