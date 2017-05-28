package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * The GuiController class represents the Controller of the Model-View-Controller pattern
 * 
 * @author dayakern
 *
 */
public class GuiController implements Initializable {
	/* Constants */
	private static final DecimalFormat MEASUREMENT_DECIMAL = new DecimalFormat("0.000");
	private static final DecimalFormat TIME_DECIMAL = new DecimalFormat("0.0");
	private static final String ISO_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.S";

	private static final String FILE_FORMAT_EXTENSION = "*.csv";
	private static final String FILE_FORMAT_TITLE = "Comma Separated Files";

	private static final double X_UPPER_BOUND = 20D;
	private static final double X_LOWER_BOUND = 0D;
	private static final double Y_UPPER_BOUND = 50D;
	private static final double Y_LOWER_BOUND = -10D;

	/* Other classes used */
	private GuiModel model;
	private DataEvents event;
	private DataComparator compare;
	private ModifyMultimeterMeasurements modifyMeasurements;
	private CheckOverlap checkingOverlap;
	private ISOTimeInterval startTime;
	private SerialTest serialTest;

	/* Components required for resizing the GUI when maximising or resizing */
	@FXML
	Pane appPane;
	@FXML
	AnchorPane rightAnchor;
	@FXML
	AnchorPane graphLabelAnchor;
	@FXML
	GridPane chartGrid;
	@FXML
	protected TabPane modeOptions;
	@FXML
	protected Tab disconnectedTab;
	@FXML
	protected Tab connectedTab;

	/* Components relating to which data to display */
	private int dataPlotPosition;
	protected volatile boolean resistance;
	protected volatile boolean voltage;
	protected volatile boolean current;
	protected volatile boolean continuity;
	protected volatile boolean logic;

	private volatile boolean isChanged;

	/* Components relating to the 'connected' mode */
	@FXML
	private Button pauseBtn;
	private volatile boolean isPaused;// Flag for if pauseBtn has been clicked

	@FXML
	private Button saveBtn;
	@FXML
	protected Button discardBtn;
	@FXML
	protected ComboBox<String> portsAvailable;
	@FXML
	private Button refreshBtn;

	/* Components relating to the 'disconnected' mode */

	// To display plotted data point information
	@FXML
	private Label plotCoordLabel;
	@FXML
	protected Label xDataCoord;
	@FXML
	protected Label yDataCoord;
	@FXML
	protected Label recordTimeLabel;

	// Holds x/y coordinates of mouse position relative to line chart background
	@FXML
	protected Label yCoordValues;
	@FXML
	protected Label xCoordValues;

	/* Components relating to mask-testing */
	@FXML
	private Button discardAllBtn;
	@FXML
	private Button importMaskBtn;
	@FXML
	private Button exportMaskBtn;
	@FXML
	private RadioButton maskVRBtn;
	@FXML
	private RadioButton maskARBtn;
	@FXML
	private RadioButton maskORBtn;
	@FXML
	private Button setHighBtn;
	private boolean isHighBtnSelected; // Flag for if setHighBtn has been clicked
	@FXML
	private Button setLowBtn;
	private boolean isLowBtnSelected;// Flag for if setLowBtn has been clicked

	@FXML
	private Button setMaskBtn;
	@FXML
	private Button runMaskBtn;

	/* Components relating to mask testing */
	@FXML
	protected TextArea maskTestResults;
	private List<Line2D> overlappedIntervals;

	// To keep track of the previous mask boundary point
	int lowCounter;

	/* Line chart components */
	NumberAxis xAxis;
	NumberAxis yAxis;
	ModifiedLineChart lineChart;
	Node chartBackground; // Handle on chart background for getting lineChart coordinates

	// Holds the upper & lower boundary points and read data
	private XYChart.Series<Number, Number> highMaskBoundarySeries;
	private XYChart.Series<Number, Number> lowMaskBoundarySeries;
	private XYChart.Series<Number, Number> readingSeries;
	private ArrayList<String> storedYUnits; // To store the displayed y-unit

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
	private Button selectACDCBtn;
	private boolean isACMode;
	@FXML
	private Label switchDCLabel;

	@FXML
	private Label brightnessLabel;
	@FXML
	private ComboBox<Integer> brightnessLevel;
	private ObservableList<Integer> brightnessLevels;

	@FXML
	private ComboBox<String> sampleRate;
	private ObservableList<String> sampleRates;

	/* Components relating to acquired data */
	private ArrayList<Data<Number, Number>> totalAcquisitionData;
	private ArrayList<ISOTimeInterval> storedISOTimes;
	private ArrayList<ISOTimeInterval> pausedStoredISOTimeData;
	private ArrayList<String> pausedStoredYUnitData;

	private AtomicBoolean quit;

	public static GuiController instance;

	public GuiController() {
		instance = this;
		this.quit = new AtomicBoolean(false);
		this.serialTest = new SerialTest(quit);

		overlappedIntervals = new ArrayList<>();
		highMaskBoundarySeries = new XYChart.Series<>();
		lowMaskBoundarySeries = new XYChart.Series<>();
		readingSeries = new XYChart.Series<>();
		isLowBtnSelected = false;
		isHighBtnSelected = false;

		totalAcquisitionData = new ArrayList<>();
		storedISOTimes = new ArrayList<>();
		pausedStoredISOTimeData = new ArrayList<>();
		pausedStoredYUnitData = new ArrayList<>();
		storedYUnits = new ArrayList<>();

		model = new GuiModel();
		event = new DataEvents();
		compare = new DataComparator();
		modifyMeasurements = new ModifyMultimeterMeasurements();
		checkingOverlap = new CheckOverlap();
		startTime = null;

		lowCounter = 0;
		dataPlotPosition = 0;
		resistance = false;
		voltage = false;
		current = false;
		continuity = false;
		logic = false;
		isChanged = false;
		isPaused = false;
		isACMode = false;

		xAxis = new NumberAxis();
		yAxis = new NumberAxis();

		sampleRates = FXCollections.observableArrayList();
		brightnessLevels = FXCollections.observableArrayList();
	}

	/**
	 * Gets the overlapped mask intervals.
	 * 
	 * @return the overlapped intervals
	 */
	public List<Line2D> getOverlappedIntervals() {
		return overlappedIntervals;
	}

	/**
	 * Gets the high mask boundary.
	 * 
	 * @return the high mask boundary series
	 */
	public XYChart.Series<Number, Number> getHighSeries() {
		return highMaskBoundarySeries;
	}

	/**
	 * Gets the lower mask boundary.
	 * 
	 * @return the low mask boundary series
	 */
	public XYChart.Series<Number, Number> getLowSeries() {
		return lowMaskBoundarySeries;
	}

	/**
	 * Getter method for lower mask boundary.
	 * 
	 * @return the low mask boundary series
	 */
	public XYChart.Series<Number, Number> getDataSeries() {
		return readingSeries;
	}

	/**
	 * Decreases the upper and lower bounds of the x-axis to let the user see as much plotted data as they want instead
	 * of just a set amount. Zero is the farthest the user can move the plot left.
	 */
	@FXML
	private void moveXAxisLeft() {

		double newAxisUpperValue = xAxis.getUpperBound() - 1;
		double newAxisLowerValue = xAxis.getLowerBound() - 1;

		if (newAxisLowerValue >= 0) { // Cannot move further left
			xAxis.setUpperBound(newAxisUpperValue);
			xAxis.setLowerBound(newAxisLowerValue);
		}
	}

	/**
	 * Increases the upper and lower bounds of the x-axis to let the user see as much plotted data as they want instead
	 * of just a set amount.
	 */
	@FXML
	private void moveXAxisRight() {
		double newAxisUpperValue = xAxis.getUpperBound() + 1;
		double newAxisLowerValue = xAxis.getLowerBound() + 1;

		lineChart.updateMaskBoundaries(newAxisUpperValue, newAxisLowerValue);
	}

	/**
	 * Enables or disables the connected multimeter components depending on whether or not there is a valid two-way
	 * connection.
	 * 
	 * @param status
	 *            whether or not the components should be disabled (true) or enabled (false)
	 */
	protected void setConnectedMultimeterComponents(boolean status) {
		// sampleRate.setDisable(status);
		// brightnessLevel.setDisable(status); //FIXME

		voltageBtn.setDisable(status);
		currentBtn.setDisable(status);
		resistanceBtn.setDisable(status);

		switchDCLabel.setDisable(status);
		brightnessLabel.setDisable(status);
		selectACDCBtn.setDisable(status);

		modeLabel.setDisable(status);
		continuityBtn.setDisable(status);
		logicBtn.setDisable(status);
	}

	/**
	 * Enables or disables the connected mode components depending on whether or not there is a valid two-way
	 * connection.
	 * 
	 * @param status
	 *            whether or not the components should be disabled (true) or enabled (false)
	 */
	public void setConnectedModeComponents(boolean status) {
		pauseBtn.setDisable(status);
		saveBtn.setDisable(status);
		discardBtn.setDisable(status);
	}

	/**
	 * Switches the voltage and current to DC when selected and back to AC when deselected.
	 */
	@FXML
	private void switchACDC() {
		if (!isACMode) {
			isACMode = true;
			selectACDCBtn.setText("AC");

		} else {
			isACMode = false;
			selectACDCBtn.setText("DC");
		}
	}

	/**
	 * Changes the status of the 'selectACDCBtn' button to DC, as well as sets the AC flag
	 */
	public void driveACMode() {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					driveACMode();
				}
			});
			return;
		}

		if (!isPaused) {
			isACMode = true; // in AC mode
			selectACDCBtn.setText("DC"); // Want to switch to DC
		}
	}

	/**
	 * Changes the status of the 'selectACDCBtn' button to AC, as well as sets the AC flag
	 */
	public void driveDCMode() {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					driveDCMode();
				}
			});
			return;
		}

		if (!isPaused) {
			isACMode = false; // in DC mode
			selectACDCBtn.setText("AC"); // Want to switch to AC
		}
	}

	/**
	 * Writes out code to remotely control multimeter's continuity mode.
	 */
	@FXML
	private void selectContinuityMode() {
		String code = MultimeterCodes.CONTINUITY.getCode();
		serialTest.writeCode(code);
	}

	// FIXME
	/**
	 * Executes any continuity mode related events.
	 */
	protected void driveContinuity() {
		System.out.println("Driving continuity");
	}

	/**
	 * Writes out code to remotely control multimeter's logic mode
	 */
	@FXML
	private void selectLogicMode() {
		String code = MultimeterCodes.LOGIC.getCode();
		serialTest.writeCode(code);
	}

	// FIXME
	/**
	 * Executes any logic mode related events
	 */
	protected void driveLogic() {
		System.out.println("Driving Logic");

	}

	/**
	 * Writes out code to remotely control multimeter (Select voltage).
	 */
	@FXML
	private void measureVoltage() {
		String code = "";

		if (!isACMode) { // DC
			System.err.println("-----");
			code = MultimeterCodes.VOLTAGE.getCode();
		} else { // AC
			System.err.println("++++");
			code = MultimeterCodes.VOLTAGE_RMS.getCode();
		}
		serialTest.writeCode(code);
	}

	// FIXME
	/**
	 * Executes any voltage mode related events
	 */
	protected void driveVoltage() {
		System.out.println("Driving voltage");
	}

	/**
	 * Writes out code to remotely control multimeter (Select current).
	 */
	@FXML
	private void measureCurrent() {
		String code = "";

		if (!isACMode) { // DC
			System.err.println("====");
			code = MultimeterCodes.CURRENT.getCode();
		} else { // AC
			System.err.println("*****");
			code = MultimeterCodes.CURRENT_RMS.getCode();
		}

		serialTest.writeCode(code);
	}

	// FIXME
	/**
	 * Executes any current mode related events
	 */
	protected void driveCurrent() {
		System.out.println("Driving current");
	}

	/**
	 * Writes out code to remotely control multimeter (Select resistance).
	 */
	@FXML
	private void measureResistance() {
		String code = MultimeterCodes.RESISTANCE.getCode();
		serialTest.writeCode(code);
	}

	/**
	 * Executes any resistance mode related events.
	 */
	protected void driveResistance() {
		System.out.println("Driving resistance");
	}

	/**
	 * Updates the initial list of the required configurable brightness levels.
	 */
	private void initialiseBrightnessLevels() {
		brightnessLevels.add(0);
		brightnessLevels.add(25);
		brightnessLevels.add(50);
		brightnessLevels.add(75);
		brightnessLevels.add(100);

		brightnessLevel.setItems(brightnessLevels);
	}

	@FXML
	private void selectBrightnessLevel() {
		if (brightnessLevel.getSelectionModel().getSelectedIndex() == 0) { // 0
			System.err.println("[B 0]");

			String code = MultimeterCodes.BRIGHTNESS_0.getCode();
			serialTest.writeCode(code);
		} else if (brightnessLevel.getSelectionModel().getSelectedIndex() == 1) { // 25
			System.err.println("[B 1]");

			String code = MultimeterCodes.BRIGHTNESS_1.getCode();
			serialTest.writeCode(code);
		} else if (brightnessLevel.getSelectionModel().getSelectedIndex() == 2) { // 50
			System.err.println("[B 2]");

			String code = MultimeterCodes.BRIGHTNESS_2.getCode();
			serialTest.writeCode(code);
		} else if (brightnessLevel.getSelectionModel().getSelectedIndex() == 3) { // 75
			System.err.println("[B 3]");

			String code = MultimeterCodes.BRIGHTNESS_3.getCode();
			serialTest.writeCode(code);
		} else { // 100
			System.err.println("[B 4]");
			String code = MultimeterCodes.BRIGHTNESS_4.getCode();
			serialTest.writeCode(code);
		}
	}

	/**
	 * Updates the initial list of the required configurable sample rates.
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
	}

	/**
	 * Sends out different codes depending on the sample rate selected
	 */
	@FXML
	private void selectSampleRate() {
		if (sampleRate.getSelectionModel().getSelectedIndex() == 0) {
			System.err.println("[F A]");

			String code = MultimeterCodes.SAMPLE_RATE_A.getCode();
			serialTest.writeCode(code);
		} else if (sampleRate.getSelectionModel().getSelectedIndex() == 1) {
			System.err.println("[F B]");

			String code = MultimeterCodes.SAMPLE_RATE_B.getCode();
			serialTest.writeCode(code);
		} else if (sampleRate.getSelectionModel().getSelectedIndex() == 2) {
			System.err.println("[F C]");

			String code = MultimeterCodes.SAMPLE_RATE_C.getCode();
			serialTest.writeCode(code);
		} else if (sampleRate.getSelectionModel().getSelectedIndex() == 3) {
			System.err.println("[F D]");

			String code = MultimeterCodes.SAMPLE_RATE_D.getCode();
			serialTest.writeCode(code);
		} else if (sampleRate.getSelectionModel().getSelectedIndex() == 4) {
			System.err.println("[F E]");

			String code = MultimeterCodes.SAMPLE_RATE_E.getCode();
			serialTest.writeCode(code);
		} else if (sampleRate.getSelectionModel().getSelectedIndex() == 5) {
			System.err.println("[F F]");

			String code = MultimeterCodes.SAMPLE_RATE_F.getCode();
			serialTest.writeCode(code);
		} else if (sampleRate.getSelectionModel().getSelectedIndex() == 6) {
			System.err.println("[F G]");

			String code = MultimeterCodes.SAMPLE_RATE_G.getCode();
			serialTest.writeCode(code);
		} else if (sampleRate.getSelectionModel().getSelectedIndex() == 7) {
			System.err.println("[F H]");

			String code = MultimeterCodes.SAMPLE_RATE_H.getCode();
			serialTest.writeCode(code);
		} else {
			System.err.println("[F I]");

			String code = MultimeterCodes.SAMPLE_RATE_I.getCode();
			serialTest.writeCode(code);
		}
	}

	/**
	 * Updates the data displayed on the line chart.
	 * 
	 * @param multimeterDataValue
	 *            the received multimeter data value
	 * @param unit
	 *            the received y-unit value
	 */
	public void recordAndDisplayNewResult(Double multimeterDataValue, String unit) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					recordAndDisplayNewResult(multimeterDataValue, unit);
				}
			});
			return;
		}

		// Update Multimeter display
		updateMultimeterDisplay(multimeterDataValue, unit);
	}

	/**
	 * A private helper function to 'recordAndDisplayNewResult' which displays multimeter data on chart.
	 * 
	 * @param multimeterDataValue
	 *            the received multimeter data value
	 * @param unit
	 *            the received y-unit value
	 */
	private void updateMultimeterDisplay(Double multimeterDataValue, String unit) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					updateMultimeterDisplay(multimeterDataValue, unit);
				}
			});
			return;
		}

		// Modify Plot Parts.
		System.out.println("U------U:" + unit);
		if (!modifyMeasurements.validateYAxisUnits(unit)) {
			modifyPlotParts();
		}

		// Add data
		if (!isPaused) { // Not paused
			liveDataMode(multimeterDataValue, unit);
		} else { // Paused
			pausedDataMode(multimeterDataValue, unit);
		}

		dataPlotPosition++;
	}

	/**
	 * A private helper function to 'updateMultimeterDisplay' which dictates how the storage of data occurs and how the
	 * data looks like displayed within the GUI when it's not paused.
	 * 
	 * @param multimeterDataValue
	 *            the received multimeter data value
	 * @param unit
	 *            the received y-unit value
	 */
	private void liveDataMode(Double multimeterDataValue, String unit) {

		// Change multimeter text display according to ranges and values.
		modifyMeasurements.convertYUnit(unit, yAxis); // FIXME: UNITS

		// Has been paused as some point
		acquiredDataHasBeenPaused();

		// Add to stored data
		pausedStoredYUnitData.add(modifyMeasurements.getUnitToSave(unit));

		// ISO Time Intervals
		ISOTimeInterval endTime = establishISOTime();
		pausedStoredISOTimeData.add(endTime);

		// System.out.println("START: " + startTime + " END: " + endTime);

		// Normally add received data
		Double xValue = ISOTimeInterval.xValue(startTime.getDate(), endTime.getDate());

		readingSeries.getData().add(new XYChart.Data<Number, Number>(xValue, multimeterDataValue));

		// Add plot behaviour
		readingSeries.getData().get(dataPlotPosition).getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
				event.getDataXYValues(readingSeries.getData().get(dataPlotPosition), dataPlotPosition, xDataCoord,
						yDataCoord, startTime.toString(), endTime.toString(), false));

		// Update chart bounds
		updateChartXBounds(startTime.getDate(), endTime.getDate());
	}

	/**
	 * Updates the chart's upper and lower x-axis bounds.
	 * 
	 * @param startTime
	 *            the first time the data point was recorded
	 * @param endTime
	 *            the lastest time the last data point was recorded
	 */
	private void updateChartXBounds(LocalDateTime startTime, LocalDateTime endTime) {
		int dataBoundsRange = (int) Math.ceil(ISOTimeInterval.xValue(startTime, endTime));
		if (dataBoundsRange > X_UPPER_BOUND) {
			xAxis.setLowerBound(dataBoundsRange - X_UPPER_BOUND);
			xAxis.setUpperBound(dataBoundsRange);
		}
	}

	/**
	 * A private helper function to 'liveData' which adds the accumulated data to the displayed multimeter data on the
	 * chart (as well as adding to the ISO time and y-units list).
	 */
	private void acquiredDataHasBeenPaused() {
		if (totalAcquisitionData.size() > 0) {

			// If the y-unit has been changed, clear all multimeter data
			if (isChanged) {
				readingSeries.getData().clear();
				pausedStoredYUnitData.clear();
				pausedStoredISOTimeData.clear();

				resetXAxis();

				isChanged = false;
			}

			// Add results to multimeter data, y-units and time
			readingSeries.getData().addAll(totalAcquisitionData);
			pausedStoredYUnitData.addAll(storedYUnits);
			pausedStoredISOTimeData.addAll(storedISOTimes);

			addAcquiredDataWhilePaused(pausedStoredISOTimeData);

			// Reset paused acquired data
			totalAcquisitionData.clear();
			storedYUnits.clear();
			storedISOTimes.clear();
		}
	}

	/**
	 * A private helper function to 'acquiredDataHasBeenPaused' which adds the plot behaviour to the points acquired
	 * when the application was paused.
	 * 
	 * @param pausedStoredISOTimeDataPoints
	 *            the list of times the data was received when the application was paused.
	 */
	private void addAcquiredDataWhilePaused(ArrayList<ISOTimeInterval> pausedStoredISOTimeDataPoints) {

		// Add listener to add acquired data when it was paused.
		for (int i = 0; i < readingSeries.getData().size(); i++) {

			readingSeries.getData().get(i).getNode().addEventHandler(MouseEvent.MOUSE_ENTERED,
					event.getDataXYValues(readingSeries.getData().get(i), i, xDataCoord, yDataCoord,
							startTime.toString(), pausedStoredISOTimeData.get(i).toString(), false));
		}
	}

	/**
	 * A private helper function to 'updateMultimeterDisplay' which dictates how the storage of data occurs when it's
	 * paused.
	 * 
	 * @param multimeterDataValue
	 *            the received multimeter data value
	 * @param unit
	 *            the received y-unit value
	 */
	private void pausedDataMode(Double multimeterDataValue, String unit) {

		// Store total y-units
		storedYUnits.add(modifyMeasurements.getUnitToSave(unit));

		ISOTimeInterval endTime = establishISOTime();

		// Store total ISO time intervals
		storedISOTimes.add(endTime);

		// Modify Plot Parts.
		totalAcquisitionData.add(new XYChart.Data<Number, Number>(
				ISOTimeInterval.xValue(startTime.getDate(), endTime.getDate()), multimeterDataValue));
	}

	/**
	 * A private helper function to 'updateMultimeterDisplay' which does the modifying of the plot and sets the flag
	 * that when the data was paused, the measurement value changed (i.e. V->A)
	 */
	private void modifyPlotParts() {

		// Reset the plot data
		dataPlotPosition = 0;

		if (!isPaused) {
			resetXAxis();
			readingSeries.getData().clear();
			pausedStoredYUnitData.clear();
			pausedStoredISOTimeData.clear();
		} else {
			isChanged = true;

			// Only clear acquiring data, not displayed
			totalAcquisitionData.clear();
			storedYUnits.clear();
			storedISOTimes.clear();
		}
	}

	/**
	 * Calculates the ISO 8601 time interval between the first and latest last point.
	 * 
	 * @return the time the point was displayed
	 */
	private ISOTimeInterval establishISOTime() {
		ISOTimeInterval endTime = null; // Time of each data point

		if (dataPlotPosition == 0) { // Get the start time and initial end time
			LocalDateTime local = LocalDateTime.now();
			startTime = new ISOTimeInterval(local, DateTimeFormatter.ofPattern(ISO_FORMATTER));
			endTime = startTime;
		} else { // Get the end time //FIXME: CHANGED LOCAL2 TO LOCAL
			LocalDateTime local = LocalDateTime.now();
			endTime = new ISOTimeInterval(local, DateTimeFormatter.ofPattern(ISO_FORMATTER));
		}

		// Display ISO time if the data is not paused.
		if (!isPaused) {
			displayDateStamp(startTime.toString(), endTime.toString());
		}

		return endTime;
	}

	/**
	 * Displays the date-stamp start and end points.
	 * 
	 * @param startTime
	 *            the time of the first reading
	 * @param endTime
	 *            the time of the latest last reading
	 */
	private void displayDateStamp(String startTime, String endTime) {
		recordTimeLabel.setText(startTime + " / " + endTime);
	}

	/**
	 * If the units of the y-values change, then reset the x-axes bounds.
	 */
	private void resetXAxis() {
		if (voltage || current || resistance || logic || continuity) {
			xAxis.setLowerBound(X_LOWER_BOUND);
			xAxis.setUpperBound(X_UPPER_BOUND);

			xAxis.setMinorTickCount(2);
			xAxis.setTickUnit(1D);
		}
	}

	/**
	 * Reverts the status' of connected components.
	 */
	protected void revertConnectedComponents() {

		// Reset ports
		quit();
		serialTest.refreshSelectablePortsList();
		this.quit.set(false);

		resetAxesGraphDetails();

		// TODO: Make sure this is set at some point
		// sampleRate.setValue(sampleRate.getPromptText());

		// Reset displayed multimeter display
		multimeterDisplay.setText("");

		isPaused = false;
		isChanged = false;

		totalAcquisitionData.clear();
		pauseBtn.setText("Pause");

		selectACDCBtn.setDisable(true);
		isACMode = false;

		resistance = false;
		voltage = false;
		current = false;
		continuity = false;
		logic = false;

		// Reset the plot data
		readingSeries.getData().clear();
		pausedStoredISOTimeData.clear();
		pausedStoredYUnitData.clear();
		storedYUnits.clear();
		storedISOTimes.clear();
		startTime = null;
		dataPlotPosition = 0;
	}

	/**
	 * Reverts the status' of mask-testing components.
	 */
	protected void revertMaskTestingComponents() {

		// Reset data options components
		setHighBtn.setDisable(false);
		isHighBtnSelected = false;

		setLowBtn.setDisable(true);
		isLowBtnSelected = false;

		setMaskBtn.setDisable(false);

		importMaskBtn.setDisable(false);

		exportMaskBtn.setDisable(true);
		maskORBtn.setDisable(true);
		maskARBtn.setDisable(true);
		maskVRBtn.setDisable(true);

		runMaskBtn.setDisable(true);
		maskTestResults.setDisable(true);
		maskTestResults.clear();

		// Reset graph side components
		resetAxesGraphDetails();

		highMaskBoundarySeries.getData().clear();
		lowMaskBoundarySeries.getData().clear();
		readingSeries.getData().clear();
		overlappedIntervals.clear();
		storedYUnits.clear(); // FIXME

		lowCounter = 0;
		startTime = null;

		System.out.println(isHighBtnSelected);
		System.out.println("LC " + lowCounter);
	}

	/**
	 * Clears all of the currently displaying data in 'disconnected mode'.
	 */
	@FXML
	private void discardAll() {
		if (notifyDiscardingData()) {

			// Reset the data
			revertMaskTestingComponents();

			System.out.println("DATA DISCARDED");
		}
	}

	/**
	 * Resets the axes to their original upper and lower boundaries, as well as graph axis details.
	 */
	private void resetAxesGraphDetails() {
		xAxis.setLowerBound(X_LOWER_BOUND);
		xAxis.setUpperBound(X_UPPER_BOUND);
		xAxis.setMinorTickCount(2);
		xAxis.setTickUnit(1D);

		yAxis.setLowerBound(Y_LOWER_BOUND);
		yAxis.setUpperBound(Y_UPPER_BOUND);
		yAxis.setMinorTickCount(5);
		yAxis.setTickUnit(5D);
		yAxis.setLabel("Measurements");

		// Reset the displayed chart coordinate values.
		xCoordValues.setText("X: ");
		yCoordValues.setText("Y: ");
		recordTimeLabel.setText("");

		// Reset the displayed plot related details.
		xDataCoord.setText("X: ");
		yDataCoord.setText("Y: ");
		recordTimeLabel.setText("");
	}

	/**
	 * Pauses the displayed acquired data.
	 */
	@FXML
	private void pauseDataAcquisition() {

		if (!isPaused) {
			System.out.println("DATA IS PAUSED");
			// System.out.println("PAUSED RS: " + readingSeries.getData().size() + ", " + yUnits.size());

			isPaused = true;
			pauseBtn.setText("Unpause");

			// Disable multimeter components
			multimeterDisplay.setDisable(true);
			multimeterDisplay.clear();

			voltageBtn.setDisable(true);
			currentBtn.setDisable(true);
			resistanceBtn.setDisable(true);
			selectACDCBtn.setDisable(true);

			modeLabel.setDisable(true);
			logicBtn.setDisable(true);
			continuityBtn.setDisable(true);
		} else {
			System.out.println("DATA IS UNPAUSED");

			resumedDataAcquisition();
		}
	}

	/**
	 * Enable multimeter components when pause button has been clicked on again, and pause is now resumed
	 */
	private void resumedDataAcquisition() {
		isPaused = false;
		pauseBtn.setText("Pause");

		// Enable digital multimeter components
		multimeterDisplay.setDisable(false);
		voltageBtn.setDisable(false);
		currentBtn.setDisable(false);
		resistanceBtn.setDisable(false);
		selectACDCBtn.setDisable(false);
		multimeterDisplay.setDisable(false);
		modeLabel.setDisable(false);
		logicBtn.setDisable(false);
		continuityBtn.setDisable(false);
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
		loadFileOptions.getExtensionFilters().add(new ExtensionFilter(FILE_FORMAT_TITLE, FILE_FORMAT_EXTENSION));

		File selectedFile = loadFileOptions.showOpenDialog(GuiView.getInstance().getStage());

		// Only if file exists extract information from it
		if (selectedFile != null) {
			System.out.println("FILE NAME: " + selectedFile.getPath());

			// Set boundaries if they haven't already been set
			setBoundariesInStone();

			// Clear data from list if reloaded multiple times
			readingSeries.getData().clear();
			storedISOTimes.clear();
			storedYUnits.clear();

			yAxis.setLabel("Measurements");

			// Set up new arrays
			ArrayList<Double> inputDataXValues = new ArrayList<>();
			ArrayList<Double> inputDataYValues = new ArrayList<>();
			ArrayList<String> inputDataYUnits = new ArrayList<>();
			ArrayList<String> inputDataIsoTime = new ArrayList<>();

			// Read from file
			readDataFromFile(selectedFile, inputDataXValues, inputDataYValues, inputDataYUnits, inputDataIsoTime);

			// Modify y-units
			storedYUnits.addAll(inputDataYUnits); // FIXME?
			if (storedYUnits.size() > 0) { // should be greater than 0.
				modifyMeasurements.convertMeasurementYUnit(storedYUnits.get(0), yAxis);
			}

			yAxis.setAutoRanging(true); // FIXME

			// Display data + plot behaviours
			addDataToSeries(inputDataXValues, inputDataYValues, inputDataIsoTime);

			// Disable specifying of mask y-unit since loading in a file
			if (!(maskVRBtn.isDisabled() && maskARBtn.isDisabled() && maskORBtn.isDisabled())) {
				maskVRBtn.setDisable(true);
				maskARBtn.setDisable(true);
				maskORBtn.setDisable(true);
			}
		}
	}

	/**
	 * A private helper function for 'loadFile' which reads in data from a given file.
	 * 
	 * @param selectedFile
	 *            file to read data from
	 * @param inputDataXValues
	 *            temporary placeholder for x values
	 * @param inputDataYValues
	 *            temporary placeholder for y values
	 * @param inputDataIsoTime
	 *            temporary placeholder for ISO time values
	 */
	private void readDataFromFile(File selectedFile, ArrayList<Double> inputDataXValues,
			ArrayList<Double> inputDataYValues, ArrayList<String> inputDataYUnits, ArrayList<String> inputDataIsoTime) {

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

		// Read in ISO time values & convert to ISO time
		for (String s : model.readColumnData(selectedFile.getPath(), 3)) {
			inputDataIsoTime.add(s);
		}

	}

	/**
	 * A private helper function to 'loadFile' which adds the x and y values to the correct line chart series
	 * (readingSeries) as well as to determine which ISO display behaviour to use (files from SD card and from recorded
	 * software are different).
	 * 
	 * @param inputDataXValues
	 *            the x values loaded in from the file
	 * @param inputDataYValues
	 *            the y values loaded in from the file
	 * @param isoTimes
	 *            the ISO times loaded in from the file
	 */
	private void addDataToSeries(ArrayList<Double> inputDataXValues, ArrayList<Double> inputDataYValues,
			ArrayList<String> isoTimes) {

		String checkedIsoTime = "";
		ArrayList<String> checkedIsoTimes = new ArrayList<>();
		boolean isSD = false;

		try { // Software mode
			System.out.println("SOFTWARE");

			ISOTimeInterval firstPointXValue = ISOTimeInterval.parseISOTime(isoTimes.get(0));
			checkedIsoTime = firstPointXValue.toString();
			checkedIsoTimes = isoTimes;
			isSD = false;

			// Display Time-stamp
			displayDateStamp(isoTimes.get(0), isoTimes.get(isoTimes.size() - 1));
		} catch (DateTimeParseException e) { // SD mode

			System.out.println("SD");
			checkedIsoTime = inputDataXValues.get(0).toString();

			for (Double d : inputDataXValues) {
				checkedIsoTimes.add(d.toString());
			}

			isSD = true;
		}

		// lineChart.getYAxis().setAutoRanging(false); //FIXME

		// Add data to series
		addData(inputDataXValues, inputDataYValues, checkedIsoTime, checkedIsoTimes, isSD);
	}

	/**
	 * A private helper function to 'addDataToSeries' which adds the data loaded from the file to the correct line-chart
	 * series.
	 * 
	 * @param inputDataXValues
	 *            the x values loaded in from the file
	 * @param inputDataYValues
	 *            the y values loaded in from the file
	 * @param checkedIsoTime
	 *            the start value for displaying ISO time of first data point (has been checked if it belongs to the SD
	 *            file or recorded software file)
	 * @param checkedIsoTimes
	 *            the others values for displaying ISO time of all data points (have been checked if it belongs to the
	 *            SD file or recorded software file)
	 * @param isSD
	 *            whether or not the data belongs to an SD file or the recorded software file.
	 */
	private void addData(ArrayList<Double> inputDataXValues, ArrayList<Double> inputDataYValues, String checkedIsoTime,
			ArrayList<String> checkedIsoTimes, boolean isSD) {

		ISOTimeInterval startTime = ISOTimeInterval.parseISOTime(checkedIsoTime);
		readingSeries.getNode().toFront();

		for (int i = 0; i < inputDataXValues.size(); i++) {
			double inputXDataValue = inputDataXValues.get(i);
			double inputYDataValue = inputDataYValues.get(i);

			readingSeries.getData().add(new XYChart.Data<Number, Number>(inputXDataValue, inputYDataValue));

			// Assign indexing to each node.
			Data<Number, Number> dataPoint = readingSeries.getData().get(i);

			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED, event.getDataXYValues(dataPoint, i,
					xDataCoord, yDataCoord, checkedIsoTime, checkedIsoTimes.get(i), isSD));

			dataPoint.getNode().addEventFilter(MouseEvent.MOUSE_EXITED,
					event.clearDataXYValues(xDataCoord, yDataCoord));

			// Update chart bounds if line chart exceeds them.
			ISOTimeInterval endTime = ISOTimeInterval.parseISOTime(checkedIsoTimes.get(i));
			updateChartXBounds(startTime.getDate(), endTime.getDate());

			// Update mask upper boundary
			updateMaskAfterFileLoaded(highMaskBoundarySeries);
			updateMaskAfterFileLoaded(lowMaskBoundarySeries);
		}
	}

	/**
	 * A private helper function to 'addData' which updates any existing upper/lower mask upper boundary
	 * 
	 * @param series
	 *            high/low mask series
	 */
	private void updateMaskAfterFileLoaded(XYChart.Series<Number, Number> series) {
		if (series.getData().size() > 0) {
			series.getData().get(series.getData().size() - 1).setXValue(xAxis.getUpperBound());
		}
	}

	/**
	 * Saves the currently acquired data if data acquisition is paused. An error message will pop up if the data has not
	 * been paused, and the data will not be saved.
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
			saveFileOptions.getExtensionFilters().add(new ExtensionFilter(FILE_FORMAT_TITLE, FILE_FORMAT_EXTENSION));

			File selectedFile = saveFileOptions.showSaveDialog(GuiView.getInstance().getStage());

			if (selectedFile != null) {
				System.out.println("Saving file name in directory: " + selectedFile.getPath());

				// Save the data to a file
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile.getPath()))) {
					System.out.println("SIZES: " + readingSeries.getData().size() + ", " + pausedStoredYUnitData.size()
							+ ", " + pausedStoredISOTimeData.size());
					model.saveColumnData(bw, readingSeries, pausedStoredYUnitData, pausedStoredISOTimeData);
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

			// Reset the plotted data
			resetPlottedData();
		}
	}

	// FIXME
	/**
	 * A private helper function to 'discardData' which clears the plot and resets some connected components
	 */
	private void resetPlottedData() {

		// Clear all things
		multimeterDisplay.setText("");

		resumedDataAcquisition();

		isChanged = false;
		totalAcquisitionData.clear();
		pausedStoredYUnitData.clear();
		pausedStoredISOTimeData.clear();

		resistance = false;
		voltage = false;
		current = false;
		continuity = false;
		logic = false;

		selectACDCBtn.setDisable(true);
		isACMode = false;

		// Reset the plot data
		readingSeries.getData().clear();
		storedISOTimes.clear();

		dataPlotPosition = 0;
		resetAxesGraphDetails();
		storedYUnits.clear();

	}

	/**
	 * A private helper function to 'discardData'. Displays a pop-up message notifying the user that data will be
	 * discarded if they continue.
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
		Optional<ButtonType> result = GuiView.getInstance().alertUser(title, warning, errorType, alertType)
				.showAndWait();

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
	 *            the mouse event to attach this to
	 * @return the stored values of both coordinates
	 */
	private ArrayList<Number> getMouseToChartCoords(MouseEvent mouseEvent) {
		ArrayList<Number> foundCoordinates = new ArrayList<>();

		foundCoordinates.add(getMouseChartCoords(mouseEvent, true)); // Add x
		foundCoordinates.add(getMouseChartCoords(mouseEvent, false)); // Add y

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
			double x = lineChart.getXAxis().sceneToLocal(event.getSceneX(), event.getSceneY()).getX();

			returnedCoord = lineChart.getXAxis().getValueForDisplay(x);

		} else {

			// Get y coordinate
			double y = lineChart.getYAxis().sceneToLocal(event.getSceneX(), event.getSceneY()).getY();

			returnedCoord = lineChart.getYAxis().getValueForDisplay(y);
		}

		return returnedCoord;
	}

	/**
	 * Adds and sorts the elements in the given mask series as well as adds delete and move point functionality.
	 * 
	 * @param series
	 *            the high/low mask boundaries to add the data to
	 * @param coordX
	 *            the x-value of the high/low mask series data point
	 * @param coordY
	 *            the y-value of the high/low mask series data point
	 */
	private void setUpBoundaries(XYChart.Series<Number, Number> series, Number coordX, Number coordY) {

		// Add data to specified series.
		series.getData().add(new XYChart.Data<Number, Number>(coordX, coordY));

		// Sort data by x-axis value
		series.getData().sort(compare.sortChart());

		for (int i = 0; i < series.getData().size(); i++) {
			Data<Number, Number> dataPoint = series.getData().get(i);

			// Changes mouse cursor type
			dataPoint.getNode().addEventHandler(MouseEvent.MOUSE_ENTERED, event.changeCursor(dataPoint));

			// Deletes the point that was clicked
			event.removeMaskDataPoint(dataPoint, series);

			// Moves the point that was hovered over + changes mouse cursor type
			moveData(dataPoint, series);
		}
	}

	/**
	 * When the data-point is moved by the mouse, make sure the user cannot drag it into the other mask area.
	 * 
	 * @param dataPoint
	 *            the data point that is moved.
	 * @param series
	 *            the series the data-point comes from (high/low).
	 */
	protected void moveData(XYChart.Data<Number, Number> dataPoint, XYChart.Series<Number, Number> series) {

		// Keep tabs on the original data
		double originX = dataPoint.getXValue().doubleValue();
		double originY = dataPoint.getYValue().doubleValue();

		dataPoint.getNode().setOnMouseMoved(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (event.isShiftDown()) {

					// Change cursor
					dataPoint.getNode().setCursor(Cursor.HAND);

					// Change position to match the mouse coords
					dataPoint.setXValue(getMouseChartCoords(event, true));
					dataPoint.setYValue(getMouseChartCoords(event, false));

					// Only need to check for low boundary
					if (series.getName().contains("low")) {

						// Make sure first point isn't invalid
						if (lowMaskBoundarySeries.getData().size() == 1
								&& !lineChart.maskTestSinglePointOverlapCheck(lowMaskBoundarySeries.getData().get(0))) {
							dataPoint.setXValue(originX);
							dataPoint.setYValue(originY);
						}

						// Testing if any line segments overlap as a result of the moved data point
						if (!checkingOverlap.testMaskOverlap(lowMaskBoundarySeries, highMaskBoundarySeries)) {
							dataPoint.setXValue(originX);
							dataPoint.setYValue(originY);
						}
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
	 * A private helper function for 'moveData' which updates the displayed x and y positions of the cursor within the
	 * line-chart space.
	 * 
	 * @param mouseEvent
	 *            the mouse event to attach this to.
	 */
	private void updateXYCoordinates(MouseEvent mouseEvent) {
		xCoordValues.setText("X: " + TIME_DECIMAL.format(getMouseChartCoords(mouseEvent, true)));
		yCoordValues.setText("Y: " + MEASUREMENT_DECIMAL.format(getMouseChartCoords(mouseEvent, false)));
	}

	/**
	 * Creates the high/low mask area.
	 * 
	 * @param chartBackground
	 *            the background of the line chart to attach this event to
	 */
	protected void createMaskAreas(Node chartBackground) {

		chartBackground.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				// Left mouse button and at least one of the add mask buttons
				if (event.getButton() == MouseButton.PRIMARY && (isHighBtnSelected || isLowBtnSelected)) {

					// Modify chart behaviour
					lineChart.setAnimated(false);
					yAxis.setAutoRanging(false);

					// Gets the coordinates
					Number coordX = getMouseToChartCoords(event).get(0);
					Number coordY = getMouseToChartCoords(event).get(1);

					if (isHighBtnSelected) { // Set up high boundary

						// No need to check if it overlaps, as lower bound is compared to it.
						setUpBoundaries(highMaskBoundarySeries, coordX, coordY);
					} else {// Set up low boundary

						// Check that there's no overlap before adding new points
						if (checkingOverlap.checkLowHighMaskOverlap(lowMaskBoundarySeries, highMaskBoundarySeries,
								coordX, coordY, lowCounter)) {

							setUpBoundaries(lowMaskBoundarySeries, coordX, coordY);

							lowCounter++;
						}
					}
				}
			}
		});

	}

	/**
	 * Orders the specified series data-points by increasing x-axis values. If a first and last boundary point (i.e. x =
	 * 0, x = 50) haven't been specified, they are created.
	 * 
	 * @param existingSeries
	 *            the mask series to sort
	 * @param currentSeries
	 *            the mask series currently being modified
	 * @return true if boundary points were added without intersection issue, false otherwise
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
		finalBoundaryPoint.setYValue(existingSeries.getData().get(existingSeries.getData().size() - 1).getYValue());

		double lowestXValue = existingSeries.getData().get(0).getXValue().doubleValue();
		double upperXValue = existingSeries.getData().get(existingSeries.getData().size() - 1).getXValue()
				.doubleValue();

		// Add initial boundary point
		if (addingBoundaryPoints(initialBoundaryPoint, lowestXValue, currentSeries, existingSeries)) {

			initialBoundarySuccess = true;

			if (existingSeries.getName().contains("low"))
				lowCounter = existingSeries.getData().size();
		}

		// Add final boundary point
		if (addingBoundaryPoints(finalBoundaryPoint, upperXValue, currentSeries, existingSeries)) {

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

	/**
	 * A private helper function to 'orderAndAddBoundaryPoints' which adds the first and last boundary points (x lower
	 * and x upper) to the mask series.
	 * 
	 * @param boundaryPoint
	 *            the absolute first/last boundary point to be added (0 or x-axis upperbound)
	 * @param xValueExistingData
	 *            the first point in the existing mask series
	 * @param currentSeries
	 *            the mask series which is currently being modified
	 * @param existingSeries
	 *            the mask series which already has been set
	 * @return true if there is no overlap, false if the added point will overlap with existing mask
	 */
	private boolean addingBoundaryPoints(XYChart.Data<Number, Number> boundaryPoint, double xValueExistingData,
			XYChart.Series<Number, Number> currentSeries, XYChart.Series<Number, Number> existingSeries) {

		int insertBoundaryPosition = 0; // the place within the mask series to add the new boundary point
		int newExistingDataPosition = 0; // the new position of the first/last boundary point to be added
		int finalBoundaryDataPosition = 0; // the new position of the original first/last mask series data point
		boolean isLowerBoundary = false; // determining if moving right to left (high) / left to right (low)
		double xBounds = 0D; // upper / lower bounds of the x-axis

		if (boundaryPoint.getXValue().intValue() == 0) { // Lower bound
			System.out.println("LOW B");

			xBounds = 0D;
			insertBoundaryPosition = 0;
			newExistingDataPosition = 0;
			finalBoundaryDataPosition = 1;
			isLowerBoundary = true;
		} else { // Upper bound
			System.out.println("HIGH B");

			xBounds = xAxis.getUpperBound();
			insertBoundaryPosition = existingSeries.getData().size();
			newExistingDataPosition = existingSeries.getData().size() - 1;
			finalBoundaryDataPosition = existingSeries.getData().size() - 2;
			isLowerBoundary = false;
		}

		// If there do not exist first and last points with the x upper and lower bound values, add the boundary points
		if (xValueExistingData != xBounds) {

			// Make sure that you can't add incorrect first elements
			if ((currentSeries.getData().size() > 0)) {
				if (getMax(existingSeries.getData().get(newExistingDataPosition), currentSeries, isLowerBoundary)) {

					existingSeries.getData().add(insertBoundaryPosition, boundaryPoint);
					existingSeries.getData().get(insertBoundaryPosition).getNode().setVisible(false);
				} else {

					GuiView.getInstance().illegalMaskPoint();
					return false;
				}
			} else {
				existingSeries.getData().add(insertBoundaryPosition, boundaryPoint);
				existingSeries.getData().get(insertBoundaryPosition).getNode().setVisible(false);
			}
		} else {

			// Set last y-value of the existing series (boundary point) to match the second last data point
			existingSeries.getData().get(newExistingDataPosition)
					.setYValue(existingSeries.getData().get(finalBoundaryDataPosition).getYValue());
		}
		return true;
	}

	/**
	 * Check that the given data point is less than all the other data points within the mask series.
	 * 
	 * @param dataPoint
	 *            the boundary data point to add
	 * @param existingSeries
	 *            the mask series which already has been set, and to be checked against for potential collision
	 * @param isLowerBoundary
	 *            whether or not the series is the low (true)/high (false) boundary
	 * @return true if there is no collision, false otherwise
	 */
	private boolean getMax(XYChart.Data<Number, Number> dataPoint, XYChart.Series<Number, Number> existingSeries,
			boolean isLowerBoundary) {
		ArrayList<XYChart.Data<Number, Number>> subList = new ArrayList<>();

		System.out.println(dataPoint.toString());

		// Only deal with points to the left or right of the data point
		for (int i = 0; i < existingSeries.getData().size(); i++) {

			if (dataPoint.getXValue().doubleValue() < existingSeries.getData().get(i).getXValue().doubleValue()) {
				if (isLowerBoundary) {
					subList.addAll(existingSeries.getData().subList(0, i));
				} else {
					subList.addAll(existingSeries.getData().subList(i, existingSeries.getData().size() - 1));
				}

				break;
			}
		}

		// Check if there will be a collision with the points from the existing series data
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
	 * Locks in the current boundary mask (high/low). If both mask boundary areas have been selected, then enable the
	 * running of the mask test to occur.
	 */
	@FXML
	private void setMaskBoundary() {
		setBoundariesInStone();
	}

	/**
	 * Locks in the boundary mask areas, orders and adds boundary points.
	 */
	private void setBoundariesInStone() {
		if (isHighBtnSelected && !isLowBtnSelected && (highMaskBoundarySeries.getData().size() > 0)) {
			if (orderAndAddBoundaryPoints(highMaskBoundarySeries, lowMaskBoundarySeries)) {
				event.removeAllListeners(highMaskBoundarySeries);

				setHighBtn.setDisable(true);
				isHighBtnSelected = false;
				setLowBtn.setDisable(false);
			}

		} else if (isLowBtnSelected && !isHighBtnSelected && (lowMaskBoundarySeries.getData().size() > 0)) {

			if (orderAndAddBoundaryPoints(lowMaskBoundarySeries, highMaskBoundarySeries)) {
				event.removeAllListeners(lowMaskBoundarySeries);

				setLowBtn.setDisable(true);
				isLowBtnSelected = false;
			} else {
				System.out.println("NOT YET");
			}
		}

		// Enable running of mask-testing & exporting of mask
		if (setHighBtn.isDisabled() && setLowBtn.isDisabled()) {
			runMaskBtn.setDisable(false);
			maskTestResults.setDisable(false);
			exportMaskBtn.setDisable(false);

			if (!(storedYUnits.size() > 0)) {
				maskVRBtn.setDisable(false);
				maskARBtn.setDisable(false);
				maskORBtn.setDisable(false);
			}

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

		System.out.println("high was selected");
	}

	/**
	 * Flags whether the current mask boundary is the lower bound.
	 */
	@FXML
	private void setLowBoundary() {
		isHighBtnSelected = false;
		isLowBtnSelected = true;

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

		if ((highMaskBoundarySeries.getData().size() > 0) && (lowMaskBoundarySeries.getData().size() > 0)
				&& (readingSeries.getData().size() > 0)) {

			for (int i = 0; i < readingSeries.getData().size() - 1; i++) {

				// Test lower
				errorCounter += maskRunOutcome(readingSeries.getData().get(i), readingSeries.getData().get(i + 1),
						lowMaskBoundarySeries);

				// Test upper
				errorCounter += maskRunOutcome(readingSeries.getData().get(i), readingSeries.getData().get(i + 1),
						highMaskBoundarySeries);
			}

			counter = checkForMaskAreaOverlap(counter);

			// Set outcome to pass or fail
			if ((errorCounter > 0) || (counter > 0)) {
				maskTestResults.setText("------------------------------" + "\n");
				maskTestResults.appendText("TEST FAILED OVER INTERVALS: " + "\n");

				// Display failed intervals
				displayFailedIntervals();
			} else {
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
		int failedRegionStart = 0;

		for (Line2D l : overlappedIntervals) {
			String overlap = ("(" + l.x1 + ", " + l.x2 + ")");
			System.out.println(overlap);
		}

		// Display intervals where overlapping occurred (excluding final region).
		for (int i = 0; i < overlappedIntervals.size() - 1; i++) {
			if (overlappedIntervals.get(i).x2 < overlappedIntervals.get(i + 1).x1) {

				// Get a sublist of the directly failed region
				determineOverlapRegion(overlappedIntervals, failedRegionStart, (i + 1));

				// Change the start of the next overlapped interval
				failedRegionStart = (i + 1);
			}
		}

		// Display intervals where overlapping occurred (including final region).
		determineOverlapRegion(overlappedIntervals, failedRegionStart, overlappedIntervals.size());

		maskTestResults.appendText("------------------------------" + "\n");
		maskTestResults.appendText("FAILED AMOUNT OF TIME: \n");
		maskTestResults.appendText(determineFailedOverlapTime(overlappedIntervals) + "s\n");
		maskTestResults.appendText("------------------------------" + "\n");
	}

	/**
	 * A private helper function to 'displayFailedIntervals' which displays all the regions that were indentified as
	 * invalid.
	 * 
	 * @param overlappedIntervals
	 *            a list of line segments which failed (went into mask region).
	 * @param start
	 *            the element where the sublist should start
	 * @param end
	 *            the element where the sublist should end
	 */
	private void determineOverlapRegion(List<Line2D> overlappedIntervals, int start, int end) {

		// Sublist of the complete list of line segments which failed
		List<Line2D> subOverlappedIntervals = overlappedIntervals.subList(start, end);

		String interval1 = "(" + subOverlappedIntervals.get(0).x1 + ", ";
		String interval2 = subOverlappedIntervals.get(subOverlappedIntervals.size() - 1).x2 + ")";

		maskTestResults.appendText(interval1 + interval2 + "\n");
	}

	/**
	 * A private helper function to 'displayFailedIntervals' which calculates the total amount of time spent in the
	 * failed regions.
	 * 
	 * @param overlappedIntervals
	 *            a list of line segments which failed (went into mask region).
	 * @return the total amount of time spent in the failed regions
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
	 * A private helper function for 'runMaskTest' which determines if any of the loaded points overlaps the mask area
	 * below the line.
	 */
	private int checkForMaskAreaOverlap(int counter) {
		for (int i = 0; i < readingSeries.getData().size() - 1; i++) {
			XYChart.Data<Number, Number> currentDataPoint = readingSeries.getData().get(i);
			XYChart.Data<Number, Number> nextDataPoint = readingSeries.getData().get(i + 1);

			counter += lineChart.maskTestOverlapCheck(true, currentDataPoint, nextDataPoint); // High boundary

			counter += lineChart.maskTestOverlapCheck(false, currentDataPoint, nextDataPoint); // Low boundary
		}

		return counter;
	}

	/**
	 * A private helper function for 'runMaskTest' which checks if there have been collisions and counts them.
	 * 
	 * @param current
	 *            the current point in the series.
	 * @param next
	 *            the next point in the series.
	 * @param existingSeries
	 *            the series the points (current/next) are checking against.
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
						new Point2D(dataPoint.getXValue().floatValue(), dataPoint.getYValue().floatValue()),
						new Point2D(dataPoint2.getXValue().floatValue(), dataPoint2.getYValue().floatValue())))) {
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
		loadFileOptions.getExtensionFilters().add(new ExtensionFilter(FILE_FORMAT_TITLE, FILE_FORMAT_EXTENSION));

		File selectedFile = loadFileOptions.showOpenDialog(GuiView.getInstance().getStage());

		// Only if file exists extract information from it
		if (selectedFile != null) {
			System.out.println("NAME: " + selectedFile.getPath());

			setHighBtn.setDisable(true);
			setMaskBtn.setDisable(true);

			// If you reload files, reload the existing masks
			if (highMaskBoundarySeries.getData().size() > 0) {
				highMaskBoundarySeries.getData().clear();
			}
			if (lowMaskBoundarySeries.getData().size() > 0) {
				lowMaskBoundarySeries.getData().clear();
			}

			// Check if there is a clash of units.
			if (determineUnitClash(selectedFile)) {

				// Display mask data points.
				addMaskDataPoints(selectedFile);

				// Enable running of mask test
				runMaskBtn.setDisable(false);
				maskTestResults.setDisable(false);
			}

		}
	}

	/**
	 * A private helper function for 'importMaskData' which determines if the mask to load in has y-unit values which
	 * clash.
	 * 
	 * @param selectedFile
	 *            the file to grab specific mask data from
	 * @return true if there was no y-unit clash, false otherwise
	 */
	private boolean determineUnitClash(File selectedFile) {
		String yUnitValue = "";

		if (storedYUnits.size() > 0) { // Data has been loaded
			for (String[] column : model.readMaskData(selectedFile.getPath())) {
				yUnitValue = column[3];

				// Check same y-units
				if (!revertModifiedMaskUnit(yUnitValue, storedYUnits.get(0))) {
					errorMessageInvalidMask();
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * A private helper function for 'determineUnitClash' which checks that the y-unit of the mask file correspond to
	 * the y-units of the displayed data.
	 * 
	 * @param maskYUnit
	 *            the mask y-unit values.
	 * @param dataYUnit
	 *            the loaded data y-unit values.
	 * @return true if there's a match, false otherwise.
	 */
	private boolean revertModifiedMaskUnit(String maskYUnit, String dataYUnit) {
		if ((maskYUnit.equals("V") && dataYUnit.contains(maskYUnit))
				|| (maskYUnit.contains("A") && (dataYUnit.contains(maskYUnit)))) {
			System.out.println("Y");
			return true;
		}

		/*
		 * TODO: incorporate resistance else if (maskYUnit.contains(OHM_SYMBOL) && dataYUnit.contains(maskYUnit)) { //
		 * FIXME: find out which symbol will represent ohms return true; }
		 */
		return false;
	}

	/**
	 * Displays an error message if the user has tried to load in a mask file with values that do not match those of the
	 * already loaded data.
	 */
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
				XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(Double.parseDouble(column[1]),
						Double.parseDouble(column[2]));
				tempHighMaskBoundarySeries.getData().add(dataPoint);
			} else {
				XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(Double.parseDouble(column[1]),
						Double.parseDouble(column[2]));
				tempLowMaskBoundarySeries.getData().add(dataPoint);
			}
		}

		// Sort unordered masks.
		tempHighMaskBoundarySeries.getData().sort(compare.sortChart());
		tempLowMaskBoundarySeries.getData().sort(compare.sortChart());

		// Check if mask overlap occurs
		if (checkingOverlap.testMaskOverlap(tempLowMaskBoundarySeries, tempHighMaskBoundarySeries)) {
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
		saveFileOptions.getExtensionFilters().add(new ExtensionFilter(FILE_FORMAT_TITLE, FILE_FORMAT_EXTENSION));

		File selectedFile = saveFileOptions.showSaveDialog(GuiView.getInstance().getStage());

		if (selectedFile != null) {
			String savedUnits = "";

			// Save the data to a file
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile.getPath()))) {

				// Only need one element from yUnit, as they are all the same
				if (storedYUnits.size() > 0) { // multimeter readings data file has been loaded
					savedUnits = modifyMeasurements.modifyMaskUnit(storedYUnits.get(0));
				} else {
					savedUnits = modifyMeasurements.modifyMaskUnit(maskVRBtn, maskARBtn, maskORBtn);
				}

				model.saveMaskData(bw, highMaskBoundarySeries, savedUnits, "high");
				model.saveMaskData(bw, lowMaskBoundarySeries, savedUnits, "low");
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("Saving Data");
		}
	}

	/**
	 * Selects Voltage to save out for mask type
	 */
	@FXML
	private void selectSaveV() {
		selectMaskYUnit(maskARBtn, maskORBtn, maskVRBtn);
	}

	/**
	 * Selects Current to save out for mask type
	 */
	@FXML
	private void selectSaveA() {
		selectMaskYUnit(maskVRBtn, maskORBtn, maskARBtn);
	}

	/**
	 * Selects Resistance to save out for mask type
	 */
	@FXML
	private void selectSaveO() {
		selectMaskYUnit(maskARBtn, maskVRBtn, maskORBtn);
	}

	/**
	 * Determines that only 'V', 'A', 'Ohm' options can be selected at any one time for the mask y-unit value.
	 * 
	 * @param one
	 *            the radio button that is not selected
	 * @param two
	 *            the other radio button that is not selected
	 * @param self
	 *            the radio button that is selected
	 */
	private void selectMaskYUnit(RadioButton one, RadioButton two, RadioButton self) {
		one.setSelected(false);
		two.setSelected(false);

		if (!(one.isSelected() && two.isSelected()) && self.isSelected() == false) {
			self.setSelected(true);
		}
	}

	/**
	 * Re-populates the ports combo-box with all available ports.
	 */
	@FXML
	public void refreshPorts() {
		revertConnectedComponents();
	}

	/**
	 * Binds to the selected port and begins listening for data.
	 */
	@FXML
	private void changePorts() {
		serialTest.selectPort();
	}

	/**
	 * Closes any open ports that were/are being used for serialTest
	 */
	public void quit() {
		this.quit.set(true);
		serialTest.closeOpenPort();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// Refresh the list of available ports
		serialTest.refreshSelectablePortsList();

		// Add elements to the list of brightness levels
		initialiseBrightnessLevels();

		// Add elements to the list of sample rates
		initialiseSampleRate();
	}
}
