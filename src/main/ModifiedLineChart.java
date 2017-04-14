// MODIFIED FROM http://stackoverflow.com/questions/38871202/how-to-add-shapes-on-javafx-linechart
// & http://stackoverflow.com/questions/32639882/conditionally-color-background-javafx-linechart
package main;

import java.util.ArrayList;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

//FIXME: MAKE SURE THAT THE CHARTS INCLUDE THE RIGHT INITIAL AND FINAL POINTS
public class ModifiedLineChart extends LineChart<Number, Number> {

	private boolean testUpper = false;
	private boolean testLower = false;
	private double tempX = 0D;
	private double tempY = 0D;

	private ArrayList<Polygon> polygonsLower = new ArrayList<>();
	private ArrayList<Polygon> polygonsUpper = new ArrayList<>();

	private NumberAxis yAxis;
	// private NumberAxis xAxis;

	public ModifiedLineChart(NumberAxis xAxis, NumberAxis yAxis) {
		super(xAxis, yAxis);

		// this.xAxis = xAxis;
		this.yAxis = yAxis;

		this.setHorizontalZeroLineVisible(false);
		this.setLegendVisible(false);

		this.setWidth(645);
		this.setHeight(518);
	}
	
	public boolean getLowerTest() {
		return testLower;
	}

	public boolean getUpperTest() {
		return testUpper;
	}

	public void setLowerTest(boolean value) {
		testLower = value;
	}

	public void setUpperTest(boolean value) {
		testUpper = value;
	}

	@Override
	protected void layoutPlotChildren() {
		super.layoutPlotChildren();

		// Reset the position of the mask upper
		getPlotChildren().removeAll(polygonsLower);
		polygonsLower.clear();

		// Reset the position of the mask lower
		getPlotChildren().removeAll(polygonsUpper);
		polygonsUpper.clear();

		XYChart.Series<Number, Number> series = (XYChart.Series<Number, Number>) getData().get(0);
		ObservableList<Data<Number, Number>> listOfData = series.getData();

		XYChart.Series<Number, Number> series2 = (XYChart.Series<Number, Number>) getData().get(1);
		ObservableList<Data<Number, Number>> listOfData2 = series2.getData();

		setupMaskArea(series, listOfData, polygonsLower);
		setupMaskArea(series2, listOfData2, polygonsUpper);

		getPlotChildren().addAll(polygonsLower);
		getPlotChildren().addAll(polygonsUpper);

		// lower is selected and upper has been completed
		System.out.println("upper: " + testUpper);
		System.out.println("lower: " + testLower);
		
		setupBoundsCheck(polygonsUpper, testUpper);
		setupBoundsCheck(polygonsLower, testLower);

	}
	
	private void setupBoundsCheck(ArrayList<Polygon> polygons, boolean isSelected) {
		for (Polygon p : polygons) {
			if (!isSelected) {
				p.setMouseTransparent(false);
				setupPolygonListener(p);
			} else {
				p.setMouseTransparent(true);
			}
		}
	}

	private void setupMaskArea(XYChart.Series<Number, Number> series,
			ObservableList<Data<Number, Number>> listOfData, ArrayList<Polygon> polygonList) {
		for (int i = 0; i < listOfData.size() - 1; i++) {
			// [0,1]
			double x1 = getXAxis().getDisplayPosition(listOfData.get(i).getXValue());
			double y1 = getYAxis().getDisplayPosition(this.yAxis.getUpperBound());

			if (series.getName().equals("low")) {
				y1 = getYAxis().getDisplayPosition(this.yAxis.getLowerBound());
			}

			// [2,3]
			double x2 = getXAxis().getDisplayPosition(listOfData.get((i + 1)).getXValue());

			// if () {
			// Recalculate the position of the mask
			Polygon polygon = new Polygon();
			polygon.getPoints()
					.addAll(new Double[] { x1, y1, x1,
							getYAxis().getDisplayPosition(listOfData.get(i).getYValue()), x2,
							getYAxis().getDisplayPosition(listOfData.get((i + 1)).getYValue()), x2,
							y1 });

			// // CHECK IF MOUSE IN BOUNDS
			// // polygon.contains(x, y);
			// setMouseTransparent(false);
			// setupPolygonListener(polygon);

			if (series.getName().equals("low")) {
				polygon.setFill(Color.rgb(240, 128, 128, 0.3));
			} else {
				polygon.setFill(Color.rgb(100, 149, 237, 0.3));// //cornflower blue with alpha
			}

			// Add polygons to arraylist
			polygonList.add(polygon);

		}
		// }
	}

	private void setupPolygonListener(Polygon polygon) {
		polygon.addEventHandler(MouseEvent.MOUSE_CLICKED, addWarning(polygon));
	}

	private EventHandler<MouseEvent> addWarning(Polygon polygon) {
		EventHandler<MouseEvent> onMouseClick = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				// TODO Auto-generated method stub
				System.out.println("CLICKED ON POLYGON");
			}

		};
		return onMouseClick;
	}

}
