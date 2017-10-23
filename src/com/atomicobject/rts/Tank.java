package com.atomicobject.rts;

import com.atomicobject.rts.models.Enemy;
import com.atomicobject.rts.models.Game;
import com.atomicobject.rts.models.Tile;
import kotlin.Pair;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Random;

import static com.atomicobject.rts.Client.*;

public class Tank {
	@SuppressWarnings("unchecked")
	public static JSONObject exploreCommand(Unit tank) {
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
		command.put(UNIT, tank.id);
		return command;
	}

	public static JSONObject attackCommand(Unit tank) {
		JSONObject command = new JSONObject();

		return command;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject defendCommand(Unit tank, Game game) {
		JSONObject command = new JSONObject();
		Tile homeTile = game.getMap().get(new Pair(0L, 0L));
		Tile tankTile = game.getMap().get(new Pair(tank.x, tank.y));

		//They aren't already headed toward home
		if(tank.destination != homeTile) {
			tank.destination = homeTile;
			tank.moves = Utils.findPath(tank, homeTile, game.getMap());
			if(tank.moves.size() == 0) {
				return exploreCommand(tank);
			}

			Tile move = tank.move();
			command.put(COMMAND, COMMAND_MOVE);
			command.put(DIRECTION, Utils.getDirection(tankTile, move));
			command.put(UNIT, tank.id);
			return command;
		}
		else {
			Tile move = tank.move();

			//Needs to move toward the base
			if(move != null) {
				command.put(COMMAND, COMMAND_MOVE);
				command.put(DIRECTION, Utils.getDirection(tankTile, move));
				command.put(UNIT, tank.id);
				return command;
			}
			//Stand guard and attack.
			else {
				if(tank.canAttack) {
					ArrayList<Tile> neighbors = Utils.getNeighbors(tankTile, game.getMap());
					Enemy weakestEnemy = null;
					Tile weakestTile = null;
					for(int i = 0; i < neighbors.size(); i ++) {
						int lowestHealth = -1;
						if(neighbors.get(i).getEnemies().size() != 0) {
							for(int j = 0; j < neighbors.get(i).getEnemies().size(); j++) {
								if(neighbors.get(i).getEnemies().get(j).getHealth() < lowestHealth) {
									weakestEnemy = neighbors.get(i).getEnemies().get(j);
									lowestHealth = weakestEnemy.getHealth();
									weakestTile = neighbors.get(i);
								}
							}
						}
					}
					if(weakestEnemy != null) {
						if(weakestTile != null) {
							command.put(COMMAND, COMMAND_SHOOT);
							command.put(UNIT, tank.id);

							String direction = Utils.getDirection(tankTile, weakestTile);
							if(direction.equals(DIRECTION_NORTH)) {
								command.put(DX, 0);
								command.put(DY, 1);
							}
							else if(direction.equals(DIRECTION_SOUTH)) {
								command.put(DX, 0);
								command.put(DY, -1);
							}
							else if(direction.equals(DIRECTION_EAST)) {
								command.put(DX, -1);
								command.put(DY, 0);
							}
							else if(direction.equals(DIRECTION_WEST)) {
								command.put(DX, 1);
								command.put(DY, 0);
							}
							else {
								return null;
							}

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
				else {
					return null;
				}
			}
		}
	}
}
