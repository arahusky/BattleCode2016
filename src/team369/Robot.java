package team369;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.ZombieSpawnSchedule;


public abstract class Robot {
	Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	Random rand;
	int myAttackRange;
	Team myTeam, enemyTeam;
	RobotController rc;
	SignalAdapter sa;
	ZombieSpawnSchedule zss;
	MapLocation[] initialAllyLocs, initialEnemyLocs;
	MapLocation enemyHomeAreaLocation, allyHomeAreaLocation;
	
	protected Robot(RobotController rc){
        rand = new Random(rc.getID());
        myAttackRange = rc.getType().attackRadiusSquared;
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        sa = new SignalAdapter(rc);
        this.rc = rc;
        rc.emptySignalQueue(); //receives signals during construction, so get rid of those
		zss = rc.getZombieSpawnSchedule();
		initialAllyLocs = rc.getInitialArchonLocations(myTeam);
		initialEnemyLocs = rc.getInitialArchonLocations(enemyTeam);
		
		int enemyLocIndex = 0, enemyLocXSum = 0,enemyLocYSum = 0;
		for(; enemyLocIndex < initialEnemyLocs.length; enemyLocIndex++){
			enemyLocXSum += initialEnemyLocs[enemyLocIndex].x;
			enemyLocYSum += initialEnemyLocs[enemyLocIndex].y;
		}
		enemyHomeAreaLocation = new MapLocation(enemyLocXSum/enemyLocIndex, enemyLocYSum/enemyLocIndex);
		
		int allyLocIndex = 0, allyLocXSum = 0,allyLocYSum = 0;
		for(; allyLocIndex < initialAllyLocs.length; allyLocIndex++){
			allyLocXSum += initialAllyLocs[allyLocIndex].x;
			allyLocYSum += initialAllyLocs[allyLocIndex].y;
		}
		allyHomeAreaLocation = new MapLocation(allyLocXSum/allyLocIndex, allyLocYSum/allyLocIndex);
	}
	public abstract void loop() throws Exception;
	
	public boolean areZombiesSpawning(){
		for(int i : zss.getRounds())
			if(i > rc.getRoundNum() - 10 && i < rc.getRoundNum() + 10)
				return true;
		return false;
	}
	
	public static final int estimationSenseRange = 100;
	
	private static RobotInfo[] senseHostileRobotsCachedValue;
	private static int senseHostileRobotsCachedRound = -1;
	public RobotInfo[] senseHostileRobotsCached(){
		if(rc.getRoundNum() > senseHostileRobotsCachedRound)
			return senseHostileRobotsCachedValue = rc.senseHostileRobots(rc.getLocation(),estimationSenseRange);
		else
			return senseHostileRobotsCachedValue;
	}
	private static RobotInfo[] senseEnemyRobotsCachedValue;
	private static int senseEnemyRobotsCachedRound = -1;
	public RobotInfo[] senseEnemyRobotsCached(){
		if(rc.getRoundNum() > senseEnemyRobotsCachedRound)
			return senseEnemyRobotsCachedValue = rc.senseNearbyRobots(rc.getLocation(),estimationSenseRange,enemyTeam);
		else
			return senseEnemyRobotsCachedValue;
	}
	private static RobotInfo[] senseZombieRobotsCachedValue;
	private static int senseZombieRobotsCachedRound = -1;
	public RobotInfo[] senseZombieRobotsCached(){
		if(rc.getRoundNum() > senseZombieRobotsCachedRound)
			return senseZombieRobotsCachedValue = rc.senseNearbyRobots(rc.getLocation(),estimationSenseRange,Team.ZOMBIE);
		else
			return senseZombieRobotsCachedValue;
	}
	private static RobotInfo[] senseFriendlyRobotsCachedValue;
	private static int senseFriendlyRobotsCachedRound = -1;
	public RobotInfo[] senseFriendlyRobotsCached(){
		if(rc.getRoundNum() > senseFriendlyRobotsCachedRound)
			return senseFriendlyRobotsCachedValue = rc.senseNearbyRobots(rc.getLocation(),estimationSenseRange,myTeam);
		else
			return senseFriendlyRobotsCachedValue;
	}
	
	public int hostileCount(){
		return senseHostileRobotsCached().length;
	}
	public int friendlyCount(){
		return senseFriendlyRobotsCached().length;
	}
	public int hostileHealthTotal(){
		int total = 0;
		for(RobotInfo r : senseHostileRobotsCached())
			total += r.health;
		return total;
	}
	public int friendlyHealthTotal(){
		int total = 0;
		for(RobotInfo r : senseFriendlyRobotsCached())
			total += r.health;
		return total;
	}
	public RobotInfo nearestHostile(){ //todo - use this in Scout to avoid ALL enemy units as much as possible
		return nearest(rc.getLocation(),senseHostileRobotsCached());
	}
	public RobotInfo nearestFriendly(){
		return nearest(rc.getLocation(),senseFriendlyRobotsCached());
	}
	public Direction hostileDirection(){
		return averageDirectionToRobots(rc.getLocation(), senseHostileRobotsCached());
	}
	public Direction friendlyDirection(){
		return averageDirectionToRobots(rc.getLocation(), senseFriendlyRobotsCached());
	}
	
	public static Direction averageDirection(Direction[] dirs){
		int totaldx = 0, totaldy = 0;
		for(Direction d : dirs){
			totaldx += d.dx;
			totaldy += d.dy;
		}
		return new MapLocation(0,0).directionTo(new MapLocation(totaldx, totaldy));
	}
	public static Direction averageDirectionToRobots(MapLocation here, RobotInfo[] robots){
		int totaldx = 0, totaldy = 0;
		for(RobotInfo r : robots){
			totaldx += r.location.x - here.x;
			totaldy += r.location.y - here.y;
		}
		return new MapLocation(0,0).directionTo(new MapLocation(totaldx, totaldy));
	}
	public static RobotInfo nearest(MapLocation here, RobotInfo[] others){
		if(others.length == 0)return null;
		if(others.length == 1)return others[0];
		
    	int shortestI = 0;
    	int shortestDistanceSquared = here.distanceSquaredTo(others[0].location);
    	for(int i = 1; i < others.length; i++){
    		if(others[i].type==RobotType.ZOMBIEDEN)continue; //a paltry attempt at prioritization
    		int newdist = here.distanceSquaredTo(others[i].location);
    		if(shortestDistanceSquared > newdist){
    			shortestDistanceSquared = newdist;
    			shortestI = i;
    		}
    	}
    	return others[shortestI];
	}
	public static MapLocation nearest(MapLocation here, MapLocation[] others){
		if(others.length == 0)return null;
    	int shortestI = 0;
    	int shortestDistanceSquared = here.distanceSquaredTo(others[0]);
    	for(int i = 1; i < others.length; i++){
    		int newdist = here.distanceSquaredTo(others[i]);
    		if(shortestDistanceSquared > newdist){
    			shortestDistanceSquared = newdist;
    			shortestI = i;
    		}
    	}
    	return others[shortestI];
	}
	
	
	public static final int[] possibleDirections = {0, 1, -1, 2, -2, 3, -3, 4};
	public ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>();
	public int patience = 30;
	public void squishMove(Direction dir) throws GameActionException{
		if(patience <= 0){
			if(!(rc.getType()!=RobotType.TTM && clearIfRubble(dir)) && rc.canMove(dir)){
				rc.move(dir);
				patience = Math.min(patience + 10, 30);
			}
			return;
		}else{
			for(int d : possibleDirections){
				Direction candidateDirection = Direction.values()[(dir.ordinal()+d+8)%8];
				if(rc.canMove(candidateDirection) && !pastLocations.contains(rc.getLocation().add(candidateDirection))){
					pastLocations.add(rc.getLocation().add(candidateDirection));
					if(pastLocations.size() > 10)
						pastLocations.remove(0);
					rc.move(candidateDirection);
					patience = Math.min(patience + 10, 30);
					return;
				}
			}
		}
		// in case it does something stupid and gets itself stuck
		patience -= 5;
		if(pastLocations.size() > 0)
			pastLocations.remove(0);
	}
	public static final double maxClearableRubble = 100000;
	public boolean clearIfRubble(Direction dir) throws GameActionException{
		double rubble = rc.senseRubble(rc.getLocation().add(dir));
		if(rubble > GameConstants.RUBBLE_OBSTRUCTION_THRESH
				&& rubble < maxClearableRubble){
			rc.clearRubble(dir);
			return true;
		}
		else
			return false;
	}
}
