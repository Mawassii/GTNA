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
 * Evaluation.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: Nico;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import gtna.graph.Graph;
import gtna.io.Filewriter;
import gtna.io.GraphWriter;
import gtna.networks.Network;
import gtna.networks.model.BarabasiAlbert;
import gtna.networks.model.ErdosRenyi;
import gtna.routing.RoutingAlgorithm;
import gtna.routing.greedy.Greedy;
import gtna.routing.greedy.GreedyBacktracking;
import gtna.routing.lookahead.LookaheadSequential;
import gtna.transformation.Transformation;
import gtna.transformation.edges.Bidirectional;
import gtna.transformation.gd.*;
import gtna.transformation.partition.GiantConnectedComponent;
import gtna.transformation.partition.WeakConnectivityPartition;
import gtna.transformation.spanningtree.BFS;
import gtna.util.Config;
import gtna.util.Stats;

/**
 * @author Nico
 * 
 */
public class GDAEvaluation {
	public static void main(String[] args) {
		Stats stats = new Stats();
		int times = 5;
		int threads = 10;
		int sizeFactor = 1000;
		int degree = 10;

		ProgressMonitor pm = new ProgressMonitor();

		Config.overwrite("METRICS", "ROUTING");
		// R for routing
		Config.overwrite("MAIN_DATA_FOLDER", "./data/GDAevaluation/");
		Config.overwrite("MAIN_PLOT_FOLDER", "./plots/GDAevaluation/");
		Config.overwrite("GNUPLOT_PATH", "C:\\Cygwin\\bin\\gnuplot.exe");
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "" + true);

		Transformation[] sTArray;
		Network nw;
		ConcurrentHashMap<String, Integer> lastCounter = new ConcurrentHashMap<String, Integer>();

		ArrayList<Network> todoList = new ArrayList<Network>();
		RoutingAlgorithm[] rA = new RoutingAlgorithm[] { new Greedy(25), new GreedyBacktracking(25),
				new LookaheadSequential(25) };
		GraphDrawingAbstract[] t = new GraphDrawingAbstract[] { new CanonicalCircularCrossing(1, 100, true, null),
				new SixTollis(1, 100, true, null), new WetherellShannon(100, 100, null), new Knuth(100, 100, null),
				new MelanconHerman(100, 100, null), new BubbleTree(100, 100, null),
				new FruchtermanReingold(1, new double[] { 100, 100 }, false, 100, null) };
		for (GraphDrawingAbstract originalT : t) {

			for (int i = 1; i <= 10; i++) {
				for (int j = 0; j < times; j++) {
					GraphDrawingAbstract singleT = originalT.clone();

					if (singleT instanceof HierarchicalAbstract) {
						sTArray = new Transformation[] { new Bidirectional(), new WeakConnectivityPartition(),
								new GiantConnectedComponent(), new BFS("hd"), singleT };
					} else {
						sTArray = new Transformation[] { new Bidirectional(), new WeakConnectivityPartition(),
								new GiantConnectedComponent(), singleT };
					}

					nw = new ErdosRenyi(i * sizeFactor, degree, true, null, sTArray);
					lastCounter.put((i * sizeFactor) + "/" + nw.folder(), 0);
					todoList.add(nw);

					singleT = originalT.clone();

					if (singleT instanceof HierarchicalAbstract) {
						sTArray = new Transformation[] { new Bidirectional(), new WeakConnectivityPartition(),
								new GiantConnectedComponent(), new BFS("hd"), singleT };
					} else {
						sTArray = new Transformation[] { new Bidirectional(), new WeakConnectivityPartition(),
								new GiantConnectedComponent(), singleT };
					}

					nw = new BarabasiAlbert(i * sizeFactor, degree, null, sTArray);
					lastCounter.put((i * sizeFactor) + "/" + nw.folder(), 0);
					todoList.add(nw);
				}
			}
		}

		NetworkThread[] nwThreads = new NetworkThread[threads];
		for (int i = 0; i < threads; i++) {
			nwThreads[i] = new NetworkThread(i, lastCounter, pm);
		}

		Collections.shuffle(todoList);

		int counter = 0;
		for (Network n : todoList) {
			// System.out.println(n.nodes() + "/" + n.folder());
			nwThreads[counter].add(n);
			counter = (counter + 1) % threads;
		}

		pm.output("Starting to generate " + todoList.size() + " networks");
		pm.setMaxNetworks(todoList.size());
		
		for (int i = 0; i < threads; i++) {
			nwThreads[i].start();
		}
		for (int i = 0; i < threads; i++) {
			try {
				nwThreads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		stats.end();
	}

	private static class ProgressMonitor {
		private Date start;
		private int generatedNetworks = 0;
		private int maxNetworks;

		public ProgressMonitor() {
			start = new Date();
			output("Started ProgressMonitor");
		}

		public void setMaxNetworks(int mNW) {
			this.maxNetworks = mNW;
		}

		public void tickNW() {
			generatedNetworks++;
			double runtime = new Date().getTime() - start.getTime();
			output("Generated network " + generatedNetworks + " of " + maxNetworks + " after running for " + (runtime/1000) + " seconds");
		}

		public void output(String text) {
			System.out.println((new Date()) + " -- " + text);
		}
	}

	private static class NetworkThread extends Thread {
		private ArrayList<Network> nws;
		private ConcurrentHashMap<String, Integer> lastCounter;
		private ProgressMonitor pm;
		private int id;

		public NetworkThread(int id, ConcurrentHashMap<String, Integer> lastCounter, ProgressMonitor pm) {
			this.nws = new ArrayList<Network>();
			this.lastCounter = lastCounter;
			this.pm = pm;
			this.id = id;
		}

		public void add(Network nw) {
			this.nws.add(nw);
		}

		public void run() {
			for (Network nw : nws) {
				Graph g = nw.generate();
				int graphSize = g.getNodes().length;

				String folderName = "./resources/evaluation/" + graphSize + "/" + nw.folder();
				int i = lastCounter.get(graphSize + "/" + nw.folder());
				lastCounter.put(graphSize + "/" + nw.folder(), (i + 1));

				String fileName = folderName + i + ".txt";
				if (isCompleteDataset(fileName)) {
					/*
					 * This seems to be a complete data file: the lookahead list
					 * is generated as the last transformation, and if a list
					 * was exported, the whole set of transformations was
					 * already done
					 */
					pm.output("Skipping " + fileName);
					pm.tickNW();
					continue;
				}

				pm.output("Thread " + id + " starts generation of " + fileName);
				
				double startTime = System.currentTimeMillis();
				Filewriter runtimeLogger = new Filewriter(fileName + ".RUNTIME");
				for (Transformation t : nw.transformations()) {
					g = t.transform(g);
					runtimeLogger.writeln(t.key() + ":" + (System.currentTimeMillis() - startTime));
					startTime = System.currentTimeMillis();
				}
				runtimeLogger.close();

				GraphWriter.writeWithProperties(g, fileName);
				pm.output("Thread " + id + " wrote " + fileName + " after " + (System.currentTimeMillis() - startTime)/1000 + " seconds");
				pm.tickNW();
			}
		}

		public boolean isCompleteDataset(String prefix) {
			File temp;
			temp = new File(prefix + "_LOOKAHEAD_LIST_0");
			if (!temp.exists()) {
				// return false;
			}
			temp = new File(prefix + ".RUNTIME");
			if (!temp.exists()) {
				return false;
			}
			temp = new File(prefix + "_ID_SPACE_0");
			if (!temp.exists()) {
				return false;
			}
			return true;
		}
	}
}