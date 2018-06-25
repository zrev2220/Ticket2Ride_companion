import java.util.*;
import java.io.*;

public class CLI
{
	private static PrintStream out = System.out;
	private static boolean quitProgram = true;

	public static void main(String[] args)
	{
		Scanner in = new Scanner(System.in);
		out.println("==============================================");
		out.println("  Welcome to Ticket to Ride Java companion!");
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
		try
		{
			Ticket2Ride.getInstance().loadMap(map);
			out.printf("Map initialized for %s%n", map);
		} catch (IOException ex) {
			System.err.println("ERROR - " + ex.getMessage());
			System.exit(1);
		}

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
						Ticket2Ride.getInstance().addTicket(cityA, cityB);
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
						Ticket2Ride.getInstance().removeTicket(cityA, cityB);
						out.printf("Removed ticket %s to %s%n", cityA, cityB);
					} catch (Exception ex)
					{
						out.printf("! %s to %s is not in the tickets list.%n", cityA, cityB);
					}
				}
			}
			else if ("reset".startsWith(command[0])) // reset tickets & blocks
			{
				int nTickets = Ticket2Ride.getInstance().getTicketSet().size();
				int nBlocked = Ticket2Ride.getInstance().getBlockedRoutes().size();
				Ticket2Ride.getInstance().resetModel();
				out.printf("All tickets removed (%d)%n", nTickets);
				out.printf("All routes unblocked (%d)%n", nBlocked);
			}
			else if ("tickets".startsWith(command[0])) // print tickets
			{
				TreeSet<Ticket> tickets = Ticket2Ride.getInstance().getTicketSet();
				out.printf("%d ticket%s%s%n", tickets.size(), tickets.size() == 1 ? "" : "s", tickets.size() > 0 ? ":" : "");
				for (Ticket t : tickets)
					out.printf("- %s to %s%n", t.aCity(), t.bCity());
			}
			else if ("block".startsWith(command[0])) // block route
			{
				// validate argument count
				if (command.length != 3 && command.length != 1)
				{
					out.println("! Incorrect argument count");
					out.println("  Usage: block [city1 city2]");
				}
				else if (command.length == 1)
				{
					// display all blocked routes
					if (Ticket2Ride.getInstance().getBlockedRoutes().isEmpty())
						out.println("No blocked routes");
					else
					{
						out.println("Blocked routes:");
						for (Ticket route : Ticket2Ride.getInstance().getBlockedRoutes())
							out.printf(" - %s to %s%n", route.aCity(), route.bCity());
					}
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
						Ticket2Ride.getInstance().blockRoute(true, cityA, cityB);
						out.printf("Blocked route %s to %s%n", cityA, cityB);
					} catch (Exception ex)
					{
						String errmsg = ex.getMessage();
						if (errmsg.startsWith("1"))
						{
							// route already blocked
							out.printf("! %s to %s is already blocked.%n", cityA, cityB);
						}
						else if (errmsg.startsWith("2"))
						{
							// cities not adjacent, blocking unnecessary
							out.printf("! %s and %s are not connected by a route.%n", cityA, cityB);
						}
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
						Ticket2Ride.getInstance().blockRoute(false, cityA, cityB);
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
				else if (Ticket2Ride.getInstance().getTicketSet().isEmpty())
				{
					out.println("! No tickets have been added");
					out.println("  Add some tickets to compute a path!");
				}
				else
				{
					Ticket2Ride.getInstance().buildAPSP();

					if ("MST".startsWith(command[1].toUpperCase()))
					{
						try
						{
							Pair<Integer, ArrayList<OrderedTriple<Integer, Integer, Integer>>> result = Ticket2Ride.getInstance().steinerTreeApprox();
							int cost = result.first();
							ArrayList<OrderedTriple<Integer, Integer, Integer>> routes = result.second();
							
							out.printf("Route%s to claim:%n", routes.size() == 1 ? "" : "s");
							for (OrderedTriple<Integer, Integer, Integer> e : routes)
								out.printf(" - %s to %s%n", Ticket2Ride.getInstance().getIntToCityMap().get(e.first()),
															Ticket2Ride.getInstance().getIntToCityMap().get(e.second()));

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
				out.println("                   train route (will use more trains)");
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
			TreeMap<String, Integer> cityToInt = Ticket2Ride.getInstance().getCityToIntMap();
			if (!cityToInt.containsKey(command[i]) && (cityToInt.ceilingKey(command[i]) == null || !cityToInt.ceilingKey(command[i]).startsWith(command[i])))
			{
				// can't find exact key or key prefix
				out.printf("! The city \"%s\" does not exist and is not a prefix of an existing city.%n", command[i]);
				return false;
			}
			else if (!cityToInt.containsKey(command[i]))
			{
				// no exact key, but found key prefix
				out.printf("* City \"%s\" auto-completed to ", command[i]);
				command[i] = cityToInt.ceilingKey(command[i]);
				out.printf("\"%s\"%n", command[i]);
			}
			// else if city matches exact key, don't need to modify
		}
		return true;
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
		Ticket2Ride.getInstance().buildAPSP();

		Iterator<Map.Entry<Integer, String>> it1 = Ticket2Ride.getInstance().getIntToCityMap().entrySet().iterator();
		Iterator<Map.Entry<String, Integer>> it2 = Ticket2Ride.getInstance().getCityToIntMap().entrySet().iterator();
		for (; it1.hasNext();)
		{
			Map.Entry<Integer, String> e1 = it1.next();
			Map.Entry<String, Integer> e2 = it2.next();
			out.printf("%2d - %-13s              %-13s - %2d%n", e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue());
		}
		out.printf("%n   ");
		int[][] apsp = Ticket2Ride.getInstance().getApsp();
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

		int[][] path = Ticket2Ride.getInstance().getPath();
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
