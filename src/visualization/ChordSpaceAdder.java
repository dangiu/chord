package visualization;

import chord.Configuration;
import chord.Node;
import repast.simphony.space.Dimensions;
import repast.simphony.space.continuous.ContinuousAdder;
import repast.simphony.space.continuous.ContinuousSpace;

public class ChordSpaceAdder<T> implements ContinuousAdder<T> {
	
	private double alpha; //degree between 2 nodes in the chord circle
	
	public ChordSpaceAdder() {
		this.alpha = Math.toRadians(360) / Configuration.MAX_NUMBER_OF_NODES;
	}
	
	public void add(ContinuousSpace<T> space, T obj) {
		Dimensions dims = space.getDimensions();
		
		double radius = space.getDimensions().getHeight() * 0.8 / 2; //set radius of the circle so that the circle occupies 80% of the space
		
		assert dims.size() == 2; //space adder works only for 2D space
		
		double relX = 0, relY = 0; //relative x and y from the center of the circle
		double centerX, centerY; //origin of the circle (must be placed in the middle of the display)
		centerX = dims.getDimension(0) / 2;
		centerY = dims.getDimension(1) / 2;
		
		if (obj instanceof Node) {
			int id = ((Node) obj).getId();
			double beta = (alpha * id) - Math.toRadians(90); //angle on the circle where we place the current node
			//node 0 must be placed at the top part of the circle (12 o'clock)
			relX = Math.cos(beta) * radius;
			relY = (-1) * Math.sin(beta) * radius; //needed in order to go clockwise instead of counterclockwise with id placement
		}
		

		double x,y; //final coordinates computed
		x = centerX + relX;
		y = centerY + relY;
		
		double[] location = new double[]{x, y};

		space.moveTo(obj,location);
	}
}
