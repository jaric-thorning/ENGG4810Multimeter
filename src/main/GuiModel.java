package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javafx.scene.chart.XYChart;

// the format for reading in files <x-value><y-value><y-value units>
public class GuiModel {
	private ArrayList<Integer> readings = new ArrayList<>();

	private static final String DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";
	private static String[] fileHeaders = { "Time", "Value", "Units" };

	private static GuiModel instance;

	public GuiModel() {
		instance = this;
		// TODO: GET DATA FROM SERIAL.
		// TODO: SAVE DATA TO FILE.
	}

	// public static GuiModel getInstance() {
	// return instance;
	// }
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

	// TODO: GET DATA FROM FILE.
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
			String unit) throws IOException {
		
		setupHeader(bufferedWriter);

		for (int i = 0; i < series.getData().size(); i++) {
			writeColumnData(bufferedWriter, series.getData().get(i).getXValue(),
					series.getData().get(i).getYValue(), unit);
		}
	}

	/**
	 * Writes the contents of the csv file after the header.
	 * 
	 * @param bufferedWriter
	 *            the buffered writer needed to write data to the file.
	 * @param tempData
	 *            is a single data value for temperature data values.
	 * @param airData
	 *            is a single data value for windspeed data values.
	 * @param lightData
	 *            is a single data value for the luminosity data values.
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

}
