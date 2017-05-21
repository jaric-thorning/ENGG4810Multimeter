package main;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Polygon;

/**
 * The DataEvents class deals with events (i.e. mouse events) that affect the data points of the multimeter values or
 * high/low mask boundaries.
 * 
 * @author dayakern
 *
 */
public class DataEvents {
	private static final DecimalFormat MEASUREMENT_DECIMAL = new DecimalFormat("0.000");

	public DataEvents() {
	}

	// TODO: ADD FOR CONNECTED MODE
	/**
	 * An event handler for displaying the x/y values of the plotted data point when the mouse enters the data point
	 * 
	 * @param dataPoint
	 *            the data point to attach this event to
	 * @param index
	 *            where the data point is located within the list
	 * @param xDataCoord
	 *            the GUI label to display the different x values
	 * @param yDataCoord
	 *            the GUI label to display the different y values
	 * @param startTime
	 *            when the first data point was recorded
	 * @param endTime
	 *            when the last data point was recorded
	 * @param isSD
	 *            whether or not the data points have come from the software or the SD card
	 * @return the event handler
	 */
	protected EventHandler<MouseEvent> getDataXYValues(XYChart.Data<Number, Number> dataPoint, int index,
			Label xDataCoord, Label yDataCoord, String startTime, String endTime, boolean isSD) {
		EventHandler<MouseEvent> getValues = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				if (!isSD) { // Loaded from saved software-recorded file
					LocalDateTime startDurationTime = LocalDateTime.parse(startTime);
					LocalDateTime endDurationTime = LocalDateTime.parse(endTime.toString());
					Duration duration = Duration.between(startDurationTime, endDurationTime);

					xDataCoord.setText("X: Sample: " + (index + 1) + " at " + endTime.toString() + ", "
							+ Duration.parse(duration.toString()));

				} else { // Loaded from SD card file
					Long timeDurationInMillis = (long) ((Double.parseDouble(endTime) * 1000)
							- (Double.parseDouble(startTime) * 1000));
					Duration duration = Duration.ofMillis(timeDurationInMillis);
					String isoDurationFormat = duration.toString();

					xDataCoord.setText("X: Sample: " + (index + 1) + ", " + Duration.parse(isoDurationFormat));
				}

				yDataCoord.setText("Y: " + MEASUREMENT_DECIMAL.format(dataPoint.getYValue()));
			}
		};
		return getValues;
	}

	/**
	 * An event handler for clearing the displayed x/y values of plotted data when the mouse exits the data point.
	 * 
	 * @param xDataCoord
	 *            the GUI label which displays the x values
	 * @param yDataCoord
	 *            the GUI label which displays the y values
	 * @return the event handler
	 */
	protected EventHandler<MouseEvent> clearDataXYValues(Label xDataCoord, Label yDataCoord) {
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
	 * An event handler to change the cursor type when hovering over a data point to help identify that these objects
	 * can be interacted with.
	 * 
	 * @param dataPoint
	 *            the data point to attach this event to
	 * @return the event handler
	 */
	protected EventHandler<MouseEvent> changeCursor(XYChart.Data<Number, Number> dataPoint) {
		EventHandler<MouseEvent> changeCursorType = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				dataPoint.getNode().setCursor(Cursor.HAND);
			}
		};

		return changeCursorType;
	}

	/**
	 * Deals with the deletion of data points from the high/low mask series (when the user has right clicked).
	 * 
	 * @param dataPoint
	 *            the data point to remove
	 * @param series
	 *            the specified high/low mask series which holds the to-be-removed data point
	 */
	protected void removeMaskDataPoint(XYChart.Data<Number, Number> dataPoint, XYChart.Series<Number, Number> series) {
		dataPoint.getNode().setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				// User has right clicked
				if (event.getButton() == MouseButton.SECONDARY) {
					series.getData().remove(dataPoint);

					// FIXME: find out what this actually does
					// Modify the 'book-mark' of the low mask series
					if (series.getName().contains("low")) {
						GuiController.instance.lowCounter--;
					}
				}
			}
		});
	}

	/**
	 * Deals with the removal of mouse event listeners on the data points of the specified series.
	 * 
	 * @param series
	 *            the specified high/low mask series which needs to have it's data points cleared of specific mouse
	 *            event listeners
	 */
	protected void removeAllListeners(XYChart.Series<Number, Number> series) {
		for (XYChart.Data<Number, Number> dataPoint : series.getData()) {

			// Remove the option to delete the data points
			dataPoint.getNode().setOnMouseClicked(null);

			// Remove the option to move the data points
			dataPoint.getNode().setOnMouseMoved(null);
		}
	}

	/**
	 * Creates an event handler to deal with the event when the user clicks on any space of the opposite mask boundary
	 * area.
	 * 
	 * @param polygon
	 *            the polygon that was clicked
	 * @param isLowBoundary
	 *            whether or not the mask area in question is the low mask boundary area
	 * @return the event handler
	 */
	protected EventHandler<MouseEvent> addWarning(Polygon polygon, boolean isLowBoundary) {
		EventHandler<MouseEvent> onMouseClick = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (isLowBoundary) { // Only error under specific conditions

					// Display warning
					GuiView.getInstance().illegalMaskPoint();
				}
			}

		};
		return onMouseClick;
	}
}
