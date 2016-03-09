package ours;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class Guard extends BattlecodeRobot {

	public Guard(RobotController rc) {
		this.rc = rc;
	}

	@Override
	public void run() {
		while (true) {
			try {
				boolean attacking = false;
				RobotInfo[] zombieArray = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, Team.ZOMBIE);
				RobotInfo[] allEnemyArray = rc.senseHostileRobots(rc.getLocation(), rc.getType().attackRadiusSquared);
				//prefer to kill zombies first
				if (zombieArray.length > 0) {
					if (rc.isWeaponReady()) {
						// look for adjacent zombies to attack
						for (RobotInfo zombie : zombieArray) {
							if (rc.canAttackLocation(zombie.location)) {
								rc.attackLocation(zombie.location);
								attacking = true;
								break;
							}
						}
					}

					if (!attacking) {
						// could not find any zombies adjacent to attack
						// try to move toward them
						if (rc.isCoreReady()) {
							MapLocation goal = zombieArray[0].location;
							Direction toEnemy = rc.getLocation().directionTo(goal);
							if (rc.canMove(toEnemy)) {
								rc.setIndicatorString(0, "moving to enemy");
								rc.move(toEnemy);
							} else {
								MapLocation ahead = rc.getLocation().add(toEnemy);
								if (rc.senseRubble(ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
									rc.clearRubble(toEnemy);
								}
							}
						}
					}
				}

				if (!attacking && allEnemyArray.length > 0) {
					if (rc.isWeaponReady()) {
						// look for adjacent enemies to attack
						for (RobotInfo enemy : allEnemyArray) {
							if (rc.canAttackLocation(enemy.location)) {
								rc.attackLocation(enemy.location);
								break;
							}
						}
					}
				}
			} catch (GameActionException e) {
				// nothing to do here
			}
		}
	}

}
