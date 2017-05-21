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
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * The GuiController class represents the Controller of the Model-View-Controller pattern
 * 
 * @author dayakern
 *
 */
public class GuiController implements Initializable {
	public static GuiController instance;

	private GuiModel model = new GuiModel();
	private DataEvents event = new DataEvents();
	private DataComparator compare = new DataComparator();
	private ModifyMultimeterMeasurements modifyMeasurements = new ModifyMultimeterMeasurements();
	private ISOTimeInterval startTime = null;

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
	GridPane chartGrid;

	/* Components relating to which data to display */
	private int dataPlotPosition = 0;
	protected volatile boolean resistance = false;
	protected volatile boolean voltage = false;
	protected volatile boolean current = false;
	protected volatile boolean continuity = false;
	protected volatile boolean logic = false;

	private volatile boolean isChanged = false;

	/* Components relating to the 'connected' mode */
	@FXML
	protected RadioButton connRBtn;
	@FXML
	private Button pauseBtn;
	private volatile boolean isPaused = false; // Flag for if pauseBtn has been clicked

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

	// Holds x/y coordinates of mouse position relative to line chart background
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
	private RadioButton maskVRBtn;
	@FXML
	private RadioButton maskARBtn;
	@FXML
	private RadioButton maskORBtn;
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

	/* Components relating to mask testing */
	@FXML
	protected TextArea maskTestResults;
	private List<Line2D> overlappedIntervals = new ArrayList<>();

	// To keep track of the previous mask boundary point
	int lowCounter = 0;

	/* Line chart components */
	NumberAxis xAxis = new NumberAxis();
	NumberAxis yAxis = new NumberAxis();
	ModifiedLineChart lineChart;
	Node chartBackground; // Handle on chart background for getting lineChart coordinates

	// Holds the upper & lower boundary points and read data
	private XYChart.Series<Number, Number> highMaskBoundarySeries = new XYChart.Series<>();
	private XYChart.Series<Number, Number> lowMaskBoundarySeries = new XYChart.Series<>();
	private XYChart.Series<Number, Number> readingSeries = new XYChart.Series<>();
	ArrayList<String> storedYUnits = new ArrayList<>(); // The y-unit displayed

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

	/* Components relating to acquired data */
	private ArrayList<Data<Number, Number>> totalAcquisitionData = new ArrayList<>();
	private ArrayList<ISOTimeInterval> storedISOTimes = new ArrayList<>();
	private ArrayList<ISOTimeInterval> pausedStoredISOTimeData = new ArrayList<>();
	private ArrayList<String> pausedStoredYUnitData = new ArrayList<>();

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

	public static double SAMPLES = 2D;
	public static double PER_TIMEFRAME = 1D; // Default second

	private static final String OHM_SYMBOL = Character.toString((char) 8486);

	public GuiController() {
		instance = this;
	}

	/**
	 * Getter method for overlapped mask intervals.
	 * 
	 * @return the overlapped intervals
	 */
	public List<Line2D> getOverlappedIntervals() {
		return overlappedIntervals;
	}

	/**
	 * Getter method for high mask boundary.
	 * 
	 * @return the high mask boundary series
	 */
	public XYChart.Series<Number, Number> getHighSeries() {
		return highMaskBoundarySeries;
	}

	/**
	 * Getter method for lower mask boundary.
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
	 * Decreases the upper and lower bounds of the x-axis. TTo let the user see as much plotted data as they want
	 * instead of just a set amount. Zero is the farthest the user can move the plot left.
	 */
	@FXML
	private void moveXAxisLeft() {

		double newAxisUpperValue = xAxis.getUpperBound() - 1;
		double newAxisLowerValue = xAxis.getLowerBound() - 1;

		if (newAxisLowerValue >= 0) {
			lineChart.updateMaskBoundaries(newAxisUpperValue, newAxisLowerValue);
		}
	}

	/**
	 * Increases the upper and lower bounds of the x-axis. To let the user see as much plotted data as they want instead
	 * of just a set amount.
	 */
	@FXML
	private void moveXAxisRight() {
		double newAxisUpperValue = xAxis.getUpperBound() + 1;
		double newAxisLowerValue = xAxis.getLowerBound() + 1;

		lineChart.updateMaskBoundaries(newAxisUpperValue, newAxisLowerValue);
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
	 * Writes out code to remotely control multimeter's continuity mode
	 */
	@FXML
	private void selectContinuityMode() {
		String code = MultimeterCodes.CONTINUITY.getCode();
		SerialFramework.writeCode(code);

		// if (continuity) {
		// lineChart.setContinuityMode();
		// } else {
		// lineChart.revertContinuityMode();
		// }
		// if (!isContinuityMode) { // Entering Continuity Mode
		// System.out.println("I clicked on continuity mode");
		//
		// isContinuityMode = true;
		// lineChart.setContinuityMode();
		// } else { // Leaving Continuity Mode
		// isContinuityMode = false;
		// lineChart.revertContinuityMode();
		// }
	}

	/**
	 * Executes any continuity mode related events
	 */
	protected void driveContinuity() {
		System.out.println("Driving continuity");

		voltage = false;
		current = false;
		resistance = false;
		continuity = true;
		logic = false;
	}

	/**
	 * Writes out code to remotely control multimeter's logic mode
	 */
	@FXML
	private void selectLogicMode() {
		String code = MultimeterCodes.CONTINUITY.getCode();
		SerialFramework.writeCode(code);
	}

	/**
	 * Executes any logic mode related events
	 */
	protected void driveLogic() {
		System.out.println("Driving Logic");
		// Display LOW or HIGH
		voltage = false;
		current = false;
		resistance = false;
		continuity = false;
		logic = true;
	}

	// FIXME CHECK PORT CONNECTED
	/**
	 * Writes out code to remotely control multimeter. [Select voltage].
	 */
	@FXML
	private void measureVoltage() {
		String code = MultimeterCodes.VOLTAGE.getCode();
		SerialFramework.writeCode(code);
	}

	/**
	 * Executes any voltage mode related events
	 */
	protected void driveVoltage() {
		System.out.println("Driving voltage");
	}

	/**
	 * Writes out code to remotely control multimeter. [Select current].
	 */
	@FXML
	private void measureCurrent() {
		String code = MultimeterCodes.CURRENT.getCode();
		SerialFramework.writeCode(code);
	}

	/**
	 * Executes any current mode related events
	 */
	protected void driveCurrent() {
		System.out.println("Driving current");
	}

	/**
	 * Writes out code to remotely control multimeter. [Select resistance].
	 */
	@FXML
	private void measureResistance() {
		String code = MultimeterCodes.RESISTANCE.getCode();
		SerialFramework.writeCode(code);
	}

	/**
	 * Executes any resistance mode related events.
	 */
	protected void driveResistance() {
		System.out.println("Driving resistance");
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
	 * Updates the data displayed on the line chart, and takes care of any extra data storage/display.
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

		System.out.println("	RS: " + readingSeries.getData().size() + ", " + storedYUnits.size());
		System.out.println("SS: " + storedISOTimes.size());

		// Modify Plot Parts.
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
		// FIXME: STILL NEED TO STORE THE SAMPLES/TIME SOMEWHERE

		// Change multimeter text display according to ranges and values.
		modifyMeasurements.updateYAxisLabel(multimeterDataValue, unit, multimeterDisplay, yAxis);

		// Has been paused as some point
		acquiredDataHasBeenPaused();

		// Add to stored data
		pausedStoredYUnitData.add(modifyMeasurements.getUnitToSave(unit));

		// ISO Time Intervals
		pausedStoredISOTimeData.add(establishISOTime(readingSeries.getData().size()));

		// Normally add received data
		readingSeries.getData().add(
				new XYChart.Data<Number, Number>(dataPlotPosition / (SAMPLES / PER_TIMEFRAME), multimeterDataValue));

		// Update chart bounds
		int dataBoundsRange = (int) Math.ceil(dataPlotPosition / (SAMPLES / PER_TIMEFRAME));
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

			// Reset paused acquired data
			totalAcquisitionData.clear();
			storedYUnits.clear();
			storedISOTimes.clear();
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

		// Modify Plot Parts.
		totalAcquisitionData.add(
				new XYChart.Data<Number, Number>(dataPlotPosition / (SAMPLES / PER_TIMEFRAME), multimeterDataValue));

		// Store total y-units
		storedYUnits.add(modifyMeasurements.getUnitToSave(unit));

		// Store total ISO time intervals
		storedISOTimes.add(establishISOTime(totalAcquisitionData.size()));
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
	 * Calculates the ISO 8601 time interval for the first and last point.
	 */
	private ISOTimeInterval establishISOTime(int dataSize) {
		ISOTimeInterval endTime = null; // Time of each data point

		if (dataPlotPosition == 0) { // Get the start time and initial end time
			LocalDateTime local = LocalDateTime.now();
			startTime = new ISOTimeInterval(local, DateTimeFormatter.ofPattern(ISO_FORMATTER));
			endTime = startTime;
		} else { // Get the end time
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
	 *            the time of the last reading
	 */
	private void displayDateStamp(String startTime, String endTime) {
		recordTimeLabel.setText(startTime + " / " + endTime);
	}

	/**
	 * If the units of the y-values change, then reset the axes bounds.
	 */
	private void resetXAxis() {
		if (voltage || current || resistance) {
			xAxis.setLowerBound(X_LOWER_BOUND);
			xAxis.setUpperBound(X_UPPER_BOUND);
		}
	}

	/**
	 * Selects the connected mode of the GUI if there is a connection, otherwise it's disabled.
	 */
	@FXML
	private void selectConnected() {

		// If there a connection and the radio button is selected
		if (connRBtn.isSelected()) {// && testConnection(connRBtn)) {
			System.out.println("CONNECTED MODE INITIATED");
			System.out.println("//-------------------//");

			yAxis.setAutoRanging(true);
			setupConnectedComponents();
		} else if (!testConnection(connRBtn)) {
			System.out.println("There is no test connection");

			disconnRBtn.setDisable(false);
		} else { // Assuming 'else' just covers when radio button is not selected. TODO: check.

			if (notifyUserConnected()) {
				SerialFramework.closeOpenPort();
				yAxis.setAutoRanging(false);
				disconnRBtn.setDisable(false);

				revertConnectedComponents();

				System.out.println("CONNECTED MODE EXITED");
			} else {
				System.out.println("CONNECTED MODE STAYING");

				connRBtn.setSelected(true);
			}
		}
	}

	// TODO: setup connection test.
	// TODO: make sure we can tell if there is a oneway/twoway connection
	/**
	 * A private helper function to selectConnected. This function is called to determine if there is a connection
	 * (optical link).
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
	 * A private helper function to 'selectConnected' which modifies the status of related components.
	 */
	private void setupConnectedComponents() {

		// Disable the disconnected mode from being editable during
		// connected mode
		disconnRBtn.setDisable(true);
		startTime = null;

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

		// FIXME: SET UP SAMPLES/TIME
		// TODO: SPLIT UP INTO CHECK PORT + NOT CHECKING PORT
		// Receive data
		SerialFramework.selectPort();
		// refreshSelectablePortsList();
	}

	/**
	 * A private helper function to 'selectConnected'. Displays a pop-up message asking the user if they wish to exit
	 * connected mode.
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
		Optional<ButtonType> result = GuiView.getInstance().alertUser(title, warning, errorType, alertType)
				.showAndWait();

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
	 * A private helper function to 'selectConnected' which modifies the status of related components.
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

		isChanged = false;

		totalAcquisitionData.clear();
		pauseBtn.setText("Pause");

		saveBtn.setDisable(true);
		discardBtn.setDisable(true);

		dcRBtn.setSelected(false);
		isDC = false;

		resistance = false;
		voltage = false;
		current = false;
		continuity = false;
		logic = false;

		// Clear all data related things.

		// Reset the plot data
		readingSeries.getData().clear();
		pausedStoredISOTimeData.clear();
		pausedStoredYUnitData.clear();

		dataPlotPosition = 0;
		resetAxes();
		storedYUnits.clear();
		storedISOTimes.clear();

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
				recordTimeLabel.setText("");

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
				revertMaskTestingComponents();
			} else {
				System.out.println("DISCONNECTED MODE STAYING");

				disconnRBtn.setSelected(true);
			}
		}
	}

	/**
	 * Reverts the states of the mask-testing components.
	 */
	private void revertMaskTestingComponents() {
		maskTestingSelected = false;
		isHighBtnSelected = false;
		isLowBtnSelected = false;
		lineChart.setHighBoundarySelected(false);
		lineChart.setLowBoundarySelected(false);

		lowCounter = 0;
		maskTestResults.clear();

		runMaskBtn.setDisable(true);
		maskTestResults.setDisable(true);
		exportMaskBtn.setDisable(true);
		maskVRBtn.setVisible(false);
		maskARBtn.setVisible(false);
		maskORBtn.setVisible(false);

		setLowBtn.setDisable(true);

		highMaskBoundarySeries.getData().clear();
		lowMaskBoundarySeries.getData().clear();
		readingSeries.getData().clear();
		overlappedIntervals.clear();

		storedYUnits.clear(); // FIXME

		System.out.println(isHighBtnSelected);
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

		// Also reset the displayed chart coordinate values.
		xCoordValues.setText("X: ");
		yCoordValues.setText("Y: ");
		recordTimeLabel.setText("");
	}

	/**
	 * A private helper function to 'selectDisconnected'. Displays a pop-up message asking the user if they wish to exit
	 * disconnected mode.
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
		Optional<ButtonType> result = GuiView.getInstance().alertUser(title, warning, errorType, alertType)
				.showAndWait();

		if (result.get() == ButtonType.OK) { // User was OK exiting disconnected mode
			return true;
		} else { // User was not OK exiting disconnected mode (cancelled or closed dialog box)
			return false;
		}
	}

	// TODO: FIX ME WITH DISCARDED DATA.
	/**
	 * Pauses the displayed acquired data.
	 */
	@FXML
	private void pauseDataAcquisition() {

		if (!isPaused) {// FIXME?
			System.out.println("DATA IS PAUSED");
			// System.out
			// .println("PAUSED RS: " + readingSeries.getData().size() + ", " + yUnits.size());

			isPaused = true;
			pauseBtn.setText("Unpause");

			// Disable multimeter components
			multimeterDisplay.setDisable(true);
			voltageBtn.setDisable(true);
			currentBtn.setDisable(true);
			resistanceBtn.setDisable(true);
			dcRBtn.setDisable(true);
			multimeterDisplay.setDisable(true);
			modeLabel.setDisable(true);
			logicBtn.setDisable(true);
			continuityBtn.setDisable(true);
		} else {
			System.out.println("DATA IS UNPAUSED");

			resume();
		}
	}

	/**
	 * Enable multimeter components when pause button has been clicked on again, and pause is now resumed
	 */
	private void resume() {
		isPaused = false;
		pauseBtn.setText("Pause");

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

			// FIXME: make sure auto-ranging is set true/false in right places
			yAxis.setAutoRanging(true);

			// Display data + plot behaviours
			addDataToSeries(inputDataXValues, inputDataYValues, inputDataIsoTime);
		} else {
			System.err.println("File doesn't exist");
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
	 * (readingSeries) as well as determine which ISO display behaviour to use (files from SD card and from recorded
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
			int dataBoundsRange = (int) Math.ceil(i / SAMPLES);
			if (dataBoundsRange > X_UPPER_BOUND) {
				xAxis.setLowerBound(dataBoundsRange - X_UPPER_BOUND);
				xAxis.setUpperBound(dataBoundsRange);
			}
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

	// FIXME: make sure that everything gets reset.
	/**
	 * Discards all saved data and starts the plot again.
	 */
	@FXML
	private void discardData() {
		if (notifyDiscardingData()) {

			// Reset the plot data
			revert();

			System.out.println("DATA DISCARDED");
		} else {
			System.out.println("DATA NOT DISCARDED");
		}
	}

	// FIXME: MODULARSE THIS WITH REVERTCONNECTEDCOMPONENTS
	private void revert() {
		// FIXME: not sure if I control these
		// -------------------------
		dcRBtn.setSelected(false);
		isDC = false;
		// -------------------------

		// Clear all things
		multimeterDisplay.setText("");

		resume();

		isChanged = false;
		totalAcquisitionData.clear();
		pausedStoredYUnitData.clear();
		pausedStoredISOTimeData.clear();

		resistance = false;
		voltage = false;
		current = false;
		continuity = false;
		logic = false;

		// Reset the plot data
		readingSeries.getData().clear();
		storedISOTimes.clear();

		dataPlotPosition = 0;
		resetAxes();
		storedYUnits.clear();

		xDataCoord.setText("X: ");
		yDataCoord.setText("Y: ");
		recordTimeLabel.setText("");
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
	 * TODO Sets up the specified mask
	 * 
	 * @param series
	 *            the high/low mask boundaries to modify.
	 * @param coordX
	 * @param coordY
	 */
	private void setUpBoundaries(XYChart.Series<Number, Number> series, Number coordX, Number coordY) {

		// Add data to specified series.
		series.getData().add(new XYChart.Data<Number, Number>(coordX, coordY));

		// Sort data by x-axis value
		series.getData().sort(compare.sortChart());

		// Modified the for loop for IDing the line chart data points from:
		// https://gist.github.com/TheItachiUchiha/c0ae68ef8e6273a7ac10
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
	 * TODO
	 * 
	 * @param newSeries
	 * @param existingSeries
	 * @return
	 */
	private boolean testOverlapPoint(XYChart.Series<Number, Number> newSeries,
			XYChart.Series<Number, Number> existingSeries) {

		System.out.println("-------------------------");
		if (existingSeries.getData().size() > 1 && newSeries.getData().size() > 1) { // FIXME
			for (int i = 0; i < existingSeries.getData().size() - 1; i++) {
				for (int j = 0; j < newSeries.getData().size(); j++) {
					Data<Number, Number> currentNDataPoint = newSeries.getData().get(j);

					// Get current point of 'newSeries'
					Point2D currentNPoint = new Point2D(currentNDataPoint.getXValue().floatValue(),
							currentNDataPoint.getYValue().floatValue());

					// Create lines between current and next existing series data points.
					Data<Number, Number> currentDataPoint = existingSeries.getData().get(i);
					Data<Number, Number> nextDataPoint = existingSeries.getData().get(i + 1);

					Point2D existingCurrentPoint = new Point2D(currentDataPoint.getXValue().floatValue(),
							currentDataPoint.getYValue().floatValue());
					Point2D existingNextPoint = new Point2D(nextDataPoint.getXValue().floatValue(),
							nextDataPoint.getYValue().floatValue());

					// Determine if the new series point overlaps onto the exisiting series line.
					if (!determineCollinearness(currentNPoint, existingCurrentPoint, existingNextPoint)) {

						GuiView.getInstance().illegalMaskPoint();

						return false;
					}
				}
			}
		} else if (newSeries.getData().size() == 1) {
			System.out.println("YO;");
			Point2D firstPoint = new Point2D(newSeries.getData().get(0).getXValue().floatValue(),
					newSeries.getData().get(0).getYValue().floatValue());
			return checkSinglePointIntersection(firstPoint);
		}
		System.out.println("-------------------------");
		return true;
	}

	/**
	 * A private function helper for 'addMaskDataPoints' & 'moveData' which checks if overlap between the two mask
	 * boundaries has occurred.
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

					// Create line between current and next new series data points.
					Line2D checkIntersection = new Line2D();
					Point2D currentNPoint = new Point2D(currentNDataPoint.getXValue().floatValue(),
							currentNDataPoint.getYValue().floatValue());
					Point2D nextNPoint = new Point2D(nextNDataPoint.getXValue().floatValue(),
							nextNDataPoint.getYValue().floatValue());
					checkIntersection.setLine(currentNPoint, nextNPoint);

					// Create lines between current and next existing series data points.
					Data<Number, Number> currentDataPoint = existingSeries.getData().get(i);
					Data<Number, Number> nextDataPoint = existingSeries.getData().get(i + 1);

					Point2D existingCurrentPoint = new Point2D(currentDataPoint.getXValue().floatValue(),
							currentDataPoint.getYValue().floatValue());
					Point2D existingNextPoint = new Point2D(nextDataPoint.getXValue().floatValue(),
							nextDataPoint.getYValue().floatValue());

					// Overlaps
					if (checkIntersection.intersectsLine(new Line2D(existingCurrentPoint, existingNextPoint))) {

						GuiView.getInstance().illegalMaskPoint();
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * A private function for 'testOverlapPoint' which determines if the moved data point collides with any line
	 * segments of the existing series.
	 * 
	 * @param newPoint
	 *            the point selected and moved.
	 * @param existingPointStart
	 *            the points which are start of the line segment
	 * @param existingPointEnd
	 *            the points which are end of the line segment
	 * @return true if there is no collision, false otherwise
	 */
	private boolean determineCollinearness(Point2D newPoint, Point2D existingPointStart, Point2D existingPointEnd) {

		// Took collinear formula from: http://www.math-for-all-grades.com/Collinear-points.html
		float a = existingPointStart.x - newPoint.x;
		float b = newPoint.x - existingPointEnd.x;
		float c = existingPointStart.y - newPoint.y;
		float d = newPoint.y - existingPointEnd.y;

		float ad = a * d;
		float bc = b * c;

		float gradient = (1F / 2F) * (ad - bc);

		System.out.println(gradient);
		// The point is more or less collinear [taking into account float decimals]
		if (gradient < 1 && gradient > -1) {
			return false;
		}

		return true;
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

					// Change position to match the mouse coords.
					dataPoint.setXValue(getMouseChartCoords(event, true));
					dataPoint.setYValue(getMouseChartCoords(event, false));

					// Testing if moved point overlapped onto the line
					if (!testOverlapPoint(lowMaskBoundarySeries, highMaskBoundarySeries)) {
						dataPoint.setXValue(originX);
						dataPoint.setYValue(originY);
					}

					// Testing if any line segments overlap as a result of the moved data point
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
	 * TODO
	 * 
	 * @param chartBackground
	 *            the background of the line chart to attach this event to
	 */
	protected void createHighLowBoundaryAreas(Node chartBackground) {

		chartBackground.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				System.out.println("C: " + lowCounter);

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
						// Check that no overlap before adding new points
						if (checkOverlap(coordX, coordY, lowCounter)) {

							setUpBoundaries(lowMaskBoundarySeries, coordX, coordY);

							lowCounter++;
						}
					}
				}
			}
		});

	}

	/**
	 * Determines if the mask point to be added to the lower mask boundary will not overlap over areas of the high mask
	 * boundary area.
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
		Point2D newPoint = new Point2D(tempX, tempY);

		if (lowMaskBoundarySeries.getData().size() > 0) {
			ArrayList<Float> existingValues = assignExistingXValue(lowMaskBoundarySeries, tempX, counter,
					lowMaskBoundarySeries.getData().get(counter - 1).getXValue().floatValue());

			float existingX = existingValues.get(0);
			float existingY = existingValues.get(1);

			Point2D existingPoint = new Point2D(existingX, existingY);

			// System.out.println(highMaskBoundarySeries.getData().toString());

			return checkLineIntersection(existingPoint, newPoint);
		} else if (lowMaskBoundarySeries.getData().size() == 0) {
			System.out.println("YO;");
			return checkSinglePointIntersection(newPoint);
		}

		return true;
	}

	/**
	 * A private helper function for 'checkOverlap' which determines if the first low mask series placed on the chart
	 * overlaps with any of the existing mask series (high)
	 * 
	 * @param newPoint
	 *            the point to be added
	 * @return true if there is no overlap, false otherwise
	 */
	private boolean checkSinglePointIntersection(Point2D newPoint) {
		for (int i = 0; i < highMaskBoundarySeries.getData().size() - 1; i++) {

			// Points of the high mask area
			Data<Number, Number> currentDataPoint = highMaskBoundarySeries.getData().get(i);
			Data<Number, Number> nextDataPoint = highMaskBoundarySeries.getData().get(i + 1);

			Point2D currentPoint = new Point2D(currentDataPoint.getXValue().floatValue(),
					currentDataPoint.getYValue().floatValue());
			Point2D nextPoint = new Point2D(nextDataPoint.getXValue().floatValue(),
					nextDataPoint.getYValue().floatValue());

			// Check if point overlaps
			if (!determineCollinearness(newPoint, currentPoint, nextPoint)) {
				GuiView.getInstance().illegalMaskPoint();
				return false;
			}
		}

		return true;
	}

	/**
	 * A private helper function to 'checkOverlap' which determines if the point to be added would cause an overlap if
	 * added.
	 * 
	 * @param existingPoint
	 *            the start point of a to-be-line segment of the low mask series
	 * @param newPoint
	 *            the end point of a to-be-line segment of the low mask series
	 * @return true if there is no overlap, false otherwise.
	 */
	private boolean checkLineIntersection(Point2D existingPoint, Point2D newPoint) {

		// Create line to test if new point's line will overlap existing
		Line2D lowBoundaryLineSegment = new Line2D();
		lowBoundaryLineSegment.setLine(existingPoint, newPoint);

		for (int i = 0; i < highMaskBoundarySeries.getData().size() - 1; i++) {

			// Points of the high mask area
			Data<Number, Number> currentDataPoint = highMaskBoundarySeries.getData().get(i);
			Data<Number, Number> nextDataPoint = highMaskBoundarySeries.getData().get(i + 1);

			Point2D currentPoint = new Point2D(currentDataPoint.getXValue().floatValue(),
					currentDataPoint.getYValue().floatValue());
			Point2D nextPoint = new Point2D(nextDataPoint.getXValue().floatValue(),
					nextDataPoint.getYValue().floatValue());

			// Check if point overlaps
			if (!determineCollinearness(newPoint, currentPoint, nextPoint)) {
				System.out.println("ILLEAGE MOVE");
				GuiView.getInstance().illegalMaskPoint();
				return false;
			}

			// Check if line segment overlaps
			Line2D test = new Line2D(currentPoint, nextPoint);
			if (lowBoundaryLineSegment.intersectsLine(test)) {

				// Warning message
				GuiView.getInstance().illegalMaskPoint();
				return false;
			}
		}

		return true;
	}

	/**
	 * A private helper function to 'checkOverlap' which determines which direction the line will be in (right to left,
	 * or left to right).
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
	private ArrayList<Float> assignExistingXValue(XYChart.Series<Number, Number> series, float tempX, int counter,
			float compareX) {
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

			// RESET MASK COMPONENTS
			importMaskBtn.setDisable(false);
			setHighBtn.setDisable(false);
			setMaskBtn.setDisable(false);

			revertMaskTestingComponents();
			System.out.println("MASK TESTING DE-SELECTED");
		}
	}

	/**
	 * Orders the specified series data-points by increasing x-axis values. If a first and last boundary point (i.e. x =
	 * 0, x = 50) haven't been specified, they are created.
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
		finalBoundaryPoint.setYValue(existingSeries.getData().get(existingSeries.getData().size() - 1).getYValue());

		double lowerXAxisBound = existingSeries.getData().get(0).getXValue().doubleValue();
		double upperXAxisBound = existingSeries.getData().get(existingSeries.getData().size() - 1).getXValue()
				.doubleValue();
		double upperBound = xAxis.getUpperBound();

		// Add initial boundary point
		if (addingBoundaryPoints(initialBoundaryPoint, lowerXAxisBound, 0.0D, currentSeries, existingSeries, 0, 0, 1,
				true)) {

			initialBoundarySuccess = true;

			if (existingSeries.getName().contains("low"))
				lowCounter = existingSeries.getData().size();
		}

		// Add final boundary point
		if (addingBoundaryPoints(finalBoundaryPoint, upperXAxisBound, upperBound, currentSeries, existingSeries,
				existingSeries.getData().size(), existingSeries.getData().size() - 1,
				existingSeries.getData().size() - 2, false)) {

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
	 * A private helper function to 'orderAndAddBoundaryPoints' which adds the first and last boundary points (x lower
	 * and x upper) to the mask series if they do not overlap when set.
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
	private boolean addingBoundaryPoints(XYChart.Data<Number, Number> boundaryPoint, double start, double end,
			XYChart.Series<Number, Number> currentSeries, XYChart.Series<Number, Number> exisistingSeries, int position,
			int newPos, int finalPos, boolean whichWay) {

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
			exisistingSeries.getData().get(newPos).setYValue(exisistingSeries.getData().get(finalPos).getYValue());
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
	private boolean getMax(XYChart.Data<Number, Number> dataPoint, XYChart.Series<Number, Number> series,
			boolean whichWay) {
		ArrayList<XYChart.Data<Number, Number>> subList = new ArrayList<>();

		System.out.println(dataPoint.toString());

		// Only deal with points to the left of the first series data point
		for (int i = 0; i < series.getData().size(); i++) {

			if (dataPoint.getXValue().doubleValue() < series.getData().get(i).getXValue().doubleValue()) {
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
	 * Locks in the current boundary mask (high/low). If both mask boundary areas have been selected, then enable the
	 * running of the mask test to occur.
	 */
	@FXML
	private void setMaskBoundary() {
		if (isHighBtnSelected && !isLowBtnSelected && (highMaskBoundarySeries.getData().size() > 0)) {
			if (orderAndAddBoundaryPoints(highMaskBoundarySeries, lowMaskBoundarySeries)) {
				event.removeAllListeners(highMaskBoundarySeries);

				setHighBtn.setDisable(true);
				isHighBtnSelected = false;
				lineChart.setHighBoundarySelected(false);
				setLowBtn.setDisable(false);
			}

		} else if (isLowBtnSelected && !isHighBtnSelected && (lowMaskBoundarySeries.getData().size() > 0)) {

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

			if (!(storedYUnits.size() > 0)) {
				maskVRBtn.setVisible(true);
				maskARBtn.setVisible(true);
				maskORBtn.setVisible(true);
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
	 * A private helper function to 'displayFailedIntervals' which displays all the regions that are invalid.
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

			// FIXME? If you reload files, reload the units
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
				|| (maskYUnit.contains("A") && (dataYUnit.contains(maskYUnit)))) { // mA
			System.out.println("Y");
			return true;
		}

		/*
		 * TODO: incoroporate resistance else if (maskYUnit.contains(OHM_SYMBOL) && dataYUnit.contains(maskYUnit)) { //
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
	 *            other option that's not self or two
	 * @param two
	 *            other option that's not self or one
	 * @param self
	 *            the radio button in question
	 */
	private void selectMaskYUnit(RadioButton one, RadioButton two, RadioButton self) {
		one.setSelected(false);
		two.setSelected(false);

		if (!(one.isSelected() && two.isSelected()) && self.isSelected() == false) {
			self.setSelected(true);
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// TODO: DO A PING TEST TO SEE IF THERE'S A CONNECTION.
		testConnection(connRBtn);

		initialiseSampleRate();
	}
}
