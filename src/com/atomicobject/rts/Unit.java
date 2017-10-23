package com.atomicobject.rts;
import com.atomicobject.rts.models.Tile;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Unit {

	Long resource;
	String attackType;
	Long health;
	Long range;
	Long attackDamage;
	String type;
	Long speed;
	Long attackCooldown;
	Boolean canAttack;
	Long playerId;
	Long x;
	Long y;
	Long id;
	String status;
	Long attackCooldownDuration;
	Tile destination;
	ArrayList<Tile> moves;
	String mode;

	public Unit(JSONObject json) {
		resource = (Long) json.get("resource");
		attackType = (String) json.get("attack_type");
		health = (Long) json.get("health");
		range = (Long) json.get("range");
		attackDamage = (Long) json.get("attack_damage");
		type = (String) json.get("type");
		speed = (Long) json.get("speed");
		attackCooldown = (Long) json.get("attack_cooldown");
		canAttack = (Boolean) json.get("can_attack");
		playerId = (Long) json.get("player_id");
		x = (Long) json.get("x");
		y = (Long) json.get("y");
		id = (Long) json.get("id");
		status = (String) json.get("status");
		attackCooldownDuration = (Long) json.get("attack_cooldown_duration");
		mode = "defend";
		moves = new ArrayList<>();
	}

	public Tile move() {
		System.out.println("Attempting to move unit " + id);
		if(!moves.isEmpty()) {
			System.out.println("There's moves to be made");
			Tile nextMove = moves.get(0);
			System.out.println("The next move is " + nextMove.toString());
			moves.remove(nextMove);
			return nextMove;
		}
		else {
			return null;
		}
	}

	public void update(JSONObject json) {
		resource = (Long) json.get("resource");
		attackType = (String) json.get("attack_type");
		health = (Long) json.get("health");
		range = (Long) json.get("range");
		attackDamage = (Long) json.get("attack_damage");
		type = (String) json.get("type");
		speed = (Long) json.get("speed");
		attackCooldown = (Long) json.get("attack_cooldown");
		canAttack = (Boolean) json.get("can_attack");
		playerId = (Long) json.get("player_id");
		x = (Long) json.get("x");
		y = (Long) json.get("y");
		id = (Long) json.get("id");
		status = (String) json.get("status");
		attackCooldownDuration = (Long) json.get("attack_cooldown_duration");
	}
}
