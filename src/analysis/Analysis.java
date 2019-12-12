package analysis;

/**
 * Utility class for analysis, allows to select which analysis to perform
 * @author danie
 *
 */
public class Analysis {
	public enum AnalysisType {NONE, VALUES_LOST, PATH_LENGTH};
	public static final AnalysisType ACTIVE = AnalysisType.NONE;
	
	//parameters for some specific analysis
	public static final int CRASHED_PERCENTAGE = 50;
	
	
	public static boolean isActive(AnalysisType at) {
		return at == Analysis.ACTIVE;
	}
}
