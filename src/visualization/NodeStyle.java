package visualization;

import java.awt.Color;

import chord.Configuration;
import chord.Helper;
import chord.Node;
import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;

public class NodeStyle extends DefaultStyleOGL2D {
	
	public static final Color COLOR_DEFAULT = Color.black;
	public static final Color COLOR_INACTIVE = Color.lightGray;
	public static final Color COLOR_TIMEOUT = Color.red;
	
	@Override
	public Color getColor(Object agent){
		Color color = COLOR_DEFAULT;	
        if(agent instanceof Node){
        	Node n = (Node)agent;
        	if(!n.isActive()) {
        		color = COLOR_INACTIVE;
        	} else if(n.visQueryTimeout && (Helper.getCurrentTick() - n.visQueryTimeoutTick < Configuration.VISUALIZATION_TIMEOUT_SHOWTIME)) {
        		color = COLOR_TIMEOUT;
        	} else if(n.getId() == n.vis.getCurrentOriginator()) {
        		color = n.vis.getCurrentColor();
        	}
        }
        return color;
	}
	
	@Override
    public float getScale(Object object) {
        return 1;
    }
	
	@Override
	public Color getBorderColor(Object agent) {
		return Color.black;
	}
	
	@Override
	public int getBorderSize(Object agent) {
		return 0;
	}
	
}
