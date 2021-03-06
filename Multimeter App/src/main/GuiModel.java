package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javafx.scene.chart.XYChart;

/**
 * The GuiModel class represents the Model of the Model-View-Controller pattern
 * 
 * @author dayakern
 *
 */
public class GuiModel {
	private static final String DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	// The format for reading/writing files <x-value><y-value><y-value
	// units><recorded time>
	private static final String[] FILE_HEADERS = { "Time", "Value", "Units", "IsoTime" };

	private static final DecimalFormat MEASUREMENT_DECIMAL = new DecimalFormat("0.000");
	private static final DecimalFormat TIME_DECIMAL = new DecimalFormat("0.0");

	public GuiModel() {
	}

	/**
	 * Reads the contents of a selected .csv file and returns an array of the
	 * read data. If the filename is not found or there is a problem with the
	 * buffered reader, exceptions will be caught.
	 * 
	 * @param fileName
	 *            the file to read the data from
	 * @param column
	 *            which column to read from
	 * @return an array made up of only 1 column's worth of data
	 */
	public ArrayList<String> readColumnData(String fileName, int column) {
		ArrayList<String> readData = new ArrayList<String>();
		String line = "";

		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
			bufferedReader.readLine(); // Get rid of the csv file's first line
										// (column headers)

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
					case 3:
						readData.add(tokens[column]); // Add iso units
						break;
					default:
						System.err.println("Invalid column number supplied");
						break;
					}
				} else {
					System.err.println("There are no elements");
				}
			}
		} catch (FileNotFoundException e1) { // The file name supplied was
												// incorrect
			e1.printStackTrace();
		} catch (IOException e1) { // There was a problem using the buffered
									// reader
			e1.printStackTrace();
		}

		return readData;
	}

	/**
	 * Saves data to a given file.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file
	 * @param series
	 *            the x/y data to save
	 * @param yUnits
	 *            the y-value units
	 * @param isoTimes
	 *            the times each data point was displayed
	 * @throws IOException
	 *             occurs when there is a problem with the buffered writer
	 */
	public void saveColumnData(BufferedWriter bufferedWriter, XYChart.Series<Number, Number> series,
			ArrayList<String> yUnits, ArrayList<ISOTimeInterval> isoTimes) throws IOException {

		// TODO: REMOVE ME check that yUnits is always the correct value...
		System.out.println(yUnits.size() + ", " + series.getData().size() + ", " + isoTimes.size());

		// Add a file header
		setupHeader(bufferedWriter);

		// Write out data
		for (int i = 0; i < series.getData().size(); i++) {
			writeColumnData(bufferedWriter, series.getData().get(i).getXValue(), series.getData().get(i).getYValue(),
					yUnits.get(i), isoTimes.get(i));
		}
	}

	/**
	 * A private helper function to 'saveColumnData' which inserts the headers
	 * to be the first row of the .csv file.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file
	 * @throws IOException
	 *             occurs when there is a problem with the buffered writer
	 */
	private void setupHeader(BufferedWriter bufferedWriter) throws IOException {
		for (int i = 0; i < FILE_HEADERS.length; i++) {
			bufferedWriter.write(FILE_HEADERS[i]);

			if (i != (FILE_HEADERS.length - 1)) {
				bufferedWriter.write(DELIMITER);
			} else {
				bufferedWriter.write(NEW_LINE_SEPARATOR);
			}
		}
	}

	/**
	 * A private helper function to 'saveColumnData' which writes the contents
	 * of the .csv file after the header.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file
	 * @param xValue
	 *            is a single data value for time data values
	 * @param yValue
	 *            is a single data value for the voltage/current/resistant
	 *            values
	 * @param yUnit
	 *            is the unit value of the y-axis values
	 * @param isoTime
	 *            is the iso time value of when the x/y point was displayed
	 */
	private void writeColumnData(BufferedWriter bufferedWriter, Number xValue, Number yValue, String yUnit,
			ISOTimeInterval isoTime) {
		try {
			bufferedWriter.write(xValue.toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(yValue.toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(yUnit);
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(isoTime.toString());
			bufferedWriter.write(NEW_LINE_SEPARATOR);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the contents of a selected mask data .csv file and returns an array
	 * of the read data. If the filename is not found or there is a problem with
	 * the buffered reader, exceptions will be caught.
	 * 
	 * @param fileName
	 *            the file to read the mask data from
	 * @return an array with elements made up of each row of mask data
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
		} catch (FileNotFoundException e1) { // The file name supplied was
												// incorrect
			e1.printStackTrace();
		} catch (IOException e1) { // There was a problem using the buffered
									// reader
			e1.printStackTrace();
		}

		return readData;
	}

	/**
	 * Saves mask data to a given file.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file
	 * @param series
	 *            the mask boundary area which holds the x/y data to save
	 * @param yUnit
	 *            the y-value units
	 * @param seriesName
	 *            name of mask boundary area (to determine which points belong
	 *            to which mask)
	 * @throws IOException
	 *             occurs when there is a problem with the buffered writer
	 */
	public void saveMaskData(BufferedWriter bufferedWriter, XYChart.Series<Number, Number> series, String yUnit,
			String seriesName) throws IOException {

		// Write out mask data
		for (int i = 0; i < series.getData().size(); i++) {
			writeMaskData(bufferedWriter, seriesName, series.getData().get(i).getXValue(),
					series.getData().get(i).getYValue(), yUnit);
		}

	}

	/**
	 * A private helper function to 'saveMaskData' which writes the contents of
	 * the mask .csv file.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file
	 * @param boundaryName
	 *            the type of boundary (high or low)
	 * @param xValue
	 *            is a single data value for time data values
	 * @param yValue
	 *            is a single data value for the voltage/current/resistant
	 *            values
	 * @param yUnit
	 *            is the unit value of the y-axis values
	 */
	private void writeMaskData(BufferedWriter bufferedWriter, String boundaryName, Number xValue, Number yValue,
			String yUnit) {
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
}
