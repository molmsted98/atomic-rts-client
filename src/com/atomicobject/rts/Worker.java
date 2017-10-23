package com.atomicobject.rts;

import com.atomicobject.rts.Unit;
import com.atomicobject.rts.models.Enemy;
import com.atomicobject.rts.models.Game;
import com.atomicobject.rts.models.Resource;
import com.atomicobject.rts.models.Tile;
import kotlin.Pair;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Random;

import static com.atomicobject.rts.Client.*;

public class Worker {
	@SuppressWarnings("unchecked")
	public static JSONObject collectCommand(Unit worker, Game game, ArrayList<Pair<Integer, Integer>> resourceCoords) {
		Tile workerTile = game.getMap().get(new Pair(worker.x, worker.y));
		JSONObject command = new JSONObject();
		//Seek out a resource if it's not carrying one
		if(worker.resource == 0) {
			Tile nextMove = worker.move();
			System.out.println("Worker not holding anything");
			//Start moving if there's still movement
			if(nextMove != null) {
				System.out.println("Worker next move to " + nextMove.toString());
				command.put(COMMAND, COMMAND_MOVE);
				command.put(DIRECTION, Utils.getDirection(game.getMap().get(new Pair(worker.x, worker.y)), nextMove));
				command.put(UNIT, worker.id);
				return command;
			}
			//No path left. Either collect if there, or find a path
			else {
				System.out.println("no path, collect or find resource");
				//Standing at a resource, collect from it
				if(!Utils.getDirection(workerTile, worker.destination).equals("")) {
					//Get the resource tile
					Tile resourceTile = game.getMap().get(new Pair(worker.destination.getX(), worker.destination.getY()));
					if(resourceTile.getResource() != null) {
						Resource resource = resourceTile.getResource();

						//Remove the worker from the resource
						System.out.println("REMOVING THE WORKER FROM THE RESOURCE");
						resource.getAssignedWorkersIds().remove(worker.id);
					}

					System.out.println("Standing at a resource, time to collect");
					command.put(COMMAND, COMMAND_GATHER);
					command.put(DIRECTION, Utils.getDirection(workerTile, worker.destination));
					command.put(UNIT, worker.id);

					worker.destination = game.getMap().get(new Pair(0, 0));
					return command;
				}
				//Find a resource to go to, start the path
				else {
					System.out.println("Not at resource, go find one");
					Tile closestResource = null;
					double shortestDistance = -1;

					for(int i = 0; i < resourceCoords.size(); i++) {
						System.out.println("Considering resource at " + i);
						Tile resourceTile = game.getMap().get(resourceCoords.get(i));

						double distance = Utils.getDistance(workerTile, resourceTile);
						if(shortestDistance == -1 || shortestDistance > distance) {
							shortestDistance = distance;
							closestResource = resourceTile;
						}
					}

					//There are no resources? Shouldn't happen
					if(closestResource == null) {
						command.put(COMMAND, COMMAND_MOVE);
						command.put(DIRECTION, DIRECTION_SOUTH);
						command.put(UNIT, worker.id);
						return command;
					}
					else {
						worker.destination = closestResource;
					}

					//Find the path to the assigned resource
					worker.moves = Utils.findPath(worker, worker.destination, game.getMap());
					if(worker.moves.size() == 0) {
						return exploreCommand(worker);
					}

					//Start moving the worker towards the resource
					Tile move = worker.move();
					System.out.println("Moving worker from " + worker.x + ", " + worker.y + " to " + move.getX() + ", " + move.getY());
					command.put(COMMAND, COMMAND_MOVE);
					command.put(DIRECTION, Utils.getDirection(workerTile, move));
					command.put(UNIT, worker.id);
					return command;
				}
			}
		}
		//Seek out the base and return the resource
		else {
			System.out.println("Worker has the resource");
			Tile nextMove = worker.move();

			//If it has just started to return to base, path there
			if(nextMove == null) {
				System.out.println("No moves yet, path home");
				Tile homeTile = game.getMap().get(new Pair(0L, 0L));
				worker.destination = homeTile;

				worker.moves = Utils.findPath(worker, homeTile, game.getMap());
				if(worker.moves.size() == 0) {
					return exploreCommand(worker);
				}

				Tile move = worker.move();
				System.out.println("Worker moving to " + move.toString());
				command.put(COMMAND, COMMAND_MOVE);
				command.put(DIRECTION, Utils.getDirection(workerTile, move));
				command.put(UNIT, worker.id);
				return command;
			}
			//It already has a path, execute it
			else {
				System.out.println("Has a path home, move there");

				command.put(COMMAND, COMMAND_MOVE);
				command.put(DIRECTION, Utils.getDirection(workerTile, nextMove));
				command.put(UNIT, worker.id);
				return command;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static JSONObject exploreCommand(Unit worker) {
		JSONObject command = new JSONObject();

		//Determine which area needs more exploring
		command.put(COMMAND, COMMAND_MOVE);
		Random rand = new Random();
		int direction = rand.nextInt(3);
		if(direction == 0) {
			command.put(DIRECTION, DIRECTION_WEST);
		}
		else if(direction == 1) {
			command.put(DIRECTION, DIRECTION_EAST);
		}
		else if(direction == 2) {
			command.put(DIRECTION, DIRECTION_NORTH);
		}
		else if(direction == 3) {
			command.put(DIRECTION, DIRECTION_SOUTH);
		}
		command.put(UNIT, worker.id);
		return command;
	}

	public static JSONObject attackCommand(Unit worker) {
		JSONObject command = new JSONObject();

		return command;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject defendCommand(Unit worker, Game game) {
		JSONObject command = new JSONObject();
		Tile homeTile = game.getMap().get(new Pair(0L, 0L));
		Tile workerTile = game.getMap().get(new Pair(worker.x, worker.y));

		//They aren't already headed toward home
		if(worker.destination != homeTile) {
			worker.destination = homeTile;
			worker.moves = Utils.findPath(worker, homeTile, game.getMap());
			if(worker.moves.size() == 0) {
				return exploreCommand(worker);
			}

			Tile move = worker.move();
			command.put(COMMAND, COMMAND_MOVE);
			command.put(DIRECTION, Utils.getDirection(workerTile, move));
			command.put(UNIT, worker.id);
			return command;
		}
		else {
			Tile move = worker.move();

			//Needs to move toward the base
			if(move != null) {
				command.put(COMMAND, COMMAND_MOVE);
				command.put(DIRECTION, Utils.getDirection(workerTile, move));
				command.put(UNIT, worker.id);
				return command;
			}
			//Stand guard and attack.
			else {
				if(worker.canAttack) {
					ArrayList<Tile> neighbors = Utils.getNeighbors(workerTile, game.getMap());
					Enemy weakestEnemy = null;
					for(int i = 0; i < neighbors.size(); i ++) {
						int lowestHealth = -1;
						if(neighbors.get(i).getEnemies().size() != 0) {
							for(int j = 0; j < neighbors.get(i).getEnemies().size(); j++) {
								if(neighbors.get(i).getEnemies().get(j).getHealth() < lowestHealth) {
									weakestEnemy = neighbors.get(i).getEnemies().get(j);
									lowestHealth = weakestEnemy.getHealth();
								}
							}
						}
					}
					if(weakestEnemy != null) {
						command.put(COMMAND, COMMAND_MELEE);
						command.put(TARGET, weakestEnemy.getId());
						command.put(UNIT, worker.id);
						return command;
					}
					else {
						return null;
					}
				}
				else {
					return null;
				}
			}
		}
	}
}
