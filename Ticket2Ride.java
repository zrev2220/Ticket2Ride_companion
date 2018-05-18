import java.util.*;
import java.io.*;

public class Ticket2Ride
{
	private static PrintStream out = System.out;
	public static TreeMap<String, Integer> citiesMap = new TreeMap<>();
	public static TreeMap<Integer, String> cityIds = new TreeMap<>();
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
		// create list to hold tickets
		TreeSet<Ticket> tickets = new TreeSet<>();
		TreeMap<Integer, Integer> ticketCities = new TreeMap<>();
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
					// can't add a ticket from city to same city
					if (command[1].equals(command[2]))
					{
						out.println("! Can't add ticket to and from the same city.");
						continue;
					}
					// create new ticket
					Ticket newTicket = new Ticket(command[1], command[2]);
					if (tickets.contains(newTicket))
						out.printf("! The ticket %s to %s has already been added.%n", command[1], command[2]);
					else
					{
						// add new ticket
						tickets.add(newTicket);
						// increment city usages
						ticketCities.put(newTicket.aIdx(), ticketCities.getOrDefault(newTicket.aIdx(), 0) + 1);
						ticketCities.put(newTicket.bIdx(), ticketCities.getOrDefault(newTicket.bIdx(), 0) + 1);
						out.printf("Added ticket %s to %s%n", command[1], command[2]);
						// out.printf("%s is used in %d tickets%n", command[1], ticketCities.get(citiesMap.get(command[1]))); // TODO: Remove for release
						// out.printf("%s is used in %d tickets%n", command[2], ticketCities.get(citiesMap.get(command[2]))); // TODO: Remove for release
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
					// construct ticket to use to check existence in tickets
					Ticket toRemove = new Ticket(command[1], command[2]);
					if (tickets.contains(toRemove))
					{
						// remove ticket and decrement ticketCities entries
						tickets.remove(toRemove);
						int aTimes = ticketCities.get(toRemove.aIdx());
						int bTimes = ticketCities.get(toRemove.bIdx());
						// if this ticket is the last time the city is used, remove from ticketCities
						// else decrement usages
						if (aTimes == 1)
							ticketCities.remove(toRemove.aIdx());
						else
							ticketCities.put(toRemove.aIdx(), aTimes - 1);
						// repeat for b
						if (bTimes == 1)
							ticketCities.remove(toRemove.bIdx());
						else
							ticketCities.put(toRemove.bIdx(), bTimes - 1);
						out.printf("Removed ticket %s to %s%n", command[1], command[2]);
						// out.printf("%s is now used in %d tickets.%n", toRemove.aCity(), aTimes - 1); // TODO: Remove for release
						// out.printf("%s is now used in %d tickets.%n", toRemove.bCity(), bTimes - 1); // TODO: Remove for release
					}
					else
					{
						out.printf("! %s to %s is not in the tickets list.%n", toRemove.aCity(), toRemove.bCity());
					}
				}
			}
			else if ("reset".startsWith(command[0])) // remove ticket
			{
				tickets.clear();
				ticketCities.clear();
				out.println("All tickets removed");
				// TODO: Unblock all routes too
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
					out.println("Not supported yet");
					// check cities
					if (!checkCities(command))
						continue;

					// increment edge weight by 1000000 to prevent its use in path computations
					int a = citiesMap.get(command[1]);
					int b = citiesMap.get(command[2]);
					if (adjMat[a][b] > 1000000)
					{
						out.printf("! %s to %s is already blocked.%n", command[1], command[2]);
					}
					else
					{
						adjMat[a][b] += 1000000;
						adjMat[b][a] += 1000000;
						apspConstructed = false;
						out.printf("Blocked route %s to %s%n", command[1], command[2]);
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
					out.println("Not supported yet");
					// check cities
					if (!checkCities(command))
						continue;

					// increment edge weight by 1000000 to prevent its use in path computations
					int a = citiesMap.get(command[1]);
					int b = citiesMap.get(command[2]);
					if (adjMat[a][b] < 1000000)
					{
						out.printf("! %s to %s is already unblocked.%n", command[1], command[2]);
					}
					else
					{
						adjMat[a][b] -= 1000000;
						adjMat[b][a] -= 1000000;
						apspConstructed = false;
						out.printf("Unblocked route %s to %s%n", command[1], command[2]);
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
						// minimum spanning tree: shortest path to connect all cities
						// actually NP-hard Steiner Tree problem
						// will use 2-approximation technique to solve

						// step 1: build metric closure of cities (in form of edge list)
						ArrayList<IntegerTriple> edgeList = new ArrayList<>();
						for (int city : ticketCities.keySet())
						{
							for (int city2 : ticketCities.keySet())
							{
								if (city != city2)
								{
									IntegerTriple edge = new IntegerTriple(apsp[city][city2], city, city2); // form: weight, u, v; sort by weight
									edgeList.add(edge);
								}
							}
						}
						Collections.sort(edgeList);

						// step 2: find MST of cities on metric closure graph
						UnionFind unionFind = new UnionFind(citiesMap.size());
						ArrayList<IntegerTriple> mst = new ArrayList<>();
						int mst_cost = 0;
						for (int i = 0; i < edgeList.size(); ++i)
						{
							IntegerTriple nextEdge = edgeList.get(i);
							if (!unionFind.isSameSet(nextEdge.second(), nextEdge.third()))
							{
								mst.add(nextEdge);
								mst_cost += nextEdge.first();
								if (nextEdge.first() > 1000000)
								{
									// all routes are blocked to this city
									out.printf("Cannot connect all cities");
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
						ArrayList<IntegerTriple> routes = new ArrayList<>();
						for (IntegerTriple edge : mst)
						{
							// for each edge in mst...
							// ...follow path in <path> and add each edge to <routes>
							int currentV = edge.second();
							int lastV = edge.third();
							while (currentV != lastV)
							{
								int nextV = path[currentV][lastV];
								routes.add(new IntegerTriple(currentV, nextV, 0)); // last element 0 since we don't care about weight anymore
								currentV = nextV;
							}
						}

						// step 4: print routes to claim
						out.printf("Route%s to claim:%n", routes.size() == 1 ? "" : "s");
						for (IntegerTriple e : routes)
							out.printf(" - %s to %s%n", cityIds.get(e.first()), cityIds.get(e.second()));

						if (mst_cost <= 45)
							out.printf("You will need %d trains to claim these routes.%n", mst_cost);
						else
							out.printf("You will need MORE THAN 45 TRAINS (%d) to claim these routes.%n", mst_cost);
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

class IntegerTriple implements Comparable<IntegerTriple>
{
	Integer _first, _second, _third;

	public IntegerTriple(Integer f, Integer s, Integer t) {
		_first = f;
		_second = s;
		_third = t;
	}

	public int compareTo(IntegerTriple o) {
		if (!this.first().equals(((IntegerTriple)o).first()))
			return this.first() - ((IntegerTriple)o).first();
		else if (!this.second().equals(((IntegerTriple)o).second()))
			return this.second() - ((IntegerTriple)o).second();
		else
			return this.third() - ((IntegerTriple)o).third();
	}

	Integer first() { return _first; }
	Integer second() { return _second; }
	Integer third() { return _third; }

	public String toString() { return first() + " " + second() + " " + third(); }
}
