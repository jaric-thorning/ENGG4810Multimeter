package main;

import java.util.ArrayList;

import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.Point2D;

import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;

/**
 * The CheckOverlap class deals with methods that test for different cases of
 * overlap between read in data, masks.
 * 
 * @author dayakern
 *
 */
public class CheckOverlap {

	public CheckOverlap() {

	}

	/**
	 * Checks if overlap between the two mask boundaries has occurred.
	 * 
	 * @param newSeries
	 *            the mask boundary which has not been set yet (low mask
	 *            boundary)
	 * @param existingSeries
	 *            the mask boundary which has already been set (high mask
	 *            boundary)
	 * @return true if there is no overlap, false otherwise
	 */
	public boolean testMaskOverlap(XYChart.Series<Number, Number> newSeries,
			XYChart.Series<Number, Number> existingSeries) {

		if (existingSeries.getData().size() > 1 && newSeries.getData().size() > 1) {
			for (int i = 0; i < existingSeries.getData().size() - 1; i++) {
				for (int j = 0; j < newSeries.getData().size() - 1; j++) {
					Data<Number, Number> currentNDataPoint = newSeries.getData().get(j);
					Data<Number, Number> nextNDataPoint = newSeries.getData().get(j + 1);

					// Create line between current and next new series data
					// points.
					Line2D checkIntersection = new Line2D();
					Point2D currentNPoint = new Point2D(currentNDataPoint.getXValue().floatValue(),
							currentNDataPoint.getYValue().floatValue());
					Point2D nextNPoint = new Point2D(nextNDataPoint.getXValue().floatValue(),
							nextNDataPoint.getYValue().floatValue());
					checkIntersection.setLine(currentNPoint, nextNPoint);

					// Create lines between current and next existing series
					// data points.
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
	 * Determines if the mask point to be added to the lower mask boundary will
	 * not overlap over areas of the high mask boundary area.
	 * 
	 * @param lowMaskSeries
	 *            the low mask boundary series
	 * @param highMaskSeries
	 *            the high mask boundary series
	 * @param coordX
	 *            the x-value of the point to be added.
	 * @param coordY
	 *            the v-value of the point to be added.
	 * @param counter
	 *            keeps track of where the new point is (before/after existing
	 *            point)
	 * @return true if there is no overlap. false otherwise.
	 */
	public boolean checkLowHighMaskOverlap(XYChart.Series<Number, Number> lowMaskSeries,
			XYChart.Series<Number, Number> highMaskSeries, Number coordX, Number coordY, int counter) {

		// Values of new points
		float tempX = coordX.floatValue();
		float tempY = coordY.floatValue();
		Point2D newPoint = new Point2D(tempX, tempY);

		if (lowMaskSeries.getData().size() > 0) {
			ArrayList<Float> existingValues = assignExistingXValue(lowMaskSeries, tempX, counter,
					lowMaskSeries.getData().get(counter - 1).getXValue().floatValue());

			float existingX = existingValues.get(0);
			float existingY = existingValues.get(1);
			Point2D existingPoint = new Point2D(existingX, existingY);

			return checkIntersection(highMaskSeries, existingPoint, newPoint);
		} else if (lowMaskSeries.getData().size() == 0) { // Check first-to-be
															// data point
															// position validity
			Data<Number, Number> firstPoint = new Data<>(coordX, coordY);
			return GuiController.instance.lineChart.maskTestSinglePointOverlapCheck(firstPoint);
		}

		return true;
	}

	/**
	 * A private helper function to 'checkLowHighMaskOverlap' which determines
	 * which direction the line will be in (right to left, or left to right).
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
				tempList.add(currentX);
				tempList.add(currentY);
				return tempList;
			}
		}

		// Add point to the direct left (start of the list)
		if (tempX < compareX) {
			tempList.add(series.getData().get(0).getXValue().floatValue());
			tempList.add(series.getData().get(0).getYValue().floatValue());
		} else {

			// Add point to the direct right (end of the list)
			tempList.add(series.getData().get(counter - 1).getXValue().floatValue());
			tempList.add(series.getData().get(counter - 1).getYValue().floatValue());
		}

		return tempList;
	}

	/**
	 * A private helper function to 'checkLowHighMaskOverlap' which determines
	 * if the point to be added would cause an overlap if added.
	 * 
	 * @param highMaskSeries
	 *            the high mask boundary series
	 * @param existingPoint
	 *            the start point of a to-be-line segment of the low mask series
	 * @param newPoint
	 *            the end point of a to-be-line segment of the low mask series
	 * @return true if there is no intersection (points/lines) false otherwise
	 */
	private boolean checkIntersection(XYChart.Series<Number, Number> highMaskSeries, Point2D existingPoint,
			Point2D newPoint) {

		// Create line to test if new point's line will overlap existing
		Line2D lowBoundaryLineSegment = new Line2D();
		lowBoundaryLineSegment.setLine(existingPoint, newPoint);

		Data<Number, Number> existingDataPoint = new Data<>(existingPoint.x, existingPoint.y);
		Data<Number, Number> newDataPoint = new Data<>(newPoint.x, newPoint.y);

		for (int i = 0; i < highMaskSeries.getData().size() - 1; i++) {

			// Points of the high mask area
			Data<Number, Number> currentDataPoint = highMaskSeries.getData().get(i);
			Data<Number, Number> nextDataPoint = highMaskSeries.getData().get(i + 1);

			Point2D currentPoint = new Point2D(currentDataPoint.getXValue().floatValue(),
					currentDataPoint.getYValue().floatValue());
			Point2D nextPoint = new Point2D(nextDataPoint.getXValue().floatValue(),
					nextDataPoint.getYValue().floatValue());

			// Check if point overlaps
			if (!GuiController.instance.lineChart.maskTestPointsOverlapCheck(existingDataPoint, newDataPoint)) {
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
}
