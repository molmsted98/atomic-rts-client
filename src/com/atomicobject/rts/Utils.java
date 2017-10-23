package com.atomicobject.rts;

import com.atomicobject.rts.models.Game;
import com.atomicobject.rts.models.Tile;
import com.atomicobject.rts.models.WeightedTile;
import kotlin.Pair;

import java.util.*;

public class Utils {
	private static Comparator<WeightedTile> comparator = new Comparator<WeightedTile>() {
		@Override
		public int compare(WeightedTile t1, WeightedTile t2) {
			if(t1.getWeight() < t2.getWeight()) {
				return -1;
			}
			else if(t1.getWeight() > t2.getWeight()) {
				return 1;
			}
			return 0;
		}
	};

	public static String getDirection(Tile start, Tile end) {
		if(end == null) {
			return "";
		}
		if(start.getX() == end.getX() && start.getY() > end.getY()) {
			return "N";
		}
		else if(start.getX() == end.getX() && start.getY() < end.getY()) {
			return "S";
		}
		else if(start.getX() > end.getX() && start.getY() == end.getY()) {
			return "W";
		}
		else if(start.getX() < end.getX() && start.getY() == end.getY()) {
			return "E";
		}
		else {
			return "";
		}
	}

	public static double getDistance(Tile start, Tile end) {
		return(Math.sqrt((end.getX() - start.getX())^2 + (end.getY() - start.getY())^2));
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<Tile> findPath(Unit unit, Tile destination, HashMap<Pair<Integer, Integer>, Tile> map) {
		Tile start = map.get(new Pair(unit.x, unit.y));
		WeightedTile startWeighted = new WeightedTile(start, 0);

		PriorityQueue<WeightedTile> frontier = new PriorityQueue<>(map.size(), comparator);
		frontier.add(startWeighted);

		HashMap<Tile, Tile> cameFrom = new HashMap();
		cameFrom.put(start, null);

		HashMap<Tile, Long> costSoFar = new HashMap<>();
		costSoFar.put(start, 0L);

		System.out.println("Finding a path");

		//Explore the map
		boolean flag = false;
		while(!frontier.isEmpty()) {
			WeightedTile current = frontier.poll();
			System.out.println("Considering current " + current.toString());

			//Stop searching once the destinations was found
			if(destination == null) {
				System.out.println("PASDF");
			}
			if(current.getTile() == destination || flag) {
				System.out.println("Destination found");
				break;
			}

			ArrayList<Tile> neighbors = getNeighbors(current.getTile(), map);
			for(int i = 0; i < neighbors.size(); i++) {
				Tile neighbor = neighbors.get(i);
				System.out.println("Considering Neighbor " + neighbor.toString());
				if(neighbor == destination) {
					System.out.println("Destination found as neighbor");
					cameFrom.put(neighbor, current.getTile());
					flag = true;
					break;
				}
				else if(!neighbor.getBlocked()) {
					System.out.println("Determine cost of going here");
					long newCost = costSoFar.get(current.getTile()) + tileCost(neighbor);

					System.out.println("Got new cost");
					WeightedTile neighborWeighted = new WeightedTile(neighbor, heuristic(destination, neighbor) + newCost);

					System.out.println("Got neighbor weighted.");
					//Tile asdf = cameFrom.get(neighborWeighted.getTile());
					System.out.println("Got where it came from");
					//Long costsofar = costSoFar.get(neighbor);
					System.out.println("Got costsofar");
					if(cameFrom.get(neighborWeighted.getTile()) == null || newCost < costSoFar.get(neighbor)) {
						System.out.println("Better cost point");
						frontier.add(neighborWeighted);
						System.out.println("Added to frontier");
						costSoFar.put(neighbor, newCost);
						System.out.println("updated cost");
						cameFrom.put(neighbor, current.getTile());
						System.out.println("Updated route");
					}
					System.out.println("Done considering a valid neighbor");
				}
			}
		}

		//Build the path to the goal
		System.out.println("Building path to goal");
		Tile current = destination;
		ArrayList<Tile> path = new ArrayList<>();
		path.add(current);

		while(current != start) {
			//current = cameFrom.get(current);
			Tile newTile = cameFrom.get(current);

			if(newTile != null) {
				System.out.println("Adding move " + newTile.toString());
				//path.add(current);
				path.add(newTile);
			}
			//The destination was never found in the search
			else {
				return new ArrayList<>();
			}
			current = newTile;
		}
		Collections.reverse(path);
		return path;
	}

	private static long heuristic(Tile start, Tile end) {
		//Manhattan distance on a square grid
		return Math.abs(start.getX() - end.getX()) + Math.abs(start.getY() - end.getY());
	}

	private static long tileCost(Tile tile) {
		return 1;
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<Tile> getNeighbors(Tile tile, HashMap<Pair<Integer, Integer>, Tile> map) {
		ArrayList<Tile> neighbors = new ArrayList<>();

		Tile above = map.get(new Pair(tile.getX(), tile.getY() + 1));
		Tile below = map.get(new Pair(tile.getX(), tile.getY() - 1));
		Tile left = map.get(new Pair(tile.getX() - 1, tile.getY()));
		Tile right = map.get(new Pair(tile.getX() + 1, tile.getY()));

		if(above != null){
			if(!above.getBlocked() || above.getResource() != null) {
				neighbors.add(above);
			}
		}
		if(below != null) {
			if(!below.getBlocked() || below.getResource() != null) {
				neighbors.add(below);
			}
		}
		if(left != null) {
			if(!left.getBlocked() || left.getResource() != null) {
				neighbors.add(left);
			}
		}
		if(right != null) {
			if(!right.getBlocked() || right.getResource() != null) {
				neighbors.add(right);
			}
		}

		return neighbors;
	}
}

