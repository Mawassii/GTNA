/*
 * ===========================================================
 * GTNA : Graph-Theoretic Network Analyzer
 * ===========================================================
 * 
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors
 * 
 * Project Info:  http://www.p2p.tu-darmstadt.de/research/gtna/
 * 
 * GTNA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GTNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * ---------------------------------------
 * ConfidenceData.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 * 
 * Original Author: Benjamin Schiller;
 * Contributors:    -;
 * 
 * Changes since 2011-05-17
 * ---------------------------------------
 */
package gtna.data;

import gtna.io.DataReader;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.util.Config;
import gtna.util.Util;

import java.util.ArrayList;

public class ConfidenceData extends Data {
	public static void generate(String destFolder, String[] folders,
			Metric[] metrics) {
		for (Metric m : metrics) {
			String temp2 = destFolder + m.folder() + "/";
			String[] temp1 = new String[folders.length];
			for (int i = 0; i < folders.length; i++) {
				temp1[i] = folders[i] + m.folder() + "/";
			}
			for (String d : m.dataKeys()) {
				boolean cdf = Config.getBoolean(d + "_DATA_IS_CDF");
				ConfidenceData.generate(temp2, temp1, d, cdf);
			}
		}
		// ConfidenceData.generateOld(destFolder, folders);
		// String[] data = Config.getData();
		// for (String d : data) {
		// boolean cdf = Config.getBoolean(d + "_DATA_IS_CDF");
		// ConfidenceData.generate(destFolder, folders, d, cdf);
		// }
	}

	private static void generate(String destFolder, String[] folders,
			String data, boolean cdf) {
		double[][][] v1 = AverageData.readValues(data, folders);
		int maxLength = AverageData.maxLength(v1);
		double[][] max = AverageData.max(v1);
		double[][][] values = new double[v1.length][maxLength][2];
		for (int i = 0; i < v1.length; i++) {
			ConfidenceData.fill(values[i], max, v1[i], cdf);
		}
		double[][] avg = ConfidenceData.computeConf(values);
		DataWriter.writeWithoutIndex(avg, data, destFolder);
	}

	private static double[][] computeConf(double[][][] values) {
		double[][] conf = new double[values[0].length][6];
		for (int i = 0; i < values[0].length; i++) {
			double[] y = ConfidenceData.getY(values, i);

			double x = values[0][i][0];
			double min = Util.min(y);
			double avg = Util.avg(y);
			double max = Util.max(y);

			double alpha = 1 - Config.getDouble("CONFIDENCE_INTERVAL");
			double sd = Util.getStandardDeviation(y);
			double[] ci = Util.getConfidenceInterval(avg, sd, y.length, alpha);

			conf[i][0] = x;
			conf[i][1] = min;
			conf[i][2] = ci[0];
			conf[i][3] = avg;
			conf[i][4] = ci[1];
			conf[i][5] = max;
		}
		return conf;
	}

	private static double[] getY(double[][][] values, int index) {
		double[] y = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			y[i] = values[i][index][1];
		}
		return y;
	}

	// private static void generateOld(String destFolder, String[] folders) {
	// double alpha = 1 - Config.getDouble("CONFIDENCE_INTERVAL");
	// String[] DATA = Config.getData();
	// for (int i = 0; i < DATA.length; i++) {
	// ArrayList<double[][]> values = new ArrayList<double[][]>();
	// for (int j = 0; j < folders.length; j++) {
	// String filename = folders[j]
	// + Config.get(DATA[i] + "_DATA_FILENAME")
	// + Config.get("DATA_EXTENSION");
	// double[][] currentValues = DataReader.readDouble2D(filename);
	// for (int k = 0; k < currentValues.length; k++) {
	// double x = currentValues[k][0];
	// double value = currentValues[k][1];
	// if (k >= values.size()) {
	// values.add(init(folders.length));
	// }
	// values.get(k)[j][0] = x;
	// values.get(k)[j][1] = value;
	// }
	// }
	//
	// double[][] data = new double[values.size()][6];
	// // 0: x
	// // 1: min
	// // 2: conf-min
	// // 3: mean
	// // 4: conf-max
	// // 5: max
	// for (int j = 0; j < values.size(); j++) {
	// double min = Double.MAX_VALUE / 2;
	// double avg = Double.NaN;
	// double max = Double.MIN_VALUE / 2;
	// double[][] currentValues = values.get(j);
	// int counter = 0;
	// double x = 0;
	// double avgSum = 0;
	// for (int k = 0; k < currentValues.length; k++) {
	// if (!Double.isNaN(currentValues[k][1])) {
	// double currentValue = currentValues[k][1];
	// if (min > currentValue) {
	// min = currentValue;
	// }
	// if (max < currentValue) {
	// max = currentValue;
	// }
	// x += currentValues[k][0];
	// avgSum += currentValue;
	// counter++;
	// }
	// }
	// if (counter > 0) {
	// avg = (double) avgSum / (double) counter;
	// x = (double) x / (double) counter;
	// }
	// double standardDeviation = Util
	// .getStandardDeviation(values(currentValues));
	// double[] ci = Util.getConfidenceInterval(avg,
	// standardDeviation, counter, alpha);
	// data[j][0] = x;
	// data[j][1] = min;
	// data[j][2] = ci[0];
	// data[j][3] = avg;
	// data[j][4] = ci[1];
	// data[j][5] = max;
	// }
	// DataWriter.writeWithoutIndex(data, DATA[i], destFolder);
	// }
	// }

	public static double[] getConfidenceInterval(double x, double[] values) {
		// 0: x
		// 1: min
		// 2: conf-min
		// 3: mean
		// 4: conf-max
		// 5: max
		double alpha = 1 - Config.getDouble("CONFIDENCE_INTERVAL");
		double min = Util.min(values);
		double avg = Util.avg(values);
		double max = Util.max(values);
		double standardDeviation = Util.getStandardDeviation(values);
		double[] ci = Util.getConfidenceInterval(avg, standardDeviation,
				values.length, alpha);
		double[] conf = new double[6];
		conf[0] = x;
		conf[1] = min;
		conf[2] = ci[0];
		conf[3] = avg;
		conf[4] = ci[1];
		conf[5] = max;
		return conf;
	}

	private static double[] values(double[][] data) {
		double[] values = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			values[i] = data[i][1];
		}
		return values;
	}

	private static double[][] init(int length) {
		double[][] array = new double[length][2];
		for (int i = 0; i < length; i++) {
			array[i][0] = Double.NaN;
			array[i][1] = Double.NaN;
		}
		return array;
	}
}
