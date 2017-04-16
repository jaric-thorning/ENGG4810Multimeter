package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javafx.scene.chart.XYChart;

public class GuiModel {
	// private ArrayList<Integer> multimeterReadings = new ArrayList<>();

	private static final String DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	// The format for reading/writing files <x-value><y-value><y-value units>
	private static String[] fileHeaders = { "Time", "Value", "Units" };

	private static final DecimalFormat MEASUREMENT_DECIMAL = new DecimalFormat("0.000");
	private static final DecimalFormat TIME_DECIMAL = new DecimalFormat("0.0");

	private static GuiModel instance;

	public GuiModel() {
		instance = this;
	}

	/**
	 * Enables other classes to access methods within this class, once the program has been
	 * launched.
	 * 
	 * @return an instance of the GuiModel class.
	 */
	public static GuiModel getInstance() {
		if (instance == null) {
			instance = new GuiModel();
			System.out.println("Initialised GuiModel[SINGLETON]");
		}
		return instance;
	}

	/**
	 * Reads the contents of a selected .csv file and returns an array of the read data. If the
	 * filename is not found or there is a problem with the buffered reader, exceptions will be
	 * caught.
	 * 
	 * @param fileName
	 *            the file to read the data from.
	 * @param column
	 *            which column to read from.
	 * @return an array made up of only 1 column's worth of data.
	 */
	public ArrayList<String> readColumnData(String fileName, int column) {
		ArrayList<String> readData = new ArrayList<String>();
		String line = "";

		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
			bufferedReader.readLine(); // Get rid of the csv file's first line (column headers)

			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = line.split(DELIMITER);

				// Add each element into the array
				if (tokens.length > 0) {
					switch (column) {
					case 0:
						readData.add(tokens[column]); // Add x-values (time)
						break;
					case 1:
						readData.add(tokens[column]); // Add y-values (readings)
						break;
					case 2:
						readData.add(tokens[column]); // Add y-value units
						break;
					default:
						System.err.println("Invalid column number supplied");
						break;
					}
				} else {
					System.err.println("There are no elements");
				}
			}
		} catch (FileNotFoundException e1) { // The file name supplied was incorrect.
			e1.printStackTrace();
		} catch (IOException e1) { // There was a problem using the buffered reader.
			e1.printStackTrace();
		}

		return readData;
	}

	/**
	 * Saves data to a given file.
	 * 
	 * @precondition All data samples are the same length.
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file.
	 * @throws IOException
	 *             occurs when there is a problem with the buffered writer.
	 */
	public void saveColumnData(BufferedWriter bufferedWriter, XYChart.Series<Number, Number> series,
			ArrayList<String> yUnits) throws IOException {
		// TODO: REMOVE ME
		// System.out.println(unit.size());
		// System.out.println(series.getData().size());
		setupHeader(bufferedWriter);

		for (int i = 0; i < series.getData().size(); i++) {
			writeColumnData(bufferedWriter, series.getData().get(i).getXValue(),
					series.getData().get(i).getYValue(), yUnits.get(i));
		}
	}

	/**
	 * A private helper function to 'saveColumnData' which inserts the headers to be the first row
	 * of the .csv file.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file.
	 * @throws IOException
	 *             occurs when there is a problem with the buffered writer.
	 */
	private void setupHeader(BufferedWriter bufferedWriter) throws IOException {
		for (int i = 0; i < fileHeaders.length; i++) {
			bufferedWriter.write(fileHeaders[i]);

			if (i != (fileHeaders.length - 1)) {
				bufferedWriter.write(DELIMITER);
			} else {
				bufferedWriter.write(NEW_LINE_SEPARATOR);
			}
		}
	}

	/**
	 * A private helper function to 'saveColumnData' which writes the contents of the .csv file
	 * after the header.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file.
	 * @param xValue
	 *            is a single data value for time data values.
	 * @param yValue
	 *            is a single data value for the voltage/current/resistant values.
	 * @param yUnit
	 *            is the unit value of the y-axis values.
	 */
	private void writeColumnData(BufferedWriter bufferedWriter, Number xValue, Number yValue,
			String yUnit) {
		try {
			bufferedWriter.write(xValue.toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(yValue.toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(yUnit);
			bufferedWriter.write(NEW_LINE_SEPARATOR);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the contents of a selected mask data .csv file and returns an array of the read data.
	 * If the filename is not found or there is a problem with the buffered reader, exceptions will
	 * be caught.
	 * 
	 * @param fileName
	 *            the file to read the mask data from.
	 * @return an array with elements made up of each row of mask data.
	 */
	public ArrayList<String[]> readMaskData(String fileName) {
		ArrayList<String[]> readData = new ArrayList<>();
		String line = "";

		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = line.split(DELIMITER);

				// Add each row into the array.
				if (tokens.length > 0) {
					readData.add(tokens);
				} else {
					System.err.println("There are no elements");
				}
			}
		} catch (FileNotFoundException e1) { // The file name supplied was incorrect.
			e1.printStackTrace();
		} catch (IOException e1) { // There was a problem using the buffered reader.
			e1.printStackTrace();
		}

		return readData;
	}

	/**
	 * Saves mask data to a given file.
	 * 
	 * @precondition All data samples are the same length.
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file.
	 * @throws IOException
	 *             occurs when there is a problem with the buffered writer.
	 */
	public void saveMaskData(BufferedWriter bufferedWriter, XYChart.Series<Number, Number> series,
			String yUnit) throws IOException {

		for (int i = 0; i < series.getData().size(); i++) {
			writeMaskData(bufferedWriter, series.getName(), series.getData().get(i).getXValue(),
					series.getData().get(i).getYValue(), yUnit);
		}

	}

	/**
	 * A private helper function to 'saveMaskData' which writes the contents of the mask .csv file.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file.
	 * @param boundaryName
	 *            the type of boundary (high or low).
	 * @param xValue
	 *            is a single data value for time data values.
	 * @param yValue
	 *            is a single data value for the voltage/current/resistant values.
	 * @param yUnit
	 *            is the unit value of the y-axis values.
	 */
	private void writeMaskData(BufferedWriter bufferedWriter, String boundaryName, Number xValue,
			Number yValue, String yUnit) {
		try {
			bufferedWriter.write(boundaryName);
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(TIME_DECIMAL.format(xValue).toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(MEASUREMENT_DECIMAL.format(yValue).toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(yUnit);
			bufferedWriter.write(NEW_LINE_SEPARATOR);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// TODO: remove me or modify me
	// /* Clears the data values from the received data */
	// public void clearData() {
	// System.out.println("YO");
	// // for(Integer i : readings) {
	// // readings.set(new ArrayList<Integer>());
	// // }
	// }

}
