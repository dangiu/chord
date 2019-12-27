package visualization;

import java.awt.Color;

import chord.Node;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualizationOGL2D.EdgeStyleOGL2D;

public class EdgeStyle implements EdgeStyleOGL2D {

	@Override
	public int getLineWidth(RepastEdge<?> edge) {
		
		if(edge.getWeight() == Visualization.EdgeType.QUERY.ordinal() || edge.getWeight() == Visualization.EdgeType.REPLY.ordinal())
			return 2;
		
		return 1;
	}

	@Override
	public Color getColor(RepastEdge<?> edge) {
		if(edge.getWeight() == Visualization.EdgeType.SUCCESSOR.ordinal()) {
			return new Color(214, 214, 214); // light gray color to represent successors
		} else {
			Node n = (Node) edge.getSource();
			Color c = n.vis.getCurrentColor();
			if(edge.getWeight() == Visualization.EdgeType.REPLY.ordinal())
				c = c.darker().darker();
			return c;
		}
	}

}
