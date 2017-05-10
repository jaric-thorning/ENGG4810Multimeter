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

public class EventHandlers {
	private static final DecimalFormat MEASUREMENT_DECIMAL = new DecimalFormat("0.000");
	// private static final DecimalFormat TIME_DECIMAL = new DecimalFormat("0.0");

	public EventHandlers() {
		// I AM EMPTY :(
	}

	// TODO: Make sure the formatting is correct
	/**
	 * An event handler for displaying the x,y values of the node when it's passed over
	 * 
	 * @param dataPoint
	 *            the data-point to attach this event to.
	 * @param index
	 *            where the data-point is within the array.
	 * @return the event handler.
	 */
	protected EventHandler<MouseEvent> getDataXYValues(XYChart.Data<Number, Number> dataPoint,
			int index, Label xDataCoord, Label yDataCoord, String startTime, String endTime,
			boolean isSD) {
		EventHandler<MouseEvent> getValues = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				if (!isSD) { // Loaded from saved software-recorded file
					LocalDateTime startDurationTime = LocalDateTime.parse(startTime);
					LocalDateTime endDurationTime = LocalDateTime.parse(endTime.toString());
					Duration duration = Duration.between(startDurationTime, endDurationTime);

					xDataCoord.setText("X: Sample: " + (index + 1) + " at " + endTime.toString()
							+ ", " + Duration.parse(duration.toString()));

				} else { // Loaded from SD card file
					Long timeDurationInMillis = (long) ((Double.parseDouble(endTime) * 1000)
							- (Double.parseDouble(startTime) * 1000));
					Duration duration = Duration.ofMillis(timeDurationInMillis);
					String isoDurationFormat = duration.toString();

					xDataCoord.setText(
							"X: Sample: " + (index + 1) + ", " + Duration.parse(isoDurationFormat));
				}

				yDataCoord.setText("Y: " + MEASUREMENT_DECIMAL.format(dataPoint.getYValue()));
			}
		};
		return getValues;
	}

	/**
	 * An event handler for resetting the x and y displayed coordinates.
	 * 
	 * @return the event handler.
	 */
	protected EventHandler<MouseEvent> resetDataXYValues(Label xDataCoord, Label yDataCoord) {
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
	 * An event handler for change the cursor type when hovering over a data-point.
	 * 
	 * @param dataPoint
	 *            the data-point to attach this event to.
	 * @return the event handler.
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
	 * Deals with deleting boundary data points when the user right clicks.
	 * 
	 * @param dataPoint
	 *            a single data point from the series.
	 * @param series
	 *            the specified upper or lower boundary series which holds all of the boundary data
	 *            points.
	 * @return an event handler to deal with deleting elements from the series.
	 */
	protected void deleteData(XYChart.Data<Number, Number> dataPoint,
			XYChart.Series<Number, Number> series) {
		dataPoint.getNode().setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					System.out.println("OLD SIZE: " + series.getData().size());
					series.getData().remove(dataPoint);
					System.out.println("DELETE NODE FROM: " + series.getName() + " NEW SIZE: "
							+ series.getData().size());

					if (series.getName().contains("low")) {
						GuiController.instance.lowCounter--;
					}
				}
			}
		});
	}

	/**
	 * Removes listeners of the mask so that they cannot be further edited after they have been set.
	 */
	protected void removeAllListeners(XYChart.Series<Number, Number> series) {
		for (XYChart.Data<Number, Number> dataPoint : series.getData()) {
			dataPoint.getNode().setOnMouseClicked(null); // Remove deleting option
			dataPoint.getNode().setOnMouseMoved(null); // Remove moving option
		}
	}
}
