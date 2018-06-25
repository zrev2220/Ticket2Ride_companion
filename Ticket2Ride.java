import java.util.*;
import java.io.*;

public class Ticket2Ride
{
	private TreeMap<String, Integer> cityToInt = new TreeMap<>();
	private TreeMap<Integer, String> intToCity = new TreeMap<>();
	private TreeSet<Ticket> ticketSet = new TreeSet<>();
	private TreeMap<Integer, Integer> cityUsages = new TreeMap<>();
	private TreeSet<Ticket> blockedRoutes = new TreeSet<>();
	private int[][] adjMat;
	private int[][] apsp;
	private int[][] path;
	private boolean apspConstructed = false;

	private static Ticket2Ride instance = null;
	private final int INF = 1000000000;

	private Ticket2Ride()
	{
		// empty body
	}

	public static Ticket2Ride getInstance()
	{
		if (instance == null)
			instance = new Ticket2Ride();
		return instance;
	}

	// getter methods
	public TreeMap<String, Integer> getCityToIntMap() { return cityToInt; }
	public TreeMap<Integer, String> getIntToCityMap() { return intToCity; }
	public TreeSet<Ticket> getTicketSet() { return ticketSet; }
	public TreeMap<Integer, Integer> getCityUsages() { return cityUsages; }
	public TreeSet<Ticket> getBlockedRoutes() { return blockedRoutes; }
	public int[][] getAdjMat() { return adjMat; }
	public int[][] getApsp() { return apsp; }
	public int[][] getPath() { return path; }

	public void loadMap(String filename) throws IOException
	{
		try (BufferedReader mapFile = new BufferedReader(new FileReader(filename)))
		{
			// read cities edge list and build adjacency matrix
			int nCities = Integer.parseInt(mapFile.readLine());
			adjMat = new int[nCities][nCities];
			while (mapFile.ready())
			{
				String[] line = mapFile.readLine().split(" ");
				String cityA = line[0].toLowerCase();
				String cityB = line[1].toLowerCase();
				int w = Integer.parseInt(line[2]);
				if (!cityToInt.containsKey(cityA))
				{
					cityToInt.put(cityA, cityToInt.size());
					intToCity.put(cityToInt.size() - 1, cityA);
				}
				if (!cityToInt.containsKey(cityB))
				{
					cityToInt.put(cityB, cityToInt.size());
					intToCity.put(cityToInt.size() - 1, cityB);
				}
				int a = cityToInt.get(cityA), b = cityToInt.get(cityB);
				adjMat[a][b] = w;
				adjMat[b][a] = w; // undirected graph
			}
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		}
	}

	public void addTicket(String cityA, String cityB) throws Exception
	{
		// check if cities are identical
		if (cityA.equals(cityB))
			throw new Exception(String.format("1: Cities are identical (%s)", cityA));

		// check if ticket is already added
		Ticket newTicket = new Ticket(cityA, cityB);
		if (ticketSet.contains(newTicket))
			throw new Exception(String.format("2: Ticket already in list (%s - %s)", cityA, cityB));

		// add new ticket
		ticketSet.add(newTicket);
		// increment city usages
		cityUsages.put(newTicket.aIdx(), cityUsages.getOrDefault(newTicket.aIdx(), 0) + 1);
		cityUsages.put(newTicket.bIdx(), cityUsages.getOrDefault(newTicket.bIdx(), 0) + 1);
	}

	public void removeTicket(String cityA, String cityB) throws Exception
	{
		// ensure ticket is in <ticketSet>
		Ticket toRemove = new Ticket(cityA, cityB);
		if (!ticketSet.contains(toRemove))
			throw new Exception(String.format("Ticket not in list (%s - %s)", cityA, cityB));

		// remove ticket
		ticketSet.remove(toRemove);

		// decrement <cityUsages> entries
		cityUsages.computeIfPresent(toRemove.aIdx(), (key, value) -> value == 1 ? null : value - 1);
		cityUsages.computeIfPresent(toRemove.bIdx(), (key, value) -> value == 1 ? null : value - 1);
	}

	public void resetModel()
	{
		// remove all tickets
		ticketSet.clear();
		cityUsages.clear();
		// unblock all routes
		for (Ticket toUnblock : blockedRoutes)
		{
			try {
				blockRoute(false, toUnblock.aCity(), toUnblock.bCity());
			} catch (Exception ex) {}
		}
		blockedRoutes.clear();
	}

	public void blockRoute(boolean block, String cityA, String cityB) throws Exception
	{
		// increment/decrement edge weight by 1000000 to prevent/allow its use in path computations
		int a = cityToInt.get(cityA);
		int b = cityToInt.get(cityB);
		int delta = block ? 1 : -1;
		Ticket route = new Ticket(a, b);
		if (!(blockedRoutes.contains(route) ^ block))
		{
			throw new Exception(String.format("1 - Route already %sblocked (%s - %s)", !block ? "un" : "", cityA, cityB));
		}
		else if (block && adjMat[a][b] == 0)
		{
			throw new Exception(String.format("2 - Cities not adjacent (%s - %s)", cityA, cityB));
		}
		else
		{
			adjMat[a][b] += 1000000 * delta;
			adjMat[b][a] += 1000000 * delta;
			apspConstructed = false;
			if (block)
				blockedRoutes.add(route);
			else
				blockedRoutes.remove(route);
		}
	}

	public Pair<Integer, ArrayList<OrderedTriple<Integer, Integer, Integer>>> steinerTreeApprox() throws Exception
	{
		// minimum spanning tree: shortest path to connect all cities
		// actually NP-hard Steiner Tree problem
		// will use 2-approximation technique to solve

		// step 1: build metric closure of cities (in form of edge list)
		ArrayList<OrderedTriple<Integer, Integer, Integer>> edgeList = new ArrayList<>();
		for (int city : cityUsages.keySet())
		{
			for (int city2 : cityUsages.keySet())
			{
				if (city != city2)
				{
					OrderedTriple<Integer, Integer, Integer> edge = new OrderedTriple<>(apsp[city][city2], city, city2); // form: weight, u, v; sort by weight
					edgeList.add(edge);
				}
			}
		}
		Collections.sort(edgeList);

		// step 2: find MST of cities on metric closure graph
		UnionFind unionFind = new UnionFind(cityToInt.size());
		ArrayList<OrderedTriple<Integer, Integer, Integer>> mst = new ArrayList<>();
		int mst_cost = 0;
		for (int i = 0; i < edgeList.size(); ++i)
		{
			OrderedTriple<Integer, Integer, Integer> nextEdge = edgeList.get(i);
			if (!unionFind.isSameSet(nextEdge.second(), nextEdge.third()))
			{
				mst.add(nextEdge);
				mst_cost += nextEdge.first();
				if (mst_cost >= 1000000)
				{
					// cost over 1 million, so algorithm had no choice but to use a blocked route
					throw new Exception("Unable to connect all cities");
				}
				unionFind.unionSet(nextEdge.second(), nextEdge.third());
			}
			if (unionFind.sizeOfSet(nextEdge.second()) == cityUsages.size())
			{
				// all cities connected, can quit early
				break;
			}
		}
		edgeList.clear(); // don't need this anymore

		// step 3: expand edges in metric closure mst to full paths in original graph
		TreeSet<OrderedTriple<Integer, Integer, Integer>> routes = new TreeSet<>();
		for (OrderedTriple<Integer, Integer, Integer> edge : mst)
		{
			// for each edge in mst...
			// ...follow path in <path> and add each edge to <routes>
			int currentV = edge.second();
			int lastV = edge.third();
			while (currentV != lastV)
			{
				int nextV = path[currentV][lastV];
				if (!routes.add(new OrderedTriple<>(currentV, nextV, 0))) {
					// duplicate edge! reduce cost
					mst_cost -= adjMat[currentV][nextV];
				}
				currentV = nextV;
			}
		}

		// step 4: return cost and routes to claim
		return new Pair<>(mst_cost, new ArrayList<>(routes));
	}

	public void steinerTreeExact()
	{
		throw new UnsupportedOperationException("Not supported yet");
	}

	public void modifiedTSP()
	{
		throw new UnsupportedOperationException("Not supported yet");
	}

	public void buildAPSP()
	{
		if (apspConstructed)
			return; // apsp already up-to-date

		// from adjMat, construct APSP table
		apsp = new int[adjMat.length][adjMat.length];
		path = new int[adjMat.length][adjMat.length];
		// copy adjMat to apsp table to start with
		// and set intial paths
		for (int i = 0; i < apsp.length; ++i)
		{
			for (int j = 0; j < apsp[i].length; ++j)
			{
				// if adjMat[i][j] is 0 or >= INF, then i-j is A) not connected or B) blocked (respectively)
				apsp[i][j] = (adjMat[i][j] == 0 || adjMat[i][j] >= INF) ? INF : adjMat[i][j];
				path[i][j] = (adjMat[i][j] == 0 || adjMat[i][j] > INF) ? -1 : j;
			}
		}
		// Floyd Warshall's algorithm
		for (int k = 0; k < adjMat.length; ++k)
			for (int i = 0; i < adjMat.length; ++i)
				for (int j = 0; j < adjMat.length; ++j)
				{
					if (apsp[i][j] > apsp[i][k] + apsp[k][j])
					{
						apsp[i][j] = apsp[i][k] + apsp[k][j];
						// set path values to record how to get from i to j
						path[i][j] = path[i][k];
					}
				}
		apspConstructed = true;
	}
}

class UnionFind
{
	private Vector<Integer> p, rank, setSize;
	private int numSets;

	public UnionFind(int N) {
		p = new Vector<Integer>(N);
		rank = new Vector<Integer>(N);
		setSize = new Vector<Integer>(N);
		numSets = N;
		for (int i = 0; i < N; i++) {
			p.add(i);
			rank.add(0);
			setSize.add(1);
		}
	}

	public int findSet(int i) { 
		if (p.get(i) == i) return i;
		else {
			int ret = findSet(p.get(i)); p.set(i, ret);
			return ret; } }

	public Boolean isSameSet(int i, int j) { return findSet(i) == findSet(j); }

	public void unionSet(int i, int j) { 
		if (!isSameSet(i, j)) { numSets--; 
		int x = findSet(i), y = findSet(j);
		// rank is used to keep the tree short
		if (rank.get(x) > rank.get(y)) { p.set(y, x); setSize.set(x, setSize.get(x) + setSize.get(y)); }
		else													 { p.set(x, y); setSize.set(y, setSize.get(y) + setSize.get(x));
																		 if (rank.get(x) == rank.get(y)) rank.set(y, rank.get(y) + 1); } } }
	public int numDisjointSets() { return numSets; }
	public int sizeOfSet(int i) { return setSize.get(findSet(i)); }
}
