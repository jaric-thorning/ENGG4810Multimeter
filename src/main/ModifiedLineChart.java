package main;

import java.util.ArrayList;

import com.sun.javafx.geom.Line2D;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * The ModifiedLineChart class represents a modified version of the line chart class which adds the mask area to the
 * chart plot.
 * 
 * PARTS MODIFIED FROM: http://stackoverflow.com/questions/38871202/how-to-add-shapes-on-javafx-linechart (the idea of
 * refreshing the chart background) & http://stackoverflow.com/questions/32601082/javafx-linechart-color-differences
 * (the idea of adding polygons to the background of the chart to fake area between the line and a boundary).
 * 
 * @modifier/@author dayakern
 */
public class ModifiedLineChart extends LineChart<Number, Number> {

	// Stores the boundary mask polygons
	private ArrayList<Polygon> polygonsLowerBoundary;
	private ArrayList<Polygon> polygonsHigherBoundary;

	private NumberAxis yAxis;
	private NumberAxis xAxis;

	private double chartWidth = 645D;
	private double chartHeight = 518D;

	DataEvents dataEvents = new DataEvents();

	public ModifiedLineChart(NumberAxis xAxis, NumberAxis yAxis) {
		super(xAxis, yAxis);

		setupLineChart(yAxis, xAxis);

		polygonsLowerBoundary = new ArrayList<>();
		polygonsHigherBoundary = new ArrayList<>();
	}

	/**
	 * Gets the polygons belonging to high/low mask boundaries.
	 * 
	 * @param series
	 *            the specified high/low mask series which will hold the polygons
	 * @return the list of polygons belonging to the specified mask series
	 */
	protected ArrayList<Polygon> getPolygonArray(XYChart.Series<Number, Number> series) {
		ArrayList<Polygon> returnedPolygons = new ArrayList<>();

		if (series.getName().contains("high")) {
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
	 *            the y-axis of the line chart
	 * @param xAxis
	 *            the x-axis of the line chart
	 */
	private void setupLineChart(NumberAxis yAxis, NumberAxis xAxis) {
		this.yAxis = yAxis;
		this.xAxis = xAxis;

		this.setHorizontalZeroLineVisible(false);
		this.setLegendSide(Side.BOTTOM);

		this.setWidth(chartWidth);
		this.setHeight(chartHeight);

		this.getXAxis().setAutoRanging(false);
		this.setAnimated(false);
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

		XYChart.Series<Number, Number> lowSeries = (XYChart.Series<Number, Number>) getData().get(0);
		ObservableList<Data<Number, Number>> lowSeriesData = lowSeries.getData();

		XYChart.Series<Number, Number> highSeries = (XYChart.Series<Number, Number>) getData().get(1);
		ObservableList<Data<Number, Number>> highSeriesData = highSeries.getData();

		// Create the mask
		createMaskArea(lowSeries, lowSeriesData, polygonsLowerBoundary);
		createMaskArea(highSeries, highSeriesData, polygonsHigherBoundary);

		getPlotChildren().addAll(polygonsLowerBoundary);
		getPlotChildren().addAll(polygonsHigherBoundary);

		// Check if any of the new mask points are invalid.
		setupBoundsCheck(polygonsHigherBoundary);
		setupBoundsCheck(polygonsLowerBoundary);
	}

	/**
	 * Creates the mask area.
	 * 
	 * @param series
	 *            the specified high/low mask series which will hold the polygons
	 * @param seriesData
	 *            the list of all the data points within the given series
	 * @param polygonList
	 *            holds either the high or low boundary area polygons
	 */
	private void createMaskArea(XYChart.Series<Number, Number> series, ObservableList<Data<Number, Number>> seriesData,
			ArrayList<Polygon> polygonList) {

		for (int i = 0; i < seriesData.size() - 1; i++) {

			// First polygon point
			double x1 = getXAxis().getDisplayPosition(seriesData.get(i).getXValue());
			double y1 = getYAxis().getDisplayPosition(this.yAxis.getUpperBound());

			// Special case if series is low
			if (series.getName().contains("low")) {
				y1 = getYAxis().getDisplayPosition(this.yAxis.getLowerBound());
			}

			double x2 = getXAxis().getDisplayPosition(seriesData.get((i + 1)).getXValue());

			// Calculate the position of the mask
			Polygon polygon = new Polygon();
			polygon.getPoints()
					.addAll(new Double[] { x1, y1, x1, getYAxis().getDisplayPosition(seriesData.get(i).getYValue()), x2,
							getYAxis().getDisplayPosition(seriesData.get((i + 1)).getYValue()), x2, y1 });

			// Change area fill
			setPolygonFill(series.getName(), polygon);
			polygonList.add(polygon);
		}
	}

	/**
	 * A private helper function to 'createMaskArea' which changes the colour of the area under/over the mask boundary
	 * lines.
	 * 
	 * @param seriesName
	 *            the name of the mask series
	 * @param polygon
	 *            the polygon area that needs a colour change
	 */
	private void setPolygonFill(String seriesName, Polygon polygon) {
		if (seriesName.contains("low")) {
			polygon.setFill(Color.rgb(240, 128, 128, 0.3));
		} else {
			polygon.setFill(Color.rgb(100, 149, 237, 0.3));
		}
	}

	/**
	 * Sets up each polygon shape so that if the user tries to select a new point within the polygon's shape, the user
	 * will be able to.
	 * 
	 * @param polygons
	 *            the list of all the polygons which make up the mask boundary area under/over the line
	 */
	private void setupBoundsCheck(ArrayList<Polygon> polygons) {
		for (Polygon polygon : polygons) {

			// Enable the selection of any chart space under the polygon
			setupPolygonListener(polygon);
			polygon.setMouseTransparent(true);
		}
	}

	/**
	 * A private helper function to 'setupBoundsCheck' which assigns a mouse clicked event handler to the given polygon.
	 * 
	 * @param polygon
	 *            a polygon belonging to either the low or high mask boundary areas
	 */
	private void setupPolygonListener(Polygon polygon) {
		polygon.addEventHandler(MouseEvent.MOUSE_CLICKED, dataEvents.addWarning(polygon));
	}

	/**
	 * Checks if the data points overlap anywhere over the high/low mask boundary area and augments the error count if
	 * there are any overlaps. Also adds the two data points to a list of overlapped line segments.
	 * 
	 * @param isHighMaskBoundary
	 *            whether or not the mask is high/low
	 * @param currentDataPoint
	 *            current data point of multimeter data
	 * @param nextDataPoint
	 *            next data point of multimeter data
	 * @return a counter of how many overlaps occurred
	 */
	protected int maskTestOverlapCheck(boolean isHighMaskBoundary, XYChart.Data<Number, Number> currentDataPoint,
			XYChart.Data<Number, Number> nextDataPoint) {
		int errorCounter = 0;
		ArrayList<Polygon> maskPolygons;

		if (isHighMaskBoundary) { // High boundary
			maskPolygons = polygonsHigherBoundary;
		} else { // Low boundary
			maskPolygons = polygonsLowerBoundary;
		}

		// Determine overlap
		for (Polygon p : maskPolygons) {
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

	/**
	 * Checks if the given data point overlap anywhere over the high/low mask boundary area.
	 * 
	 * @param currentDataPoint
	 *            current data point of low mask series
	 * @return false if there was an overlap, true otherwise
	 */
	protected boolean maskTestSinglePointOverlapCheck(XYChart.Data<Number, Number> currentDataPoint) {
		ArrayList<Polygon> maskPolygons = polygonsHigherBoundary;

		// Determine overlap
		for (Polygon p : maskPolygons) {
			double currentTempX = this.getXAxis().getDisplayPosition(currentDataPoint.getXValue());
			double currentTempY = this.getYAxis().getDisplayPosition(currentDataPoint.getYValue());

			if (p.contains(new Point2D(currentTempX, currentTempY))) {

				GuiView.getInstance().illegalMaskPoint();
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the data points overlap anywhere over the high/low mask boundary area.
	 * 
	 * @param currentDataPoint
	 *            current data point of low mask series
	 * @param nextDataPoint
	 *            next data point of low mask series
	 * @return true if there were no overlaps, false otherwise
	 */
	protected boolean maskTestPointsOverlapCheck(XYChart.Data<Number, Number> currentDataPoint,
			XYChart.Data<Number, Number> nextDataPoint) {

		ArrayList<Polygon> maskPolygons = polygonsHigherBoundary;

		// Determine overlap
		for (Polygon p : maskPolygons) {
			double currentTempX = this.getXAxis().getDisplayPosition(currentDataPoint.getXValue());
			double currentTempY = this.getYAxis().getDisplayPosition(currentDataPoint.getYValue());

			double nextTempX = this.getXAxis().getDisplayPosition(nextDataPoint.getXValue());
			double nextTempY = this.getYAxis().getDisplayPosition(nextDataPoint.getYValue());

			if ((p.contains(new Point2D(currentTempX, currentTempY))
					|| (p.contains(new Point2D(nextTempX, nextTempY))))) {

				GuiView.getInstance().illegalMaskPoint();
				return false;
			}
		}
		return true;
	}

	/**
	 * Changes the 'look' of the line chart in order to distinctly separate standard mode (voltage, current resistance
	 * measurements) and continuity/logic mode.
	 */
	protected void setContinuityMode() {
		lookup(".chart-plot-background").setStyle("-fx-background-color: #000000;");
	}

	/**
	 * Reverts the 'look' of the line chart back to the standard style.
	 */
	protected void revertContinuityMode() {
		lookup(".chart-plot-background").setStyle("-fx-background-color: #ffffff;");
	}

	/**
	 * Sets the upper and lower x-axis boundaries of the high/low mask areas.
	 * 
	 * @param newAxisUpperValue
	 *            the new upper bound of the mask area
	 * @param newAxisLowerValue
	 *            the new lower bound of the mask area
	 */
	protected void updateMaskBoundaries(double newAxisUpperValue, double newAxisLowerValue) {
		updateMaskBoundary(GuiController.instance.getHighSeries(), newAxisUpperValue);
		updateMaskBoundary(GuiController.instance.getLowSeries(), newAxisUpperValue);

		xAxis.setUpperBound(newAxisUpperValue);
		xAxis.setLowerBound(newAxisLowerValue);
	}

	/**
	 * A private helper function to 'updateMaskBoundaries' which updates the given series' upper and lower boundaries of
	 * the x-axis.
	 * 
	 * @param series
	 *            either high/low mask boundary
	 * @param newUpper
	 *            the new x-axis upper boundary value
	 */
	private void updateMaskBoundary(XYChart.Series<Number, Number> series, double newUpper) {
		if (series.getData().size() > 0 && series.getData().get(series.getData().size() - 1).getXValue()
				.doubleValue() == xAxis.getUpperBound()) {

			// Update the mask upper value
			series.getData().get(series.getData().size() - 1).setXValue(newUpper);
		}
	}
}
