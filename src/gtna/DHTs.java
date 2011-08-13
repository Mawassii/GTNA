/* ===========================================================
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
 * DHTs.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: benni;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna;

import gtna.data.Series;
import gtna.networks.Network;
import gtna.networks.p2p.chord.Chord;
import gtna.plot.Plot;
import gtna.routing.RoutingAlgorithm;
import gtna.routing.greedy.GreedyBI;
import gtna.util.Config;
import gtna.util.Stats;

/**
 * @author benni
 * 
 */
public class DHTs {
	public static void main(String[] args) {
		Stats stats = new Stats();
		chordTest();
		stats.end();
	}

	public static void chordTest() {
		Config.overwrite("METRICS", "DD, R");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/chord/");
		Config.overwrite("MAIN_PLOT_FOLDER", "./plots/chord/");
		Config.overwrite("GNUPLOT_PATH", "/sw/bin/gnuplot");
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "true");

		Config.overwrite("R_ROUTES_PER_NODE", "5");

		int[] nodes = new int[] { 10000, 20000 };
		int[] bits = new int[] { 32, 64, 128 };
		boolean[] uniform = new boolean[] { true, false };
		int[] rpn = new int[] { 5, 10, 25, 50 };

		for (int n : nodes) {
			for (int b : bits) {
				for (boolean u : uniform) {
					for (int r : rpn) {
						Network nw = new Chord(n, b, u, r, new GreedyBI(), null);
						// Series.generate(nw, 1);
					}
				}
			}
		}

		RoutingAlgorithm ra = new GreedyBI();

		Network[] nw1 = Chord.get(nodes, 128, true, 50, ra, null);
		Network[] nw2 = Chord.get(nodes, 128, false, 50, ra, null);
		Network[] nw3 = Chord.get(20000, bits, true, 50, ra, null);
		Network[] nw4 = Chord.get(20000, bits, false, 50, ra, null);
		Network[] nw5 = Chord.get(20000, 128, uniform, 50, ra, null);
		Network[] nw6 = Chord.get(20000, 128, true, rpn, ra, null);
		Network[] nw7 = Chord.get(20000, 128, false, rpn, ra, null);

		Network[][] nw = new Network[][] { nw1, nw2, nw3, nw4, nw5, nw6, nw7 };

		Series[][] s = Series.get(nw);

		for (int i = 0; i < s.length; i++) {
			Network n1 = s[i][0].network();
			Network n2 = s[i][1].network();
			String name = n1.description(n2);
			Plot.multiAvg(s[i], name + "/");
		}

		// Network[] nw1 = Chord.get(nodes[0], bits[0], true, rpn, new
		// GreedyBI(), null);
		// Network[] nw2 = Chord.get(nodes[0], bits[0], false, rpn, new
		// GreedyBI(), null);
		// Series[] s1 = Series.get(nw1);
		// Series[] s2 = Series.get(nw2);
		// Plot.multiConf(s1, "uniform/");
		// Plot.multiConf(s2, "random/");

	}
}