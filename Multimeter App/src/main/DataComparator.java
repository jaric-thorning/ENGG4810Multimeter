package main;

import java.util.Comparator;

import com.sun.javafx.geom.Line2D;

import javafx.scene.chart.XYChart;

/**
 * The DataComparator class houses custom comparators for sorting Line2D and
 * XYChart.Data<Number, Number> values.
 * 
 * @author dayakern
 *
 */
public class DataComparator {

	public DataComparator() {
	}

	/**
	 * A comparator used to determine the order-in increasing start point x
	 * values-of all line segments found to be overlapping into the high/low
	 * mask regions.
	 * 
	 * @return the value which determines if the line segment should stay where
	 *         it is in the list or if it should be moved further down.
	 */
	protected Comparator<Line2D> sortOverlap() {

		// Comparing the x1 values (start point x values) of two line segments
		final Comparator<Line2D> overlappedIntervalOrder = new Comparator<Line2D>() {
			public int compare(Line2D lineSegment1, Line2D lineSegment2) {

				if (lineSegment2.x1 >= lineSegment1.x1) {
					return -1;
				} else {
					return 1;
				}
			}
		};

		return overlappedIntervalOrder;
	}

	/**
	 * A comparator used to determine the order-in increasing x values-of all
	 * points added into the high/low mask series.
	 * 
	 * @return the value which determines if the point should stay where it is
	 *         in the list or if it should be moved further down.
	 */
	protected Comparator<XYChart.Data<Number, Number>> sortChart() {

		// Comparing the x values of two points
		final Comparator<XYChart.Data<Number, Number>> maskOrder = new Comparator<XYChart.Data<Number, Number>>() {
			public int compare(XYChart.Data<Number, Number> dataPoint1, XYChart.Data<Number, Number> dataPoint2) {

				if (dataPoint2.getXValue().doubleValue() >= dataPoint1.getXValue().doubleValue()) {
					return -1;
				} else {
					return 1;
				}
			}
		};

		return maskOrder;
	}
}
