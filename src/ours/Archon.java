package ours;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.sun.org.apache.bcel.internal.generic.GOTO_W;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Archon extends BattlecodeRobot {

	public Archon(RobotController rc) {
		this.rc = rc;
	}

	Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	private final int numberOfScouts = 4;

	private List<MapLocation> discoveredDens = new ArrayList<>();

	@Override
	public void run() {

		Random rand = new Random(rc.getID());
		int scoutsCreated = 0;
		boolean firstCreated = false;

		List<Integer> directionsForScouts = new ArrayList<>(Arrays.asList(  
			ConfigUtils.GO_NORTH_EAST,
			ConfigUtils.GO_NORTH_WEST,
			ConfigUtils.GO_SOUTH_EAST,
			ConfigUtils.GO_SOUTH_WEST
		));
		
		while (true) {

			// Try to handle messages from scouts
			handleMessage();

			// This is a loop to prevent the run() method from returning.
			// Because of the Clock.yield()
			// at the end of it, the loop will iterate once per game round.
			try {
				if (rc.isCoreReady()) {
					Direction dirToMove = Direction.NONE;
					if (!discoveredDens.isEmpty()) {
						int minDist = Integer.MAX_VALUE;
						for (Direction dir : directions) {
							MapLocation newLoc = rc.getLocation().add(dir);
							int dist = newLoc.distanceSquaredTo(discoveredDens.get(0));
							if (minDist > dist) {
								minDist = dist;
								dirToMove = dir;
							}
						}

						// Check the rubble in that direction
						/*
						if (rc.senseRubble(
								rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
							// Too much rubble, so I should clear it
							rc.clearRubble(dirToMove);
							// Check if I can move in this direction
						} else if (rc.canMove(dirToMove)) {
							// Move
							rc.move(dirToMove);
						}*/
					} else {
						// Choose a random unit to build
						// RobotType typeToBuild = robotTypes[fate % 8];
						RobotType typeToBuild = RobotType.SCOUT;
						if (numberOfScouts < ++scoutsCreated) {
							typeToBuild = RobotType.SOLDIER;
						}

						// Check for sufficient parts
						if (rc.hasBuildRequirements(typeToBuild)) {
							// Choose a random direction to try to build in
							Direction dirToBuild = directions[rand.nextInt(8)];
							for (int i = 0; i < 8; i++) {

								// If possible, build in this direction
								if (rc.canBuild(dirToBuild, typeToBuild)) {
									if (firstCreated && !directionsForScouts.isEmpty()) {
										broadcastDirectionToScout(directionsForScouts.remove(0));
									}
									
									rc.build(dirToBuild, typeToBuild);
									firstCreated = true;
									break;
								} else {
									// Rotate the direction to try
									dirToBuild = dirToBuild.rotateLeft();
								}
							}
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
	private void handleMessage() {
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
				int x = message[0];
				int y = message[1];

				if (x == ConfigUtils.MESSAGE_FOR_ARCHON) {
					// TODO
				} else if (x >= 0 && y >= 0) {
					// Messages with both non negative numbers
					// are only for archons (zombie dens broadcast)
					MapLocation loc = new MapLocation(x, y);
					if (!discoveredDens.contains(loc)) {
						discoveredDens.add(loc);
					}
					System.out.println("ZOMBIE DEN AT: " + x + ", " + y);
				}
			}
		}
	}
	
	private void broadcastDirectionToScout(int direction) {
		try {
			rc.broadcastMessageSignal(ConfigUtils.MESSAGE_FOR_SCOUT, direction, 2);
		} catch (GameActionException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}

	}

}
