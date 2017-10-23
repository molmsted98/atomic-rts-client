package com.atomicobject.rts;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import com.atomicobject.rts.models.*;
import kotlin.Pair;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Client {
	
	BufferedReader input;
	OutputStreamWriter out;
	LinkedBlockingQueue<Map<String, Object>> updates;

	//Game knowledge of units
	Game game;
	Map<Long, Unit> scouts;
	Map<Long, Unit> workers;
	Map<Long, Unit> tanks;
	Map<Long, Enemy> enemies;
	ArrayList<Pair<Integer, Integer>> resourceCoords;
	Unit base;

	//Constants that are used for commands
	final static String DIRECTION = "dir";
	final static String COMMAND = "command";
	final static String COMMANDS = "commands";
	final static String UNIT = "unit";
	final static String TYPE = "type";
	final static String TARGET = "target";
	final static String DIRECTION_NORTH = "N";
	final static String DIRECTION_EAST = "E";
	final static String DIRECTION_SOUTH = "S";
	final static String DIRECTION_WEST = "W";
	final static String COMMAND_MOVE = "MOVE";
	final static String COMMAND_GATHER = "GATHER";
	final static String COMMAND_CREATE = "CREATE";
	final static String COMMAND_SHOOT = "SHOOT";
	final static String COMMAND_MELEE = "MELEE";
	final static String COMMAND_IDENTIFY = "IDENTIFY";
	final static String UNIT_WORKER = "worker";
	final static String UNIT_SCOUT = "scout";
	final static String UNIT_TANK = "tank";
	final static String UNIT_BASE = "base";
	final static String STATUS_IDLE = "idle";
	final static String STATUS_MOVING = "moving";
	final static String STATUS_BUILDING = "building";
	final static String STATUS_DEAD = "dead";
	final static String DX = "dx";
	final static String DY = "dy";

	//Strings that refer to the mode of the unit
	final static String MODE_ATTACK = "attack";
	final static String MODE_DEFEND = "defend";
	final static String MODE_EXPLORE = "explore";
	final static String MODE_COLLECT = "collect";

	public Client(Socket socket) {
		updates = new LinkedBlockingQueue<>();
		scouts = new HashMap<>();
		workers = new HashMap<>();
		tanks = new HashMap<>();
		enemies = new HashMap<>();
		resourceCoords = new ArrayList<>();
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new OutputStreamWriter(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		System.out.println("Starting client threads ...");
		new Thread(() -> readUpdatesFromServer()).start();
		new Thread(() -> runClientLoop()).start();
	}
	
	public void readUpdatesFromServer() {
		String nextLine;
		try {
			while ((nextLine = input.readLine()) != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> update = (Map<String, Object>) JSONValue.parse(nextLine.trim());
				updates.add(update);
			}
		} catch (IOException e) {
			// exit thread
		}		
	}

	public void runClientLoop() {
		System.out.println("Starting client update/command processing ...");
		try {
			while (true) {
				processUpdateFromServer();
				respondWithCommands();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		closeStreams();
	}

	@SuppressWarnings("unchecked")
	private void processUpdateFromServer() throws InterruptedException {
		Map<String, Object> update = updates.take();
		if (update != null) {
			System.out.println("Processing update: " + update);
			Collection<JSONObject> unitUpdates = (Collection<JSONObject>) update.get("unit_updates");
			Collection<JSONObject> tileUpdates = (Collection<JSONObject>) update.get("tile_updates");

			//Update the local model of the game
			int time = (int)(long) update.get("time");
			int turn = (int)(long) update.get("turn");
			if(turn != 0) {
				game.setTime(time);
				game.setTurn(turn);
			}

			//Retrieve game info on the first turn and initialize the game
			if(turn == 0) {
				JSONObject gameInfo = (JSONObject) update.get("game_info");
				game = Game.newInstance(gameInfo);
			}

			if(unitUpdates != null) {
				addUnitUpdate(unitUpdates);
			}
			if(tileUpdates != null) {
				addTileUpdate(tileUpdates);
			}
		}
	}

	//Iterate through all changes in the tiles and update local model accordingly
	@SuppressWarnings("unchecked")
	private void addTileUpdate(Collection<JSONObject> tileUpdates) {
		tileUpdates.forEach((tileUpdate) -> {
			//Get tile properties
			long x = (long) tileUpdate.get("x");
			long y = (long) tileUpdate.get("y");
			Pair tileKey = new Pair(x, y);
			boolean visible = (boolean) tileUpdate.get("visible");
			@Nullable JSONObject tileResource = (JSONObject) tileUpdate.get("resources");
			Collection<JSONObject> units = (Collection<JSONObject>) tileUpdate.get("units");

			//Create all enemy units that are on the tile
			ArrayList<Enemy> enemyUnits = new ArrayList<>();
			if(units != null) {
				units.forEach((enemyUnit) -> {
					Enemy enemy = Enemy.newInstance(enemyUnit);

					enemies.put(enemy.getId(), enemy);
					enemyUnits.add(enemy);
				});
			}

			//Create the resource if the update has one
			Resource resource = null;
			if(tileResource != null) {
				System.out.println("There was a resource on the tile");
				long total = (long) tileResource.get("total");
				long id = (long) tileResource.get("id");

				String type = (String) tileResource.get("type");
				long value = (long) tileResource.get("value");
				resource = new Resource(id, type, total, value, new ArrayList<>(), x, y);
				if(!resourceCoords.contains(tileKey)) {
					resourceCoords.add(tileKey);
				}
			}

			//If this tile already exists in the map
			if(game.getMap().get(tileKey) != null) {
				//If there were resources on this tile before
				if(game.getMap().get(tileKey).getResource() != null) {
					//If there are no longer resources here, don't consider it any longer
					if(resource == null && visible) {
						resourceCoords.remove(tileKey);
					}
				}
				game.getMap().get(tileKey).update(tileUpdate, resource, enemyUnits);
			}
			//Tile isn't in map, make it now
			else {
				game.getMap().put(tileKey, Tile.newInstance(tileUpdate, resource, enemyUnits));
			}
		});
	}

	//Iterate through all changes in the units and update local model accordingly
	private void addUnitUpdate(Collection<JSONObject> unitUpdates) {
		unitUpdates.forEach((unitUpdate) -> {
			Long id = (Long) unitUpdate.get("id");
			String type = (String) unitUpdate.get("type");

			Unit updatedUnit = new Unit(unitUpdate);

			if (type.equals(UNIT_WORKER)) {
				if(updatedUnit.status.equals(STATUS_DEAD)) {
					workers.remove(id);
				}
				else {
					if(workers.get(id) != null) {
						workers.get(id).update(unitUpdate);
					}
					else {
						workers.put(id, new Unit(unitUpdate));
					}
				}
			}
			else if (type.equals(UNIT_SCOUT)) {
				if(updatedUnit.status.equals(STATUS_DEAD)) {
					scouts.remove(id);
				}
				else {
					if(scouts.get(id) != null) {
						scouts.get(id).update(unitUpdate);
					}
					else {
						scouts.put(id, new Unit(unitUpdate));
					}
				}
			}
			else if (type.equals(UNIT_TANK)) {
				if(updatedUnit.status.equals(STATUS_DEAD)) {
					tanks.remove(id);
				}
				else {
					if(tanks.get(id) != null) {
						tanks.get(id).update(unitUpdate);
					}
					else {
						tanks.put(id, new Unit(unitUpdate));
					}
				}
			}
			else if(type.equals(UNIT_BASE)) {
				if(base != null) {
					base.update(unitUpdate);
				}
				else {
					base = new Unit(unitUpdate);
				}
			}
 		});
	}

	private void respondWithCommands() throws IOException {
		if (workers.size() == 0 && scouts.size() == 0 && tanks.size() == 0) return;
		
		JSONArray commands = buildCommandList();		
		sendCommandListToServer(commands);
	}

	@SuppressWarnings("unchecked")
	private JSONArray buildCommandList() {
		JSONArray commands = new JSONArray();

		//Retrieve ids of all available units
		Long[] scoutIds = scouts.keySet().toArray(new Long[scouts.size()]);
		Long[] workerIds = workers.keySet().toArray(new Long[workers.size()]);
		Long[] tankIds = tanks.keySet().toArray(new Long[tanks.size()]);

		//Determine command for all workers
		for(int i = 0; i < workerIds.length; i++) {
			Unit worker = workers.get(workerIds[i]);
			JSONObject workerCommand = workerCommand(worker);
			if(workerCommand != null) {
				commands.add(workerCommand);
			}
		}

		//Determine command for all scouts
		for(int i = 0; i < scoutIds.length; i++) {
			Unit scout = scouts.get(scoutIds[i]);
			JSONObject scoutCommand = scoutCommand(scout);
			if(scoutCommand != null) {
				commands.add(scoutCommand);
			}
		}

		//Determine command for all tanks
		for(int i = 0; i < tankIds.length; i++) {
			Unit tank = tanks.get(tankIds[i]);
			JSONObject tankCommand = tankCommand(tank);
			if(tankCommand != null) {
				commands.add(tankCommand);
			}
		}

		//Make sure that worker size is high enough
		if(workers.size() < 5) {
			if(base.resource > 200) {
				JSONObject command = new JSONObject();
				command.put(COMMAND, COMMAND_CREATE);
				command.put(TYPE, UNIT_WORKER);
				commands.add(command);
			}
		}
		//Build one defense tank
		else {
			if(tanks.size() < 1 && base.resource > 500) {
				JSONObject command = new JSONObject();
				command.put(COMMAND, COMMAND_CREATE);
				command.put(TYPE, UNIT_TANK);
				commands.add(command);
			}
			if(scouts.size() < 1 && base.resource > 400) {
				JSONObject command = new JSONObject();
				command.put(COMMAND, COMMAND_CREATE);
				command.put(TYPE, UNIT_SCOUT);
				commands.add(command);
			}
		}

		return commands;
	}

	@SuppressWarnings("unchecked")
	private JSONObject workerCommand(Unit worker) {
		System.out.println("TIME FOR " + worker.id);
		//Determine the mode that the worker should be in
		System.out.println("Available resource count is " + resourceCoords.size());
		if(resourceCoords.size() == 0) {
			System.out.println("Worker in EXPLORE mode");
			if(!worker.mode.equals(MODE_EXPLORE)) {
				worker.moves = new ArrayList<>();
				worker.mode = MODE_EXPLORE;
			}
		}
		else {
			System.out.println("Worker in COLLECT mode");
			if(!worker.mode.equals(MODE_COLLECT)) {
				worker.moves = new ArrayList<>();
				worker.mode = MODE_COLLECT;
			}
		}

		//Only move the worker if it's idle
		if(worker.status.equals(STATUS_IDLE)) {
			System.out.println("IDLE");
			if(worker.mode.equals(MODE_COLLECT)) {
				return Worker.collectCommand(worker, game, resourceCoords);
			}
			else if(worker.mode.equals(MODE_EXPLORE)) {
				return Worker.exploreCommand(worker);
			}
			else if(worker.mode.equals(MODE_ATTACK)) {
				return Worker.attackCommand(worker);
			}
			else if(worker.mode.equals(MODE_DEFEND)) {
				return Worker.defendCommand(worker, game);
			}
		}

		return null;
	}

	private JSONObject scoutCommand(Unit scout) {
		scout.mode = MODE_EXPLORE;

		if(scout.status.equals(STATUS_IDLE)) {
			if(scout.mode.equals(MODE_EXPLORE)) {
				return Scout.exploreCommand(scout);
			}
			else if(scout.mode.equals(MODE_DEFEND)) {
				return Scout.defendCommand(scout);
			}
			else if(scout.mode.equals(MODE_ATTACK)) {
				return Scout.attackCommand(scout);
			}
		}

		return null;
	}

	private JSONObject tankCommand(Unit tank) {
		tank.mode = MODE_DEFEND;

		if(tank.status.equals(STATUS_IDLE)) {
			if(tank.mode.equals(MODE_EXPLORE)) {
				return Tank.exploreCommand(tank);
			}
			else if(tank.mode.equals(MODE_DEFEND)) {
				return Tank.defendCommand(tank, game);
			}
			else if(tank.mode.equals(MODE_ATTACK)) {
				return Tank.attackCommand(tank);
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void sendCommandListToServer(JSONArray commands) throws IOException {
		JSONObject container = new JSONObject();
		container.put(COMMANDS, commands);
		System.out.println("Sending commands: " + container.toJSONString());
		out.write(container.toJSONString());
		out.write("\n");
		out.flush();
	}

	private void closeStreams() {
		closeQuietly(input);
		closeQuietly(out);
	}

	private void closeQuietly(Closeable stream) {
		try {
			stream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
