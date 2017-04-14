package main;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javafx.scene.chart.XYChart;

// the format for reading in files <x-value><y-value><y-value units>
//TODO: MAKE IT SO THAT ONLY DATA OF CURRENT UNIT IS SAVED, SO WHEN THE UNIT CHANGES< THE SAVED DATA CHANGES TOO.
public class GuiModel {
	//private ArrayList<Integer> multimeterReadings = new ArrayList<>();

	private static final String DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";
	private static String[] fileHeaders = { "Time", "Value", "Units" };
	
	DecimalFormat oneDecimal = new DecimalFormat("0.000");
	DecimalFormat timeDecimal = new DecimalFormat("0.0");

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

	/* Clears the data values from the received data */
	public void clearData() {
		System.out.println("YO");
		// for(Integer i : readings) {
		// readings.set(new ArrayList<Integer>());
		// }
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
	 * Save data to a file.
	 * 
	 * @precondition All data samples are the same length.
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file.
	 * @throws IOException
	 *             occurs when there is a problem with the buffered writer.
	 */
	public void saveColumnData(BufferedWriter bufferedWriter, XYChart.Series<Number, Number> series,
			ArrayList<String> unit) throws IOException {
		System.out.println(unit.size());
		System.out.println(series.getData().size());
		setupHeader(bufferedWriter);

		for (int i = 0; i < series.getData().size(); i++) {
			writeColumnData(bufferedWriter, series.getData().get(i).getXValue(),
					series.getData().get(i).getYValue(), unit.get(i));
		}
	}

	/**
	 * Writes the contents of the csv file after the header.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file.
	 * @param timeData
	 *            is a single data value for time data values.
	 * @param readingData
	 *            is a single data value for the voltage/current/resistant values.
	 * @param unitData
	 *            is the unit value of the y-axis values.
	 */
	private void writeColumnData(BufferedWriter bufferedWriter, Number timeData, Number readingData,
			String unitData) {
		try {
			bufferedWriter.write(timeData.toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(readingData.toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(unitData);
			bufferedWriter.write(NEW_LINE_SEPARATOR);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Inserts the headers to be the first row of the csv file.
	 * 
	 * @param bufferedWriter
	 *            - the buffered writer needed to write data to the file.
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

	public void saveMaskData(BufferedWriter bufferedWriter, XYChart.Series<Number, Number> series,
			String unit) throws IOException {

		for (int i = 0; i < series.getData().size(); i++) {
			writeMaskData(bufferedWriter, series.getName(), series.getData().get(i).getXValue(),
					series.getData().get(i).getYValue(), unit);
		}

	}

	private void writeMaskData(BufferedWriter bufferedWriter, String boundaryName, Number xValue,
			Number yValue, String yUnit) {
		try {
			bufferedWriter.write(boundaryName);
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(timeDecimal.format(xValue).toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(oneDecimal.format(yValue).toString());
			bufferedWriter.write(DELIMITER);
			bufferedWriter.write(yUnit);
			bufferedWriter.write(NEW_LINE_SEPARATOR);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String[]> readMaskData(String fileName) {
		ArrayList<String[]> readData = new ArrayList<>();
		String line = "";

		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
			// MAY HAVE MASK FILE HEADER
			// bufferedReader.readLine(); // Get rid of the csv file's first line (column headers)

			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = line.split(DELIMITER);

				// Add each element into the array
				if (tokens.length > 0) {
					readData.add(tokens); // Add series name (high/low)
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

}
