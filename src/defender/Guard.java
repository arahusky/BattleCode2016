package defender;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class Guard extends BattlecodeRobot {

	Team myTeam;
	Team opponentTeam;
	Random rand;

	public Guard(RobotController rc) {
		this.rc = rc;
		myTeam = rc.getTeam();
		opponentTeam = rc.getTeam().opponent();
		rand = new Random(rc.getID());
	}

	@Override
	public void run() {
		while (true) {
			try {
				RobotInfo[] nearbyZombies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, Team.ZOMBIE);
				RobotInfo[] nearbyOpponent = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, opponentTeam);

				// if there is any enemy nearby
				if (nearbyZombies.length > 0 || nearbyOpponent.length > 0) {

					RobotInfo attackableZombie = null;
					RobotInfo attackableEnemy = null;

					if (rc.isWeaponReady()) {
						attackableZombie = canAttackEnemy(nearbyZombies);
						attackableEnemy = canAttackEnemy(nearbyOpponent);
					}

					if (attackableZombie != null) {
						rc.attackLocation(attackableZombie.location);
					} else if (attackableEnemy != null) {
						rc.attackLocation(attackableEnemy.location);
					} else {
						// could not find any enemy adjacent to attack
						// try to move toward them
						MapLocation goal = null;
						if (nearbyZombies.length > 0) {
							goal = nearbyZombies[0].location;
						} else if (nearbyOpponent.length > 0) {
							goal = nearbyOpponent[0].location;
						}

						Direction toEnemy = rc.getLocation().directionTo(goal);
						if (rc.canMove(toEnemy)) {
							rc.move(toEnemy);
						} else {
							MapLocation ahead = rc.getLocation().add(toEnemy);
							if (rc.senseRubble(ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
								rc.clearRubble(toEnemy);
							}
						}
					}
				} else {
					// no enemy seen, move in a random direction, so that we do
					// not block archon to spawn new units
					moveInRandomDirection();
				}
			} catch (GameActionException e) {
				// nothing to do here
			}
			Clock.yield();
		}
	}

	/**
	 * Checks, whether any of the specified enemies can be attacked. The first
	 * one that can be attacked is returned. If no such exists, null is
	 * returned.
	 * 
	 */
	private RobotInfo canAttackEnemy(RobotInfo[] enemies) throws GameActionException {
		for (RobotInfo enemy : enemies) {
			if (rc.canAttackLocation(enemy.location)) {
				rc.attackLocation(enemy.location);
				return enemy;
			}
		}

		return null;
	}

	/**
	 * Tries to move the robot in a random direction. If there is too much
	 * rubble in the direction, the robot will clear it.
	 */
	private void moveInRandomDirection() throws GameActionException {
		int fate = rand.nextInt(1000);
		Direction[] directions = ConfigUtils.POSSIBLE_DIRECTIONS;

		if (rc.isCoreReady()) {
			if (fate < 600) {
				// Choose a random direction to try to move in
				Direction dirToMove = directions[fate % 8];
				// Check the rubble in that direction
				if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					// Too much rubble, so I should clear it
					rc.clearRubble(dirToMove);
					// Check if I can move in this direction
				} else if (rc.canMove(dirToMove)) {
					// Move
					rc.move(dirToMove);
				}
			}
		}
	}
}
