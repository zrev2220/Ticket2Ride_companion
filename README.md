# Ticket to Ride companion

## In Short
Companion program for Ticket to Ride board games, computing paths for connecting cities using several choices of algorithms.

## Longer Description
This program is meant to assist with your route planning in the board game series Ticket to Ride by Days of Wonder. You can add a list of tickets to fulfill, then compute the best (or approximately best) set of routes to connect all the required cities with the fewest number of trains.

In a real game, it's very rare other players stay completely out of your way, so this program also allows you to "block" routes that other players have taken, forcing the algorithms to recompute the optimal paths around these routes.

This program works for any Ticket to Ride map (or any weighted, undirected graph for that matter). The map is loaded from a text file which stores the map data in the form of an edge list. Currently, only the USA map file has been written, but files for other maps such as Europe and Germany can be easily added and are a future plan.
