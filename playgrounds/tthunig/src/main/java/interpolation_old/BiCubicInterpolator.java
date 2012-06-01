package interpolation_old;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.BivariateRealFunction;
import org.apache.commons.math.analysis.interpolation.BicubicSplineInterpolator;
import org.apache.commons.math.analysis.interpolation.BivariateRealGridInterpolator;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * interpolates data on a SpatialGrid with bicubic spline interpolation from apache (http://commons.apache.org)
 * 
 * @author tthunig
 *
 */
class BiCubicInterpolator {

	private BivariateRealFunction interpolatingFunction = null;
	
	private SpatialGrid sg = null;
	
	/**
	 * prepares bicubic spline interpolation:
	 * generates interpolation function with BicubicSplineInterpolator from apache (http://commons.apache.org/math/apidocs/org/apache/commons/math3/analysis/interpolation/BicubicSplineInterpolator.html)
	 * 
	 * @param sg the SpatialGrid to interpolate
	 */
	BiCubicInterpolator(SpatialGrid sg){
		this.sg= sg;
		
		//create default coordinates for interpolation and compatible array of values
		double[] x_default= coord(0, sg.getMatrix()[0].length-1, 1);
		double[] y_default= coord(0, sg.getMatrix().length-1, 1);
		double[][] mirroredValues= flip(sg.getMatrix());
		
		BivariateRealGridInterpolator interpolator = new BicubicSplineInterpolator();
		try {
			interpolatingFunction = interpolator.interpolate(y_default, x_default, mirroredValues); //needs default coordinates (0,1,2,...)
		} catch (MathException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * interpolates the value on a arbitrary point with bicubic spline interpolation from apache
	 * 
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @return interpolated value on the point (xCoord, yCoord)
	 */
	double biCubicInterpolation(double xCoord, double yCoord){
		try {
			return interpolatingFunction.value(transform(yCoord, this.sg.getYmin(), this.sg.getResolution()), transform(xCoord, this.sg.getXmin(), this.sg.getResolution()));
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		}
		return Double.NaN;
	}
	
	/**
	 * transforms a given coordinate into their default value in the system of base coordinates (0,1,...)
	 * 
	 * @param coord 
	 * @param min the minimum value for this coordinate where a value is known at
	 * @param res the resolution of the SpatialGrid
	 * @return transformed coordinate between 0 and the number of known values in this coordinate direction
	 */
	private static double transform(double coord, double min, double res) {
		return (coord-min)/res;
	}

	/**
	 * creates a coordinate vector
	 * 
	 * @param min the minimum coordinate
	 * @param max the maximum coordinate
	 * @param resolution
	 * @return coordinate vector from min to max with the given resolution
	 */
	private static double[] coord(double min, double max, double resolution) {
		double[] coord = new double[(int) ((max - min) / resolution) + 1];
		coord[0] = min;
		for (int i = 1; i < coord.length; i++) {
			coord[i] = min + i * resolution;
		}
		return coord;
	}
	
	/**
	 * flips the given matrix horizontal
	 * 
	 * @param matrix
	 * @return the horizontal mirrored matrix
	 */
	private static double[][] flip(double[][] matrix) {
		double[][] flip= new double[matrix.length][matrix[0].length];
		for (int i=0; i<flip.length; i++){
			for (int j=0; j<flip[0].length; j++){
				flip[i][j]= matrix[matrix.length-1-i][j];
			}
		}
		return flip;
	}
	
}
