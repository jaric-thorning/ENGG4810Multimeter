package main;

import java.util.ArrayList;

import com.sun.javafx.geom.Line2D;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * A modified version of the line chart class to add the mask area to the chart plot.
 * 
 * PARTS MODIFIED FROM:
 * http://stackoverflow.com/questions/38871202/how-to-add-shapes-on-javafx-linechart (the idea of
 * refreshing the chart background) &
 * http://stackoverflow.com/questions/32601082/javafx-linechart-color-differences (the idea of
 * adding polygons to the background of the chart to fake area between the line and a boundary).
 */
public class ModifiedLineChart extends LineChart<Number, Number> {

	// To determine whether or not the selected series relates to a high or low boundary area
	private boolean isHighBoundary = false;
	private boolean isLowBoundary = false;

	// Stores the boundary mask polygons
	private ArrayList<Polygon> polygonsLowerBoundary = new ArrayList<>();
	private ArrayList<Polygon> polygonsHigherBoundary = new ArrayList<>();

	private NumberAxis yAxis;

	private double chartWidth = 645D;
	private double chartHeight = 518D;

	public ModifiedLineChart(NumberAxis xAxis, NumberAxis yAxis) {
		super(xAxis, yAxis);

		setupLineChart(yAxis);
	}

	/**
	 * Returns a copy of the polygons.
	 * 
	 * @param type
	 *            determines which type of polygons are needed (high/low boundary).
	 * @return a copy of the determined polygon arrays.
	 */
	protected ArrayList<Polygon> getPolygonArray(XYChart.Series<Number, Number> series) {
		ArrayList<Polygon> returnedPolygons = new ArrayList<>();

		if (series.getName().equals("high")) {
			returnedPolygons.addAll(polygonsHigherBoundary);
			return returnedPolygons;
		} else {
			returnedPolygons.addAll(polygonsLowerBoundary);
			return returnedPolygons;
		}
	}

	/**
	 * Sets up any necessary line chart properties.
	 * 
	 * @param yAxis
	 *            the y-axis of the line chart.
	 */
	private void setupLineChart(NumberAxis yAxis) {
		this.yAxis = yAxis;

		this.setHorizontalZeroLineVisible(false);
		this.setLegendSide(Side.RIGHT);

		this.setWidth(chartWidth);
		this.setHeight(chartHeight);
		
		this.getXAxis().setAutoRanging(false);
	}

	/**
	 * A setter value for the low boundary boolean 'isLowBoundary'.
	 * 
	 * @param newValue
	 *            the new value of the isLowBoundary boolean.
	 */
	public void setLowBoundarySelected(boolean newValue) {
		isLowBoundary = newValue;
	}

	/**
	 * A setter value for the high boundary boolean 'isHighBoundary'.
	 * 
	 * @param newValue
	 *            the new value of the isHighBoundary boolean.
	 */
	public void setHighBoundarySelected(boolean newValue) {
		isHighBoundary = newValue;
	}
	
	public boolean getLowBoundarySelected() {
		return isLowBoundary;
	}

	@Override
	protected void layoutPlotChildren() {
		super.layoutPlotChildren();

		// Clear the lower mask area
		getPlotChildren().removeAll(polygonsLowerBoundary);
		polygonsLowerBoundary.clear();

		// Clear the higher mask area
		getPlotChildren().removeAll(polygonsHigherBoundary);
		polygonsHigherBoundary.clear();

		XYChart.Series<Number, Number> lowSeries = (XYChart.Series<Number, Number>) getData()
				.get(0);
		ObservableList<Data<Number, Number>> lowSeriesData = lowSeries.getData();

		XYChart.Series<Number, Number> highSeries = (XYChart.Series<Number, Number>) getData()
				.get(1);
		ObservableList<Data<Number, Number>> highSeriesData = highSeries.getData();

		// Create the mask
		createMaskArea(lowSeries, lowSeriesData, polygonsLowerBoundary);
		createMaskArea(highSeries, highSeriesData, polygonsHigherBoundary);

		getPlotChildren().addAll(polygonsLowerBoundary);
		getPlotChildren().addAll(polygonsHigherBoundary);

		// Check if any of the new mask points are invalid.
		setupBoundsCheck(polygonsHigherBoundary, isHighBoundary);
		setupBoundsCheck(polygonsLowerBoundary, isLowBoundary);

	}

	/**
	 * Creates the mask area.
	 * 
	 * @param series
	 *            either the high or low boundary series.
	 * @param seriesData
	 *            the list of all the data points withn the given series.
	 * @param polygonList
	 *            holds either the high or low boundary polygons.
	 */
	private void createMaskArea(XYChart.Series<Number, Number> series,
			ObservableList<Data<Number, Number>> seriesData, ArrayList<Polygon> polygonList) {
		for (int i = 0; i < seriesData.size() - 1; i++) {
			// First polygon point
			double x1 = getXAxis().getDisplayPosition(seriesData.get(i).getXValue());
			double y1 = getYAxis().getDisplayPosition(this.yAxis.getUpperBound());

			if (series.getName().equals("low")) { // Special case if series is low.
				y1 = getYAxis().getDisplayPosition(this.yAxis.getLowerBound());
			}

			double x2 = getXAxis().getDisplayPosition(seriesData.get((i + 1)).getXValue());

			// Calculate the position of the mask.
			Polygon polygon = new Polygon();
			polygon.getPoints()
					.addAll(new Double[] { x1, y1, x1,
							getYAxis().getDisplayPosition(seriesData.get(i).getYValue()), x2,
							getYAxis().getDisplayPosition(seriesData.get((i + 1)).getYValue()), x2,
							y1 });

			if (series.getName().equals("low")) { // Special case if series is low.
				polygon.setFill(Color.rgb(240, 128, 128, 0.3));
			} else {
				polygon.setFill(Color.rgb(100, 149, 237, 0.3));
			}

			polygonList.add(polygon);
		}
	}

	// FIXME: Might not need this function as always starting with low boundary disabled.
	/**
	 * Sets up each polygon shape with a listener, so that if the user tries to select a new point
	 * within the polygon's shape, the user will be notified of their wrong choice.
	 * 
	 * @param polygons
	 *            the list of all the polygons which make up the mask bounary area under/over the
	 *            line.
	 * @param isSelected
	 *            whether or not the high or low mask has been selected.
	 */
	private void setupBoundsCheck(ArrayList<Polygon> polygons, boolean isSelected) {
		for (Polygon polygon : polygons) {
			if (!isSelected) {
				// Disable the selection of any chart space under the polygon.
				polygon.setMouseTransparent(false);
				setupPolygonListener(polygon);
			} else {
				// Enable the selection of any chart space under the polygon.
				polygon.setMouseTransparent(true);
			}
		}
	}

	/**
	 * A private helper function for 'setupBoundsCheck' which assigns a mouse clicked event handler
	 * to the given polygon.
	 * 
	 * @param polygon
	 *            a polygon belonging to either the low or high mask boundary areas.
	 */
	private void setupPolygonListener(Polygon polygon) {
		polygon.addEventHandler(MouseEvent.MOUSE_CLICKED, addWarning(polygon));
	}

	/**
	 * Creates an event handler to deal with the event when the user clicks on any space of the
	 * opposite mask boundary area.
	 * 
	 * @param polygon
	 *            the polygon that was clicked.
	 * @return an event handler.
	 */
	private EventHandler<MouseEvent> addWarning(Polygon polygon) {
		EventHandler<MouseEvent> onMouseClick = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (isLowBoundary) { // Only error under specific conditions.
					// Display warning box.
					GuiView.getInstance().illegalMaskPoint();
				}
			}

		};
		return onMouseClick;
	}

	protected int maskTestOverlapCheck(ArrayList<Polygon> polygons,
			XYChart.Data<Number, Number> currentDataPoint,
			XYChart.Data<Number, Number> nextDataPoint) {
		int errorCounter = 0;

		for (Polygon p : polygons) {
			double currentTempX = this.getXAxis().getDisplayPosition(currentDataPoint.getXValue());
			double currentTempY = this.getYAxis().getDisplayPosition(currentDataPoint.getYValue());

			double nextTempX = this.getXAxis().getDisplayPosition(nextDataPoint.getXValue());
			double nextTempY = this.getYAxis().getDisplayPosition(nextDataPoint.getYValue());

			if ((p.contains(new Point2D(currentTempX, currentTempY))
					|| (p.contains(new Point2D(nextTempX, nextTempY))))) {

				// Add to overlap array
				Line2D segment = new Line2D();
				segment.setLine(
						new com.sun.javafx.geom.Point2D(currentDataPoint.getXValue().floatValue(),
								currentDataPoint.getYValue().floatValue()),
						new com.sun.javafx.geom.Point2D(nextDataPoint.getXValue().floatValue(),
								nextDataPoint.getYValue().floatValue()));

				GuiController.instance.getOverlappedIntervals().add(segment);

				errorCounter++;
			}
		}
		return errorCounter;
	}

}
