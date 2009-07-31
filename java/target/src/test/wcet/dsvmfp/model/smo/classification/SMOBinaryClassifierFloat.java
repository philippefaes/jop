package wcet.dsvmfp.model.smo.classification;

import cmp.Execute;
import cmp.ParallelExecutor;

import com.jopdesign.sys.Const;
import com.jopdesign.sys.Native;

import wcet.dsvmfp.model.smo.kernel.FloatUtil;
import wcet.dsvmfp.model.smo.kernel.KFloat;

/**
 * Class SMOBinaryClassifier with float.
 */
public class SMOBinaryClassifierFloat {

	final static boolean PRINT = true;

	/** Number of lagrange multipliers in deployed RT model */
	public final static int ALPHA_RT = 2;

	static boolean info;

	static public boolean printSMOInfo;

	/** The [m] Lagrange multipliers. */
	static public float[] alph;
	static float alph1, alph2;

	static float y1, y2;

	/** The target vector of {-1,+1}. */
	static public float[] target;

	/** The data vector [rows][columns]. */
	static public float[][] point;

	/** The high bound. */
	static public float C;

	/** The error tolerance. */
	static public float tol;

	/** The error tolerance, that is used for KKT violation checks. */
	static public float eps;

	// E1 and E1 as used in takestep
	static public float E1, E2;

	/** The bias_fp. */
	static public float bias;

	static public int i1, i2;

	/**
	 * The number of training points. It is declared final to avoid
	 * synchronization problems.
	 */
	public static int m;

	/** The input space dimensinality. */
	static public int n;

	// static public boolean takeStepFlag;

	// static public boolean takeStepResult;

	// ////////////Performance Variables////////////////////
	static public int takeStepCount;

	static public int numChanged;

	static public boolean examineAll;

	static public int loop;

	static public float k11, k12, k22;

	// objective function at a2=L and a2=H
	static public float Lobj, Hobj;

	/**
	 * Method mainRoutine, which estimates the SVM parameters. The parameters
	 * are intialized before the training of the SVM is conducted.
	 * 
	 * @return true if the trainng went well
	 */
	static public boolean mainRoutine() {

		info = false;
		// The number of updated that significantly changed the
		// Lagrange multipliers
		numChanged = 0;
		// Indicates if all examples has been tested
		examineAll = true;
		// Reset alpha_fp array , errorarray , b, and C
		takeStepCount = 0;
		initParams();
		System.out.println("After init params");

		loop = 0;

		boolean first = true;
		P("++++Pre While++++");
		while (numChanged > 0 || first) {
			P("----Pre loop----");
			first = false;
			numChanged = 0;
			// assigns i1 and i2
			// getIndex(indexarray);

			boolean takeStepResult = false;
			for (i1 = 0; i1 < m; i1++) {
				System.out.println("*******takeStep()*****");
				System.out.print("i1:");
				System.out.println(i1);
				for (i2 = 0; i2 < m; i2++) {
					System.out.print("i2:");
					System.out.println(i2);
					takeStepResult = examineExample();
					if (takeStepResult)
						numChanged++;
					System.out.print("takeStep: ");
					System.out.println(takeStepResult);
				}
			}
			P("////Post for loop:" + numChanged);
		}
		P("++++Post while++++");

		measure();

		if (false) {

			while (numChanged > 0 || examineAll) { // @WCA loop=2
				// while (loop>=0) { //temp debug forever loop
				// System.out.print("Starting loop=");
				// System.out.println(loop);
				loop++;
				// System.out.print("numChanged=");
				// System.out.println(numChanged);
				// if (examineAll)
				// System.out.println("examineAll=true");
				// else
				// System.out.println("examineAll=false");
				for (int i = 0; i < 100000; i++)
					// @WCA loop=2
					; // slow it down to print
				numChanged = 0;
				if (examineAll) {
					for (i2 = 0; i2 < m; i2++) { // @WCA loop=2
						if (examineExample()) {
							numChanged++;
						}
					}
				} else {
					// Inner loop success
					numChanged = innerLoop();
					// numChanged = 0; // TODO: remove
				}
				if (examineAll) {
					examineAll = false;
				} else if (numChanged == 0) {
					examineAll = true;
				}
				// break;
			}
			if (PRINT)
				System.out.println("SMO.mainroutine.trained");
		}// false

		if (PRINT) {
			System.out.println("Done!");
			smoInfo();
		}

		return true;
	}

	/**
	 * Method takeStep, which optimizes two lagrange multipliers at a time. The
	 * method uses DsvmUtilABC.epsEqual to determine if there was positive
	 * progress.
	 * 
	 * @param i1
	 *            - second choice heuristics
	 * @param i2
	 *            - first choice heuristics
	 * @return true if a positive step has occured
	 */
	public static boolean takeStep() {

		P("takeStep, takeStepCount=" + takeStepCount + " i1=" + i1 + " i2="
				+ i2);

		// If the first and second point is the same then return false
		if (i1 == i2) {
			return false;
		}

		alph1 = alph[i1];
		y1 = target[i1];

		P("S: alph1:" + alph1);

		E1 = getfFP(i1) - y1;
		P("f1_fp:" + getfFP(i1));

		float s_fp = y1 * y2;

		// Compute L, H
		float L = getLowerClipFP(i1, i2);
		P("L=" + L);
		float H = getUpperClipFP(i1, i2);
		P("H=" + H);
		if (L == H)
			return false;

		k11 = getKernelOutputFloat(i1, i1);
		k12 = getKernelOutputFloat(i1, i2);
		k22 = getKernelOutputFloat(i2, i2);

		float eta = 2 * k12 - k11 - k22;
		P("eta=" + eta);

		// on
		float a2, a1;

		if (eta < 0) {
			a2 = alph2 - (y2 * (E1 - E2)) / eta;
			P("eta < 0: a2=" + a2);

			if (a2 < L) {
				a2 = L;
				P("eta < 0: L=" + L + " a2=" + a2);
			} else if (a2 > H) {
				a2 = H;
				P("eta < 0: H=" + H + " a2=" + a2);
			}
		} else {

			float tempAlpha2_fp = alph[i2];
			alph[i2] = L;
			float Lobj = getObjectiveFunctionFP();
			alph[i2] = H;
			float Hobj = getObjectiveFunctionFP();
			alph[i2] = tempAlpha2_fp;
			if (Lobj > (Hobj + eps)) {
				a2 = L;
			} else if (Lobj < Hobj - eps) {
				a2 = H;
			} else {
				a2 = alph2;
			}

		}

		if (a2 < 1e-8f)
			a2 = 0;
		else if (a2 > C - 1e-8f)
			a2 = C;

		if (Math.abs(a2 - alph2) < eps * (a2 + alph2 + eps))
			return false;

		a1 = alph1 + s_fp * (alph2 - a2);

		// Update threshold to reflect change in Lagrange multipliers
		bias = E1 + target[i1] * (a1 - alph1) * getKernelOutputFloat(i1, i1)
				+ target[i2] * (a2 - alph2) * k12 + bias;
		P("bias:" + bias);

		// Update weight vector to reflect change in a1 & a2, if linear SVM

		// Update error cache using new Lagrange multipliers

		// Store a1 in the alpha array
		alph[i1] = a1;
		// Store a2 in the alpha array
		alph[i2] = a2;

		// Checking (can be removed later)
		P("f(i 0):" + getFunctionOutputFloat(0, false));
		P("f(i 1):" + getFunctionOutputFloat(1, false));

		takeStepCount++;

		return true;
	}

	// METHODS////////////////////////////////////////
	/**
	 * Method examineExample, which will take a step using a number of
	 * heuristics. The points coming into this method is from the outer in the
	 * SMO main routine: first choice heuristic
	 * 
	 * @param i2
	 *            - zero based index of second point to classify, which is
	 *            chosen by the outer loop in smo
	 * @return true if it was possible to take a step
	 */
	static boolean examineExample() {
		y2 = target[i2];
		alph2 = alph[i2];
		P("S: alph2:" + alph2);
		E2 = getfFP(i2) - target[i2];
		P("f2:" + getfFP(i2));

		// TODO: i1 will/should be set here
		return takeStep();
	}

	static int changed;
	static boolean inner_loop_success;

	/**
	 * Method innerLoop, which is the inner loop that iterates until the
	 * examples in the inner loop are self consistent.
	 * 
	 * @return the number of changed examples
	 */
	static int innerLoop() {
		changed = 0;
		inner_loop_success = true;
		return changed;
	}

	/**
	 * Method initParams, which will init the parameters of the SMO algorithm.
	 * This method should only be called once, which would be just before the
	 * mainRoutine().
	 */
	static void initParams() {

		m = target.length;
		n = point[0].length;
		// initialize alpha array to zero
		alph = new float[m];

		C = FloatUtil.mul(FloatUtil.ONE, FloatUtil.intToFp(1));
		bias = 0;
		eps = FloatUtil.div(FloatUtil.ONE, FloatUtil.intToFp(100));
		// System.out.println("Cb initParams()");
		tol = FloatUtil.div(FloatUtil.ONE, FloatUtil.intToFp(10));

		KFloat.setSigma2(FloatUtil.mul(FloatUtil.ONE, FloatUtil.ONE));
		KFloat.setKernelType(KFloat.DOTKERNEL);// GAUSSIANKERNEL or DOTKERNEL

		// KABC.setSigma2(ABC.ONE);

		// Kernel type must be set first
		KFloat.setData(point);

		int loopStartInit = 1;
	}

	/**
	 * Method getf, which calculates and returns the functional output without
	 * using a bias_fp. see keerti99
	 * 
	 * @param i
	 *            - index
	 * @return the non-biased functional output.
	 */
	static float getfFP(int i) {
		float f_fp = 0;
		for (int j = 0; j < m; j++) {
			if (alph[j] > 0) {
				f_fp += target[j] * alph[j] * getKernelOutputFloat(i, j);
			}
		}
		f_fp -= bias;
		return f_fp;
	}

	/**
	 * Method to check if the example violates the KKT conditions. The method
	 * assumes that the constrained Lagrange multipliers has been explicitly set
	 * to the the appropriate boundary value zero or C. This equation relates to
	 * equation 12.2 in Platts paper.
	 * 
	 * @param p
	 *            the example to check
	 * @return true if example violates KKT
	 */
	static boolean isKktViolated(int p) {
		boolean violation = true;
		float f_fp = getFunctionOutputFloat(p, false);
		// Is alpha_fp on lower bound?
		if (alph[p] == 0) {
			if (FloatUtil.mul(target[p], f_fp) >= FloatUtil.sub(1, eps)) {
				violation = false;
			}
		} // or is alpha_fp in non-bound, NB, set?
		else if (alph[p] > 0 && alph[p] < C) {
			if (FloatUtil.mul(target[p], f_fp) > FloatUtil.sub(1, eps)
					&& FloatUtil.mul(target[p], f_fp) < FloatUtil.add(1, eps)) {
				violation = false;
			}
		} // alpha_fp is on upper bound
		else {
			if (FloatUtil.mul(target[p], f_fp) <= FloatUtil.add(1, eps)) {
				violation = false;
			}
		}
		return violation;
	}

	/**
	 * Method getObjectiveFunction, which calculates and returns the value of
	 * the objective function based on the values in the alpha_fp array.
	 * 
	 * @return the objective function (6.1 in Christianini).
	 */
	static float getObjectiveFunctionFP() {
		// TODO: Check how often this is called and tune if possible
		float objfunc_fp = 0;
		for (int i = 0; i < m; i++) {
			// Don't do the calculation for zero alphas
			if (alph[i] > 0) {
				objfunc_fp = objfunc_fp + alph[i];
				for (int j = 0; j < m; j++) {
					if (alph[j] > 0) {
						objfunc_fp = FloatUtil.sub(objfunc_fp, FloatUtil.mul(
								FloatUtil.mul(FloatUtil
										.mul(FloatUtil.mul(FloatUtil.mul(
												FloatUtil.HALF, target[i]),
												target[j]), alph[i]), alph[j]),
								getKernelOutputFloat(i, j)));
					}
				}
			}
		}
		return objfunc_fp;
	}

	/**
	 * Method calculatedError, which calculates the error from from scratch.
	 * 
	 * @param p
	 *            - point to calculte error for
	 * @return calculated error
	 */
	static float getCalculatedErrorFP(int p) {
		return FloatUtil.sub(getFunctionOutputFloat(p, false), target[p]);
	}

	/**
	 * Only one executor allowed.
	 */
	static ParallelExecutor pe = new ParallelExecutor();

	/**
	 * Method getFunctionOutput, which will return the functional output for
	 * point p.
	 * 
	 * @param p
	 *            - the point index
	 * @param parallel
	 *            - true if to be done in parallel
	 * @return the functinal output
	 */
	static float getFunctionOutputFloat(int p, boolean parallel) {
		float functionalOutput_fp = 0;
		SVMHelp.p = p;
		if (parallel) {
			SVMHelp.functionalOutput_fp = 0.0f;
			System.out.print("m:");
			System.out.println(m);
			pe.executeParallel(new SVMHelp(), m);
			SVMHelp.functionalOutput_fp -= bias;
			functionalOutput_fp = SVMHelp.functionalOutput_fp;
		} else {
			for (int i = 0; i < m; i++) {
				// Don't do the kernel if it is epsequal
				if (alph[i] > 0) {
					functionalOutput_fp += target[i] * alph[i]
							* getKernelOutputFloat(i, p);
				}
			} // Make a check here to see any alphas has been modified after
			functionalOutput_fp -= bias;
		}
		return functionalOutput_fp;
	}

	/**
	 * Method getKernelOutput, which returns the kernel of two points.
	 * 
	 * @param i1
	 *            - index of alpha_fp 1
	 * @param i2
	 *            - index of alpha_fp 2
	 * @return kernel output
	 */
	static float getKernelOutputFloat(int i1, int i2) {

		return KFloat.kernel(i1, i2);
	}

	/**
	 * Method getEta, which returns eta_fp = 2*k12-k11-k22
	 * 
	 * @param i1
	 *            - index of first point
	 * @param i2
	 *            -index of second point
	 * @return double - eta_fp
	 */
	static float getEtaFP(int i1, int i2) {
		float eta_fp;
		float eta_fp_tmp;
		float kernel11_fp, kernel22_fp, kernel12_fp;
		kernel11_fp = getKernelOutputFloat(i1, i1);
		kernel22_fp = getKernelOutputFloat(i2, i2);
		kernel12_fp = getKernelOutputFloat(i1, i2);
		eta_fp = FloatUtil.sub(FloatUtil.sub(FloatUtil.mul(FloatUtil.TWO,
				kernel12_fp), kernel11_fp), kernel22_fp);
		return eta_fp;
	}

	/**
	 * Method getLowerClip, which returns the lower clip value for some pair of
	 * Lagrange multipliers. Pls. refer to Nello's book for more info.
	 * 
	 * @param i1
	 *            - first point
	 * @param i2
	 *            - second point
	 * @return the lower clip value
	 */
	static float getLowerClipFP(int i1, int i2) {
		float u_fp = 0;
		if (target[i1] == target[i2]) {
			u_fp = FloatUtil.sub(FloatUtil.add(alph[i1], alph[i2]), C);
			if (u_fp < 0) {
				u_fp = 0;
			}
		} else {
			u_fp = FloatUtil.sub(alph[i2], alph[i1]);
			if (u_fp < 0) {
				u_fp = 0;
			}
		}
		return u_fp;
	}

	/**
	 * Method getUpperClip, which will return the upper clip based on two
	 * Lagrange multipliers.
	 * 
	 * @param i1
	 *            - first point
	 * @param i2
	 *            - second point
	 * @return the upper clip
	 */
	static float getUpperClipFP(int i1, int i2) {
		float v_fp = 0;
		if (target[i1] == target[i2]) {
			v_fp = FloatUtil.add(alph[i1], alph[i2]);
			if (v_fp > C) {
				v_fp = C;
			}
		} else {
			v_fp = FloatUtil.add(C, FloatUtil.sub(alph[i2], alph[i1]));
			if (v_fp > C) {
				v_fp = C;
			}
		}
		return v_fp;
	}

	static public int getTrainingErrorCountFP() {
		P("getTrainingErrorCountFP");
		int errorCount = 0;
		for (int i = 0; i < m; i++) {
			float fout_fp = getFunctionOutputFloat(i, false);
			System.out.print("Tr ");
			System.out.print(i);
			System.out.print(" fn ");
			System.out.print(fout_fp);
			System.out.print(" y_fp ");
			System.out.println(target[i]);
			if (fout_fp > 0 && target[i] < 0) {
				errorCount++;
				System.out.println(" e 1 ");
			} else if (fout_fp < 0 && target[i] > 0) {
				errorCount++;
				System.out.println(" e 0 ");
			}
		}
		return errorCount;
	}

	/**
	 * Method calculateW, which calculates the weight vector. This is used for
	 * linear SVMs.
	 * 
	 * @return the weight [n] vector
	 */
	static float[] calculateWFP() {
		float[] w_fp;
		w_fp = new float[n];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				w_fp[j] = FloatUtil.add(w_fp[j], FloatUtil.mul(FloatUtil.mul(
						target[i], alph[i]), point[i][j]));
			}
		}
		return w_fp;
	}

	/**
	 * Method isExampleBound, which will return true if the point p is on the
	 * bound as defined as less then (0+tol_fp) or greater than (C-tol_fp).
	 * 
	 * @param p
	 *            - index of point
	 * @return true if p is on bound
	 */
	static boolean isExampleOnBound(int p) {
		return alph[p] < tol || alph[p] > FloatUtil.sub(C, tol);
	}

	/**
	 * Method getFunctionOutput, which will return the functional output for
	 * point represented by a input vector only.
	 * 
	 * @param xtest
	 *            - the input vector
	 * @return the functinal output
	 */
	static public float getFunctionOutputTestPointFP(float[] xtest) {
		float functionalOutput_fp = 0;
		float[][] data_fp_local = point;
		int m = data_fp_local.length;
		float func_out = 0;
		// System.out.println("---ALIVE1m---" + m);
		int n = xtest.length;
		int n2 = data_fp_local[0].length;
		// System.out.println("---ALIVE1n2---" + n2);
		// System.out.println("---ALIVE1n---" + n);
		// System.out.println("---ALIVE11---");
		// RT bound it to ALPHA_RT
		for (int i = 0; i < ALPHA_RT; i++) { // @WCA loop=2
			// System.out.println("---ALIVE1111---" + i);

			n = xtest.length;
			// MS: is the following bound correct?
			while (n != 0) { // @WCA loop=2
				n = n - 1;
				// System.out.println("---ALIVEnin---" + n);
				// System.out.println("---ALIVEnim---" + m);
				// functionalOutput_fp += KABC.kernelX(i);
				// TODO
				// func_out += (data_fp_local[alpha_index_sorted[i]][n])
				// * (xtest[n]);
			}
			// if (alpha_fp[alpha_index_sorted[i]] > 0) {
			// functionalOutput_fp += func_out;
			// }
			func_out = 0;
		}
		functionalOutput_fp -= bias;
		return functionalOutput_fp;
	}

	/**
	 * Method getFunctionOutput, which will return the functional output for
	 * point represented by a input vector only.
	 * 
	 * @param xtest
	 *            - the input vector
	 * @return the functinal output
	 */
	static public float getFunctionOutputTestPointFP_OOAD(float[] xtest) {
		float functionalOutput_fp = 0;
		float kernelOutput_fp = 0;
		KFloat.setX(xtest);
		for (int i = 0; i < m; i++) {
			// Don't do the kernel if it is epsequal
			if (alph[i] > 0) {
				kernelOutput_fp = KFloat.kernelX(i);
				functionalOutput_fp = FloatUtil.add(functionalOutput_fp,
						FloatUtil.mul(FloatUtil.mul(alph[i], target[i]),
								kernelOutput_fp));
			}
		} // Make a check here to see any alphas has been modified after
		functionalOutput_fp = FloatUtil.sub(functionalOutput_fp, bias);
		return functionalOutput_fp;
	}

	static public void setData_fp(float[][] data_fp) {
		SMOBinaryClassifierFloat.point = data_fp;
	}

	static public void setY_fp(float[] y_fp) {
		SMOBinaryClassifierFloat.target = y_fp;
	}

	static void gccheck() {
		System.out.print("GC free words ");
		// System.out.println(GC.free());
	}

	static void sp() {
		System.out.print("sp=");
		System.out.println(Native.rd(com.jopdesign.sys.Const.IO_WD));
	}

	static public void smoInfo() {
		// printScalar("wd",Native.rd(Const.IO_WD)); //TODO: Can it be read?
		System.out.println("======SMO INFO START======");
		// printScalar("sp",Native.rd(com.jopdesign.sys.Const.IO_WD));
		printScalar("i1", i1);
		printScalar("i2", i2);
		printScalar("bias_fp", bias);
		printScalar("m", m);
		printScalar("n", n);
		printMatrix("data_fp", point);
		printVector("y_fp", target);
		printVector("alpha_fp", alph);
		printScalar("C", C);
		printScalar("tol", tol);
		printScalar("eps", eps);
		printScalar("takeStepCount", takeStepCount);
		int svs = 0;
		for (int i = 0; i < m; i++) {
			if (alph[i] > tol)
				svs++;
		}
		printScalar("#sv", svs);
		printScalar("training err cnt", getTrainingErrorCountFP());

		for (int i = 0; i < 100000; i++)
			;
		System.out.println("======SMO INFO END======");
	}

	static void printBoolean(String str, boolean b) {
		System.out.print(str);
		System.out.print(':');
		if (b)
			System.out.println("true");
		else
			System.out.println("false");
	}

	static void printScalar(String str, int sca) {
		System.out.print(str);
		System.out.print(':');
		System.out.println(sca);
		for (int i = 0; i < 100; i++)
			;
	}

	static void printScalar(String str, float sca) {
		System.out.print(str);
		System.out.print(':');
		System.out.println(sca);
		for (int i = 0; i < 100; i++)
			;
	}

	static void printVector(String str, float[] ve) {
		System.out.print(str);
		System.out.print(" {");
		for (int i = 0; i < ve.length; i++) {
			System.out.print(i);
			System.out.print(':');
			System.out.print(ve[i]);
			if (i < (ve.length - 1))
				System.out.print(", ");

			for (int j = 0; j < 1000; j++)
				;
		}
		System.out.println("}");
		for (int i = 0; i < 1000; i++)
			;
	}

	static void printVector(String str, int[] ve) {
		System.out.print(str);
		System.out.print(" {");
		for (int i = 0; i < ve.length; i++) {
			System.out.print(i);
			System.out.print(':');
			System.out.print(ve[i]);
			if (i < (ve.length - 1))
				System.out.print(", ");

			for (int j = 0; j < 1000; j++)
				;
		}
		System.out.println("}");
		for (int i = 0; i < 1000; i++)
			;
	}

	static void printMatrix(String str, float[][] ma) {
		for (int i = 0; i < ma.length; i++) {
			System.out.print(str);
			System.out.print("[");
			System.out.print(i);
			System.out.print("]");
			System.out.print(":");
			printVector("", ma[i]);
		}
	}

	static public int getSV() {
		int svs = 0;
		for (int i = 0; i < m; i++) {
			if (alph[i] > tol)
				svs++;
		}
		return svs;
	}

	static void P(String s) {
		if (PRINT)
			System.out.println(s);
	}

	/**
	 * Do not instanciate me.
	 */
	private SMOBinaryClassifierFloat() {
	}

	private static class SVMHelp implements Execute {

		static Object lock;

		static int p; // the test point index

		static float functionalOutput_fp;

		public void execute(int nr) {
			// System.out.println("Parallel in core 0:nr=" + nr);
			if (alph[nr] > 0) {

				float kernelOutput_fp = getKernelOutputFloat(nr, p);
				float tmp = ((alph[nr] * target[nr]) * kernelOutput_fp);
				synchronized (lock) {
					functionalOutput_fp += tmp;
					// functionalOutput_fp += bias_fp;
				}
			}
		}

		void setP(int p) {
			SVMHelp.p = p;
			functionalOutput_fp = 0;
		}

		float getFuncOut() {
			return functionalOutput_fp;
		}
	}

	// First measure
	public static void measure() {

		int time = 0;
		float ser0, ser1, par0, par1;

		// serial
		time = Native.rd(Const.IO_US_CNT);
		ser0 = getFunctionOutputFloat(0, false);
		ser1 = getFunctionOutputFloat(1, false);
		time = Native.rd(Const.IO_US_CNT) - time;

		P("Serial time=" + time);
		P("f(i 0):" + ser0);
		P("f(i 1):" + ser1);

		// parallel
		time = Native.rd(Const.IO_US_CNT);
		par0 = getFunctionOutputFloat(0, true);
		par1 = getFunctionOutputFloat(1, true);
		time = Native.rd(Const.IO_US_CNT) - time;

		P("Parrallel time=" + time);
		P("f(i 0):" + par0);
		P("f(i 1):" + par1);

	}

}