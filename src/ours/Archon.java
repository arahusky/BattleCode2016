package ours;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import battlecode.common.*;
import scala.reflect.runtime.ThreadLocalStorage.MyThreadLocalStorage;

public class Archon extends BattlecodeRobot {

	public Archon(RobotController rc) {
		this.rc = rc;
	}

	Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	private static final int NUMBER_OF_SCOUTS = 2; // For each archon

	private List<MapLocation> corners = new ArrayList<>(); // Discovered corners
	private List<MapLocation> dens = new ArrayList<>(); // Discovered dens

	private static final MapLocation ALREADY_IN_CORNER = new MapLocation(-1, -1);
	private static final MapLocation UNDEFINED_LOCATION = new MapLocation(-42, -42);

	/*
	 * Maximum number of rounds (=time) for finding the location where to go.
	 */
	private static final int MAX_ROUNDS = 200;

	@Override
	public void run() {

		MapLocation dest = getDestination();

		// TODO: goToDestination( ... )
		// TODO: barricade()

		swarmToCorner(dest);
	}

	private void swarmToCorner(MapLocation goToLocation) {
		Direction movingDirection = Direction.NORTH_EAST;
		if (rc.getTeam() == Team.B) {
			movingDirection = Direction.SOUTH_WEST;
		}
		System.out.println(movingDirection);

		// @TODO REMOVE WHEN DONE
		if (goToLocation == null) {
			goToLocation = rc.getLocation();
			goToLocation = goToLocation.add(Direction.NONE, 0);
		}

		while(!checkIfCornered(rc)) {
			RobotInfo[] zombieEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, Team.ZOMBIE);
			RobotInfo[] normalEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, rc.getTeam().opponent());
			RobotInfo[] opponentEnemies = Utility.joinRobotInfo(zombieEnemies, normalEnemies);

			try {
				if (opponentEnemies.length > 0 && rc.getType().canAttack()) {
					if (rc.isWeaponReady()) {
						rc.attackLocation(opponentEnemies[0].location);
					}
				} else {
					if (rc.isCoreReady()) {
						if (goToLocation == null) {
							Utility.forwardish(rc, movingDirection);
						}
						else {
							System.out.println("going to location");
							Utility.goToLocation(rc, goToLocation);
						}
					}
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates a few scouts and tries to find a zombie den or corner of the map.
	 * 
	 * @return The position where to go and make barricades. If result is
	 *         (-1,-1), then we are in the corner. If
	 */
	private MapLocation getDestination() {
		Random rand = new Random(rc.getID());
		int scoutsCreated = 0;
		int broadcasted = 0;

		List<Integer> directionsForScouts = new ArrayList<>();
		if (rc.getTeam() == Team.A) {
			directionsForScouts.add(ConfigUtils.GO_NORTH_WEST);
			directionsForScouts.add(ConfigUtils.GO_SOUTH_WEST);
		} else {
			directionsForScouts.add(ConfigUtils.GO_NORTH_EAST);
			directionsForScouts.add(ConfigUtils.GO_NORTH_WEST);
		}

		// Check if we start at the corner
		if (checkIfCornered(rc)) {
			return ALREADY_IN_CORNER;
		}

		while (true) {

			if (rc.getRoundNum() > MAX_ROUNDS) {
				if (!corners.isEmpty()) {
					return corners.get(0);
				}
				if (!dens.isEmpty()) {
					return dens.get(0);
				}
				return UNDEFINED_LOCATION;
			}

			handleMessages();

			try {
				RobotType typeToBuild = RobotType.SCOUT;
				if (NUMBER_OF_SCOUTS <= scoutsCreated && NUMBER_OF_SCOUTS <= broadcasted) {
					if (corners.size() >= NUMBER_OF_SCOUTS) {
						int distance = 0;
						int minDistance = Integer.MAX_VALUE;
						int indexOfMin = 0;
						for (int i = 0; i < corners.size(); i++) {
							distance = rc.getLocation().distanceSquaredTo(corners.get(i));
							if (distance < minDistance) {
								minDistance = distance;
								indexOfMin = i;
							}
						}
						return corners.get(indexOfMin);
					}
					continue;
				}

				// Check for sufficient parts
				if (rc.hasBuildRequirements(typeToBuild) && rc.isCoreReady()) {
					// Choose a random direction to try to build in
					final int spotsForBuild = 8;
					Direction dirToBuild = directions[rand.nextInt(spotsForBuild)];
					for (int i = 0; i < spotsForBuild; i++) {

						// If possible, build in this direction
						if (rc.canBuild(dirToBuild, typeToBuild)) {
							if (scoutsCreated > 0 && !directionsForScouts.isEmpty()) {
								broadcastDirectionToScout(directionsForScouts.remove(0));
								broadcasted++;

								if (broadcasted >= NUMBER_OF_SCOUTS) {
									break;
								}
							}

							rc.build(dirToBuild, typeToBuild);
							scoutsCreated++;
							break;
						} else {
							// Rotate the direction to try
							dirToBuild = dirToBuild.rotateLeft();
						}
					}
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Handles a message received by archon.
	 */
	private void handleMessages() {
		// Get all signals
		Signal[] signals = rc.emptySignalQueue();
		if (signals.length > 0) {
			// Set an indicator string that can be viewed in the
			// client
			rc.setIndicatorString(0, "I received a signal this turn!");
		} else {
			rc.setIndicatorString(0, "I don't any signal buddies");
		}

		for (Signal s : signals) {
			int[] message = s.getMessage();
			if (message == null) {
				System.out.println("NULL MESSAGE");
			} else {
				int identifier = message[0];
				int value = message[1];
				MapLocation loc = ConfigUtils.decodeLocation(value);

				switch (identifier) {
				case ConfigUtils.REPORTING_CORNER_LOCATION:
					if (!corners.contains(loc)) {
						corners.add(loc);
					}
					break;
				case ConfigUtils.REPORTING_DEN_LOCATION:
					if (!dens.contains(loc)) {
						dens.add(loc);
					}
					break;
				}
			}
		}
	}

	private void broadcastDirectionToScout(int direction) {
		try {
			rc.broadcastMessageSignal(ConfigUtils.MESSAGE_FOR_SCOUT, direction, 3);
		} catch (GameActionException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}

	}

	public static boolean checkIfCornered(RobotController rc) {
		List<Direction[]> dirToCheck = new ArrayList<>();
		Team myTeam = rc.getTeam();
		Team enemyTeam = myTeam.opponent();

		dirToCheck.add(new Direction[] { Direction.NORTH, Direction.EAST });
		dirToCheck.add(new Direction[] { Direction.NORTH, Direction.WEST });
		dirToCheck.add(new Direction[] { Direction.SOUTH, Direction.EAST });
		dirToCheck.add(new Direction[] { Direction.SOUTH, Direction.WEST });

		for (Direction[] pair : dirToCheck) {
			MapLocation candidateLocation0 = rc.getLocation().add(pair[0]);
			MapLocation candidateLocation1 = rc.getLocation().add(pair[1]);

			try {
				if (!rc.onTheMap(candidateLocation0) && !rc.onTheMap(candidateLocation1)) {
					return true;
				}
			} catch (GameActionException e) {
				// nothing to do here
			}
		}

		return false;
	}

}
