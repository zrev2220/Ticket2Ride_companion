import java.util.*;
import java.io.*;

public class Ticket2Ride
{
	private static PrintStream out = System.out;
	public static TreeMap<String, Integer> citiesMap = new TreeMap<>();
	public static TreeMap<Integer, String> cityIds = new TreeMap<>();
	public static TreeSet<Ticket> tickets = new TreeSet<>();
	public static TreeMap<Integer, Integer> ticketCities = new TreeMap<>();
	private static int[][] adjMat;
	private static int[][] apsp;
	private static int[][] path;
	private static boolean apspConstructed = false;
	private static final int INF = 1000000000;

	public static void main(String[] args)
	{
		Scanner in = new Scanner(System.in);
		out.println("==============================================");
		out.println("Welcome to Ticket to Ride Java companion!");
		out.println("                e@@@@@@@@@@@@@@@\n"
				  + "               @@@\"\"\"\"\"\"\"\"\"\"\n"
				  + "              @\" ___ ___________\n"
				  + "             II__[w] | [i] [z] |\n"
				  + "            {======|_|~~~~~~~~~|\n"
				  + "           /oO--000'\"`-OO---OO-'");
		out.println("==============================================");
		// train ASCII art from https://www.ascii-code.com/ascii-art/vehicles/trains.php
		out.println();
		out.println("Please choose a game version:");
		out.println("1 - USA");
		out.println("2 - Europe");
		out.println("0 - Exit");
		int game = readNum(in, "> ");
		while (game < 0 || game > 2)
		{
			out.println("ERROR - Please enter a number in the valid range (0-2)");
			game = readNum(in, "> ");
		}
		String map = "";
		switch (game)
		{
			case 1:
				// USA map
				map = "usa.txt";
				break;
			case 2:
				// Europe map
				map = "eu.txt";
				break;
			case 0:
				// exit early
				return;

		}
		try (BufferedReader mapFile = new BufferedReader(new FileReader(map)))
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
				if (!citiesMap.containsKey(cityA))
				{
					citiesMap.put(cityA, citiesMap.size());
					cityIds.put(citiesMap.size() - 1, cityA);
				}
				if (!citiesMap.containsKey(cityB))
				{
					citiesMap.put(cityB, citiesMap.size());
					cityIds.put(citiesMap.size() - 1, cityB);
				}
				int a = citiesMap.get(cityA), b = citiesMap.get(cityB);
				adjMat[a][b] = w;
				adjMat[b][a] = w; // undirected graph
			}
		} catch (IOException ex) {
			System.err.println("ERROR - " + ex.getMessage());
			System.exit(0);
		}
		out.printf("Map initialized for %s%n", map);
		// command prompt loop
		in.nextLine();
		String[] command = readCmd(in, "> ");
		for (; !(!command[0].equals("") && ("exit".startsWith(command[0]) || "quit".startsWith(command[0]))); command = readCmd(in, "> "))
		{
			// TODO: implement unsupported features
			if (command[0].equals(""))
			{
				out.printf("! Type \"help\" for available commands.%n", command[0]);
			}
			else if ("add".startsWith(command[0])) // add ticket
			{
				// validate argument count
				if (command.length != 3)
				{
					out.println("! Incorrect argument count");
					out.println("  Usage: add city1 city2");
				}
				else
				{
					// check cities
					if (!checkCities(command))
						continue;

					String cityA = command[1];
					String cityB = command[2];
					try
					{
						addTicket(cityA, cityB);
						out.printf("Added ticket %s to %s%n", cityA, cityB);
					} catch (Exception ex)
					{
						String errmsg = ex.getMessage();
						if (errmsg.startsWith("1"))
						{
							// can't add a ticket from city to same city
							out.println("! Can't add ticket to and from the same city.");
						}
						else if (errmsg.startsWith("2"))
						{
							// <tickets> list already contains this ticket
							out.printf("! The ticket %s to %s has already been added.%n", cityA, cityB);
						}
					}
				}
			}
			else if ("rem".startsWith(command[0])) // remove ticket
			{
				// validate argument count
				if (command.length != 3)
				{
					out.println("! Incorrect argument count");
					out.println("  Usage: rem city1 city2");
				}
				else
				{
					// check cities
					if (!checkCities(command))
						continue;

					String cityA = command[1];
					String cityB = command[2];
					try
					{
						removeTicket(cityA, cityB);
						out.printf("Removed ticket %s to %s%n", cityA, cityB);
					} catch (Exception ex)
					{
						out.printf("! %s to %s is not in the tickets list.%n", cityA, cityB);
					}
				}
			}
			else if ("reset".startsWith(command[0])) // reset tickets & blocks
			{
				resetModel();
				out.println("All tickets removed");
			}
			else if ("tickets".startsWith(command[0])) // print tickets
			{
				out.printf("%d ticket%s%s%n", tickets.size(), tickets.size() == 1 ? "" : "s", tickets.size() > 0 ? ":" : "");
				for (Ticket t : tickets)
					out.printf("- %s to %s%n", t.aCity(), t.bCity());
			}
			else if ("block".startsWith(command[0])) // block route
			{
				// validate argument count
				if (command.length != 3)
				{
					out.println("! Incorrect argument count");
					out.println("  Usage: block city1 city2");
				}
				else
				{
					// check cities
					if (!checkCities(command))
						continue;

					String cityA = command[1];
					String cityB = command[2];
					try
					{
						blockRoute(true, cityA, cityB);
						out.printf("Blocked route %s to %s%n", cityA, cityB);
					} catch (Exception ex)
					{
						out.printf("! %s to %s is already blocked.%n", cityA, cityB);
					}
				}
			}
			else if ("unblock".startsWith(command[0])) // unblock route
			{
				// validate argument count
				if (command.length != 3)
				{
					out.println("! Incorrect argument count");
					out.println("  Usage: unblock city1 city2");
				}
				else
				{
					// check cities
					if (!checkCities(command))
						continue;

					String cityA = command[1];
					String cityB = command[2];
					try
					{
						blockRoute(false, cityA, cityB);
						out.printf("Unblocked route %s to %s%n", cityA, cityB);
					} catch (Exception ex)
					{
						out.printf("! %s to %s is already unblocked.%n", cityA, cityB);
					}
				}
			}
			else if ("path".startsWith(command[0])) // compute path using either MST or TSP
			{
				// validate argument count
				if (command.length != 2)
				{
					out.println("! Incorrect argument count");
					out.println("  Usage: path type");
					out.println("  where type is either MST or TSP");
				}
				else if (tickets.isEmpty())
				{
					out.println("! No tickets have been added");
					out.println("  Add some tickets to compute a path!");
				}
				else
				{
					buildAPSP();

					if ("MST".startsWith(command[1].toUpperCase()))
					{
						try
						{
							Pair<Integer, ArrayList<OrderedTriple<Integer, Integer, Integer>>> result = steinerTreeApprox();
							int cost = result.first();
							ArrayList<OrderedTriple<Integer, Integer, Integer>> routes = result.second();
							
							out.printf("Route%s to claim:%n", routes.size() == 1 ? "" : "s");
							for (OrderedTriple<Integer, Integer, Integer> e : routes)
								out.printf(" - %s to %s%n", cityIds.get(e.first()), cityIds.get(e.second()));

							if (cost <= 45)
								out.printf("You will need %d trains to claim these routes.%n", cost);
							else
								out.printf("You will need MORE THAN 45 TRAINS (%d) to claim these routes.%n", cost);
						} catch (Exception ex)
						{
							out.println("Cannot connect all cities - All routes are blocked to one of the cities.");
						}
					}
					else if ("SLOW".startsWith(command[1].toUpperCase()))
					{
						out.println("Not supported yet");
					}
					else if ("TSP".startsWith(command[1].toUpperCase()))
					{
						out.println("Not supported yet");
						// (modified) traveling salesman: shortest path to connect all cities in one continuous route
						// (don't need to connect beginning to end like traditional TSP)
					}
					else
					{
						out.printf("! Path \"%s\" unrecognized.%n" +
								   "  Valid types are \"MST\" and \"TSP\".%n", command[1]);
					}
				}
			}
			else if ("debug".startsWith(command[0])) // print debug info
			{
				printDebug();
			}
			else if ("help".startsWith(command[0])) // display help and usage
			{
				out.println("Ticket to Ride companion program");
				out.println("Type one of the following commands to get started:");
				out.println("  - add <city1> <city2> -- adds a new ticket from city1 to city2 to the tickets list");
				out.println("  - rem <city1> <city2> -- removes the ticket from city1 to city2, if it exists");
				out.println("  - reset -- removes all tickets and unblocks all routes");
				out.println("  - tickets -- displays a list of all added tickets");
				out.println("  - block <city1> <city2> -- blocks the route from city1 to city2, preventing it from being used when a path is computed");
				out.println("  - unblock <city1> <city2> -- unblocks a blocked route from city1 to city2, allowing it to be used when a path is computed");
				out.println("  - path <type> -- computes a path to fulfill all tickets. type must be either MST or TSP.");
				out.println("                 MST computes the routes to claim in order to fulfill all tickets with the least number of trains,");
				out.println("                   but not necessarily maintaining a continuous train route");
				out.println("                 TSP computes the routes to claim in order to fulfill all tickets while maintaining a continuous");
				out.println("                   train route (will use many more trains)");
				out.println("  - debug -- print debug info");
				out.println("  - exit/quit -- exit the program");
				out.println("  - help -- displays this help message");
			}
			else // unrecognized command
			{
				out.printf("! Command \"%s\" unrecognized.%n" +
						   "  Type \"help\" for available commands.%n", command[0]);
			}
		}
		out.println("\n--- Thanks for playing! ---\n");
	}

	public static void addTicket(String cityA, String cityB) throws Exception
	{
		// check if cities are identical
		if (cityA.equals(cityB))
			throw new Exception(String.format("1: Cities are identical (%s)", cityA));

		// check if ticket is already added
		Ticket newTicket = new Ticket(cityA, cityB);
		if (tickets.contains(newTicket))
			throw new Exception(String.format("2: Ticket already in list (%s - %s)", cityA, cityB));

		// add new ticket
		tickets.add(newTicket);
		// increment city usages
		ticketCities.put(newTicket.aIdx(), ticketCities.getOrDefault(newTicket.aIdx(), 0) + 1);
		ticketCities.put(newTicket.bIdx(), ticketCities.getOrDefault(newTicket.bIdx(), 0) + 1);
	}

	public static void removeTicket(String cityA, String cityB) throws Exception
	{
		// ensure ticket is in <tickets> list
		Ticket toRemove = new Ticket(cityA, cityB);
		if (!tickets.contains(toRemove))
			throw new Exception(String.format("Ticket not in list (%s - %s)", cityA, cityB));

		// remove ticket
		tickets.remove(toRemove);

		// decrement <ticketCities> entries
		ticketCities.computeIfPresent(toRemove.aIdx(), (key, value) -> value == 1 ? null : value - 1);
		ticketCities.computeIfPresent(toRemove.bIdx(), (key, value) -> value == 1 ? null : value - 1);
	}

	public static void resetModel()
	{
		// remove all tickets
		tickets.clear();
		ticketCities.clear();
		// TODO unblock all routes
	}

	public static void blockRoute(boolean block, String cityA, String cityB) throws Exception
	{
		// increment/decrement edge weight by 1000000 to prevent/allow its use in path computations
		int a = citiesMap.get(cityA);
		int b = citiesMap.get(cityB);
		int delta = block ? 1 : -1;
		if (!(adjMat[a][b] > 1000000 ^ block))
		{
			throw new Exception(String.format("Route already %sblocked (%s - %s)", !block ? "un" : "", cityA, cityB));
		}
		else
		{
			adjMat[a][b] += 1000000 * delta;
			adjMat[b][a] += 1000000 * delta;
			apspConstructed = false;
		}
	}

	public static Pair<Integer, ArrayList<OrderedTriple<Integer, Integer, Integer>>> steinerTreeApprox() throws Exception
	{
		// minimum spanning tree: shortest path to connect all cities
		// actually NP-hard Steiner Tree problem
		// will use 2-approximation technique to solve

		// step 1: build metric closure of cities (in form of edge list)
		ArrayList<OrderedTriple<Integer, Integer, Integer>> edgeList = new ArrayList<>();
		for (int city : ticketCities.keySet())
		{
			for (int city2 : ticketCities.keySet())
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
		UnionFind unionFind = new UnionFind(citiesMap.size());
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
			if (unionFind.sizeOfSet(nextEdge.second()) == ticketCities.size())
			{
				// all cities connected, can quit early
				break;
			}
		}
		edgeList = null; // don't need this anymore

		// step 3: expand edges in metric closure mst to full paths in original graph
		ArrayList<OrderedTriple<Integer, Integer, Integer>> routes = new ArrayList<>();
		for (OrderedTriple<Integer, Integer, Integer> edge : mst)
		{
			// for each edge in mst...
			// ...follow path in <path> and add each edge to <routes>
			int currentV = edge.second();
			int lastV = edge.third();
			while (currentV != lastV)
			{
				int nextV = path[currentV][lastV];
				routes.add(new OrderedTriple<>(currentV, nextV, 0)); // last element 0 since we don't care about weight anymore
				currentV = nextV;
			}
		}

		// step 4: return cost and routes to claim
		return new Pair<>(mst_cost, routes);
	}

	public static void steinerTreeExact()
	{
		out.println("Not supported yet");
	}

	public static void modifiedTSP()
	{
		out.println("Not supported yet");
	}

	public static boolean checkCities(String[] command)
	{
		// check if both cities exist
		for (int i = 1; i <= 2; ++i)
		{
			// convert city case to lowercase
			command[i] = command[i].toLowerCase();
			if (!citiesMap.containsKey(command[i]) && (citiesMap.ceilingKey(command[i]) == null || !citiesMap.ceilingKey(command[i]).startsWith(command[i])))
			{
				// can't find exact key or key prefix
				out.printf("! The city \"%s\" does not exist and is not a prefix of an existing city.%n", command[i]);
				return false;
			}
			else if (!citiesMap.containsKey(command[i]))
			{
				// no exact key, but found key prefix
				out.printf("* City \"%s\" auto-completed to ", command[i]);
				command[i] = citiesMap.ceilingKey(command[i]);
				out.printf("\"%s\"%n", command[i]);
			}
			// else if city matches exact key, don't need to modify
		}
		return true;
	}

	public static void buildAPSP()
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

	public static int readNum(Scanner in)
	{
		return readNum(in, "");
	}

	public static int readNum(Scanner in, String prompt)
	{
		int n;
		while (true)
		{
			if (!prompt.equals(""))
				out.print(prompt);
			try {
				String nStr = in.next();
				n = Integer.parseInt(nStr);
				break;
			} catch (NumberFormatException ex) {
				out.println("ERROR - Please enter a number");
				continue;
			}
		}
		return n;
	}

	public static String[] readCmd(Scanner in)
	{
		return readCmd(in, "");
	}

	public static String[] readCmd(Scanner in, String prompt)
	{
		if (!prompt.equals(""))
			out.print(prompt);
		String line = in.nextLine();
		return line.split(" ");
	}

	public static void printDebug()
	{
		// build apsp before we can print it
		buildAPSP();

		Iterator<Map.Entry<Integer, String>> it1 = cityIds.entrySet().iterator();
		Iterator<Map.Entry<String, Integer>> it2 = citiesMap.entrySet().iterator();
		for (; it1.hasNext();)
		{
			Map.Entry<Integer, String> e1 = it1.next();
			Map.Entry<String, Integer> e2 = it2.next();
			out.printf("%2d - %-13s              %-13s - %2d%n", e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue());
		}
		out.printf("%n   ");
		for (int i = 0; i < apsp.length; ++i)
			out.printf(" %2d", i);
		out.printf("%n   ");
		for (int i = 0; i < apsp.length; ++i)
			out.print("___");
		out.println();
		for (int i = 0; i < apsp.length; ++i)
		{
			out.printf("%2d|", i);
			for (int j = 0; j < apsp.length; ++j)
				out.printf(" %2d", apsp[i][j]);
			out.println();
		}

		out.println("\n------ Path ------");
		out.printf("%n   ");
		for (int i = 0; i < path.length; ++i)
			out.printf(" %2d", i);
		out.printf("%n   ");
		for (int i = 0; i < path.length; ++i)
			out.print("___");
		out.println();
		for (int i = 0; i < path.length; ++i)
		{
			out.printf("%2d|", i);
			for (int j = 0; j < path.length; ++j)
				out.printf(" %2d", path[i][j]);
			out.println();
		}
	}
}

class Ticket implements Comparable
{
	private int aIdx;
	private int bIdx;
	private String aCity;
	private String bCity;

	public Ticket(String a, String b)
	{
		if (Ticket2Ride.citiesMap.get(a) > Ticket2Ride.citiesMap.get(b))
		{
			// swap a and b so that a < b
			String tmp = a;
			a = b;
			b = tmp;
		}
		this.aCity = a;
		this.bCity = b;
		this.aIdx = Ticket2Ride.citiesMap.get(a);
		this.bIdx = Ticket2Ride.citiesMap.get(b);
	}

	public Ticket(int a, int b)
	{
		if (a > b)
		{
			// swap a and b so that a < b
			int tmp = a;
			a = b;
			b = tmp;
		}
		this.aCity = Ticket2Ride.cityIds.get(a);
		this.bCity = Ticket2Ride.cityIds.get(b);
		this.aIdx = a;
		this.bIdx = b;
	}

	public int aIdx() { return this.aIdx; }
	public int bIdx() { return this.bIdx; }
	public String aCity() { return this.aCity; }
	public String bCity() { return this.bCity; }

	public int compareTo(Object o)
	{
		Ticket other = (Ticket) o;
		return (this.aIdx == other.aIdx) ? this.bIdx - other.bIdx : this.aIdx - other.aIdx;
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
