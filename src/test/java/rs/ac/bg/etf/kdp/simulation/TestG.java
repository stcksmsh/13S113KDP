package rs.ac.bg.etf.kdp.simulation;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import rs.ac.bg.etf.sleep.simulation.*;

public class TestG {

	public static void main(String[] args) {
		try {

			String components = "./src/test/resources/" + args[0];
			String connections = "./src/test/resources/" + args[1];
			String componentsDst ="./src/test/resources/" + args[2];

			System.out.println(Paths.get("").toAbsolutePath().toString());
			Netlist<Object> netlist = loadNetlist(components, connections);
			 Simulator<Object> simulator = new SimulatorSinglethread<Object>(1);
//			Simulator<Object> simulator = new SimulatorOptimistic<Object>(1);
//			Simulator<Object> simulator = new SimulatorMultithread<Object>(1);

			simulator.setNetlist(netlist);
			simulator.init();
			while (simulator.getlTime() < 10) {
				simulator.execute();
			}
			storeNetList(netlist, componentsDst);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void storeNetList(Netlist<Object> netlist,
			String componentsDst) throws Exception {
		PrintWriter out = new PrintWriter(new FileWriter(componentsDst));
		for (SimComponent<Object> c : netlist.getComponents().values()) {
			String[] context = c.getState();
			String contextString = "";
			for (String s : context) {
				contextString += s + " ";
			}
			contextString = contextString.trim();
			out.println(contextString);
		}
		out.close();

	}

	public static Netlist<Object> loadNetlist(String components,
			String connections) throws Exception {
		Netlist<Object> netlist = new Netlist<Object>();
		BufferedReader in = new BufferedReader(new FileReader(components));
		String s;
		while ((s = in.readLine()) != null) {
			String[] names = s.split(" ");
			netlist.addComponent(names);
		}
		in.close();
		in = new BufferedReader(new FileReader(connections));
		List<String[]> cc = new LinkedList<String[]>();
		while ((s = in.readLine()) != null) {
			String[] names = s.split(" ");
			cc.add(names);
		}
		in.close();
		String[][] con = new String[cc.size() - 1][];
		for (int i = 0; i < cc.size() - 1; i++) {
			con[i] = cc.get(i + 1);
		}
		netlist.addConnection(con);
		return netlist;
	}
}
