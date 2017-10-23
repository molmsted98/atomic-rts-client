package com.atomicobject.rts;

import org.json.simple.JSONObject;

import java.util.Random;

import static com.atomicobject.rts.Client.*;

public class Scout {
	@SuppressWarnings("unchecked")
	public static JSONObject exploreCommand(Unit scout) {
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
		command.put(UNIT, scout.id);
		return command;
	}

	public static JSONObject attackCommand(Unit scout) {
		JSONObject command = new JSONObject();

		return command;
	}

	public static JSONObject defendCommand(Unit scout) {
		JSONObject command = new JSONObject();

		return command;
	}
}
