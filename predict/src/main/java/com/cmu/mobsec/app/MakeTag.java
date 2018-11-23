package com.cmu.mobsec.app;

import java.util.ArrayList;
import java.util.List;

import org.ejml.simple.SimpleMatrix;

/**
 * Hello world!
 *
 */
public class MakeTag 
{
	static int D = 10;  // hidden unit count
	static int M = 15;  // feature and bias count 
	static double[][] meanTmp = {{0, 17242957.5991, 10.4651709402, 11.8658119658, 3620.8292735, 11670.4899573, 393.924145299, 3.29743589744, 
		                          294.190811966, 4.37136752137, 0.664743589744, 479.807264957, 146.178881081, 185.348869593, 94009.5929592}};
	static double[][] subTmp = {{1, 119988082, 8670, 2651, 12490948.0, 3762632.0, 1460.0, 1418.0, 1460.0, 1460.0, 315, 1460, 1260.56085106, 
		                         799.674933958, 639480.0}};

	static double[][] alphaTmp = new double[][] 
				{{-3.532, -0.008, -0.290, 0.734, 0.073, -0.837, -8.222, -11.090, -2.860, -3.078, -5.162, -8.716, -7.069, -11.586, 0.751},
			     {7.634, 22.031, 0.377, 0.940, -0.677, -0.539, 8.345, -1.813, 0.492, 1.897, -5.853, 3.876, 1.821, 4.007, 2.270},
			     {3.322, 26.627, 0.728, -0.198, -0.323, -0.725, 10.570, 0.018, 6.127, 4.154, -3.309, 6.066, 4.170, 5.767, -0.064},
			     {-1.876, 1.418, 0.168, -0.287, -0.508, 0.491, -1.656, 2.308, 0.647, 1.371, 4.211, -0.977, -0.623, -1.058, -1.893},
			     {-1.451, 12.517, 0.146, -0.760, 0.095, 0.648, -2.281, 0.715, 4.973, -0.967, 5.287, 5.078, 4.225, 1.427, -3.594},
			     {5.626, 7.458, 0.670, -0.632, 0.541, -0.238, 5.298, -1.990, 1.604, 0.132, -4.095, 2.084, 0.242, 1.412, 1.388},
			     {-2.481, 0.104, 0.390, -0.468, -0.317, -0.770, -0.297, 0.067, 0.536, -0.428, 0.793, -0.595, 1.223, -0.742, 0.858},
			     {5.555, 10.148, -0.167, 0.531, -1.242, 0.346, -7.027, 14.467, -0.134, 8.412, 15.884, 1.816, 13.110, 8.348, -2.420},
			     {1.332, -0.229, -0.312, 1.473, -0.991, 1.064, -2.837, 13.223, 19.965, 9.458, 17.927, 10.899, 4.343, -10.739, -10.702},
			     {0.804, 9.015, 0.232, 0.021, 0.202, -0.070, -2.749, 9.547, -0.894, -0.556, 8.434, 2.372, 5.931, 3.446, -2.220}};
    static double[][] betaTmp = new double[][] 
 		   {{-0.512, -3.628, -7.004, -7.587, 0.242, 2.463, -4.822, 0.190, 6.506, 15.270, 1.314},
 	        {0.512, 4.499, 6.723, 8.437, 0.096, -2.798, 4.007, 0.509, -7.164, -15.491, -1.744}};

 	static SimpleMatrix alpha  = new SimpleMatrix(alphaTmp);
    static SimpleMatrix beta = new SimpleMatrix(betaTmp);
    static SimpleMatrix mean = new SimpleMatrix(meanTmp);
    static SimpleMatrix sub = new SimpleMatrix(subTmp);
    static MakeTag instance;
	public MakeTag() {

       
	}
	static public MakeTag getInstance() {
		if (instance == null) {
			instance = new MakeTag();
		}
		return instance;
	} 
	public static SimpleMatrix sigmoid(SimpleMatrix values) {
        for (int i = 0; i < values.numRows() * values.numCols(); i++) {
            values.set(i, 1 / (1 + Math.exp(-values.get(i))));
        }
        return values;
	}
	public static SimpleMatrix getA(SimpleMatrix X) {
		return alpha.mult(X.transpose());
	}
	public static SimpleMatrix getB(SimpleMatrix Z, int K) {
		SimpleMatrix tmp = new SimpleMatrix(D+1, 1);
		tmp.set(0, 1);
		for (int i = 1; i < D + 1; i++) {
			tmp.set(i, Z.get(i - 1));
		}
		return beta.mult(tmp);
	}
	public static SimpleMatrix Normalize(SimpleMatrix values) {
		for (int i = 0; i < values.numRows() * values.numCols(); i++) {
            values.set(i, (values.get(i) - mean.get(i)) / sub.get(i));
        }
        return values;
	}
	
	// The API used with adding bias and transferred to matrix
	public static int MakeSingleTag(SimpleMatrix X) {
		SimpleMatrix n_X = Normalize(X);
		SimpleMatrix A = getA(n_X);
		SimpleMatrix Z = sigmoid(A);
		SimpleMatrix B = getB(Z,2);
		if (B.get(0) > B.get(1)) {
			return 0;
		}
		return 1;
	}
	
	// The API used without adding bias(Input is the raw network flow)
	public static int GetTag(List<Double> X) {
		double[][] matrixX = new double[1][X.size() + 1];
		matrixX[0][0] = 1.0;
		for (int i = 1; i < X.size() + 1; i++) {
			matrixX[0][i] = X.get(i - 1);
		}
		SimpleMatrix n_X = Normalize(new SimpleMatrix(matrixX));
		SimpleMatrix A = getA(n_X);
		SimpleMatrix Z = sigmoid(A);
		SimpleMatrix B = getB(Z,2);
		if (B.get(0) > B.get(1)) {
			return 0;
		}
		return 1;
	}
	
    public static void main( String[] args )
    {

        // normal flow test
//    	double[] X = new double[] {564975.0, 12.0, 11.0, 1193.0, 4650.0, 1418.0, 0.0, 916.0, 
//    			                   0.0, 0.0, 1418.0, 243.45833333333337, 440.2825159378688, 
//    			                   193848.69384057968};
    	
    	// malicious flow test
    	double[] X = new double[] {40.0, 2.0, 0.0, 42.0, 0.0, 0.0, 0.0, 42.0, 0.0, 0.0, 
    			                  42.0, 28.0, 24.248711305964278, 588.0};
    	
    	ArrayList<Double> input = new ArrayList<Double>();
    	for (double i : X) {
    		input.add(i);
    	}
        int tag = MakeTag.GetTag(input);
        if (tag == 0) {
        	System.out.println("normal flow");
        } else {
        	System.out.println("malicious flow");
        }
    }
}
