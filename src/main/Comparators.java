package main;

import java.util.Comparator;

import com.sun.javafx.geom.Line2D;

import javafx.scene.chart.XYChart;

public class Comparators {

	public Comparators() {
		// I IS EMPTY :(
	}

	/**
	 * A comparator to help determine the order of sorting the line segments by increasing x1-value.
	 * 
	 * @return a comparator.
	 */
	protected Comparator<Line2D> sortOverlap() {
		final Comparator<Line2D> OVERLAP_ORDER = new Comparator<Line2D>() {
			public int compare(Line2D e1, Line2D e2) {

				if (e2.x1 >= e1.x1) {
					return -1;
				} else {
					return 1;
				}
			}
		};

		return OVERLAP_ORDER;
	}

	/**
	 * A comparator to help determine the order of sorting the data points by increasing x-value.
	 * 
	 * @return a comparator
	 */
	protected Comparator<XYChart.Data<Number, Number>> sortChart() {

		final Comparator<XYChart.Data<Number, Number>> CHART_ORDER = new Comparator<XYChart.Data<Number, Number>>() {
			public int compare(XYChart.Data<Number, Number> e1, XYChart.Data<Number, Number> e2) {

				if (e2.getXValue().doubleValue() >= e1.getXValue().doubleValue()) {
					return -1;
				} else {
					return 1;
				}
			}
		};

		return CHART_ORDER;
	}
}
