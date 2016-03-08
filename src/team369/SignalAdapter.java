package team369;

import java.util.ArrayList;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Signal;

public class SignalAdapter {
	//TO DO NEXT.
	//just have simple commands
	//move & guard here [kinda like sc2 simplicity]
		//what robot type/which robots
		//what priority level
		//what location
		//when to stop relaying the message
	//kite-and-run [baiting toward enemy team]
	//and then the informational stuff like ZOMBIE_HOME we can add later
	
	private static final int INT_LENGTH = 32; //bits
	private static final int COMMAND_LENGTH = 4; //bits
	private static final int COMMAND_MASK = 0xf0000000;
	private static final int COMMAND_REMAINING = INT_LENGTH - COMMAND_LENGTH; // bits
	
	public static enum Cmd{
		ENEMY_POSITION,
		ZOMBIE_POSITION,
		GO_AWAY,
		MOVE_HERE,
		ALLY_HOME,
		ENEMY_HOME,
		ZOMBIE_HOME,
		COMMANDER_VOTE,
		LONG_RANGE_MESSAGE;//hmm
	}
	
	
	private RobotController rc;
	public ArrayList<Signal> allyMessageSignals = new ArrayList<>(), 
			allyBasicSignals = new ArrayList<>(), 
			enemyMessageSignals = new ArrayList<>(), 
			enemyBasicSignals = new ArrayList<>();
	
	private boolean isCommander = false;
	public boolean isCommander(){return isCommander;}
	public final int COMMANDER_VOTE_RANGE = 100;
	public void sendCommanderVote() throws Exception{
		broadcastCommand(Cmd.COMMANDER_VOTE,0,0,COMMANDER_VOTE_RANGE);
		Signal[] sigs = rc.emptySignalQueue();
		for(Signal s : sigs)
			if(isCommand(s.getMessage(),Cmd.COMMANDER_VOTE))
				return;
		isCommander = true;
	}
	public SignalAdapter(RobotController rc){
		this.rc = rc;
		isCommander = false;
	}
	public void fetch(){
		Signal[] raw = rc.emptySignalQueue();
		allyMessageSignals.clear();
		allyBasicSignals.clear();
		enemyMessageSignals.clear();
		enemyBasicSignals.clear();
		
		for(Signal s : raw){
			if(s.getTeam() == rc.getTeam()){
				if(s.getMessage() != null)
					allyMessageSignals.add(s);
				else
					allyBasicSignals.add(s);
			}
			else{
				if(s.getMessage() != null)
					enemyMessageSignals.add(s);
				else
					allyBasicSignals.add(s);
			}
		}
	}
	
	public void broadcastGoAway(MapLocation loc, int broadcastRange) throws GameActionException{
		broadcastLocationCommand(Cmd.GO_AWAY, loc, 0, broadcastRange);
	}
	public void broadcastMoveHere(MapLocation loc, int broadcastRange) throws GameActionException{
		broadcastLocationCommand(Cmd.MOVE_HERE, loc, 0, broadcastRange);
	}
	public void broadcastAllyHome(MapLocation loc, int broadcastRange) throws GameActionException{
		broadcastLocationCommand(Cmd.ALLY_HOME, loc, 0, broadcastRange);
	}
	public void broadcastEnemyHome(MapLocation loc, int broadcastRange) throws GameActionException{
		broadcastLocationCommand(Cmd.ENEMY_HOME, loc, 0, broadcastRange);
	}
	public void broadcastZombieHome(MapLocation loc, int broadcastRange) throws GameActionException{
		broadcastLocationCommand(Cmd.ZOMBIE_HOME, loc, 0, broadcastRange);
	}
	public void broadcastEnemyPosition(MapLocation loc, int broadcastRange) throws GameActionException{
		broadcastLocationCommand(Cmd.ENEMY_POSITION, loc, 0, broadcastRange);
	}
	public void broadcastZombiePosition(MapLocation loc, int broadcastRange) throws GameActionException{
		broadcastLocationCommand(Cmd.ZOMBIE_POSITION, loc, 0, broadcastRange);
	}
	
	//10 bits each for location x and y
	//could theoretically broadcast three locations at once with command :D
	private void broadcastLocationCommand(Cmd cmd, MapLocation loc, int data2, int broadcastRange) throws GameActionException{
		rc.broadcastMessageSignal((cmd.ordinal() << COMMAND_REMAINING) + ((loc.x % 1024) << 10) + (loc.y % 1024), data2, broadcastRange);
	}
	public static MapLocation getLocation(int[] msg){
		return new MapLocation((msg[0]>>>10)%1024, msg[0]%1024);
	}
	/*public void broadcastTripleLocationCommand(Cmd cmd, MapLocation loc1, MapLocation loc2, MapLocation loc3, int broadcastRange){
		//rc.broadcastMessageSignal((cmd.ordinal() << COMMAND_REMAINING) + ((loc.x % 1024) << 10) + (loc.y % 1024), data2, broadcastRange);
	}
	public static MapLocation[] getTripleLocation(int[] msg){
		
	}*/
	
	private void broadcastCommand(Cmd cmd, int data1, int data2, int broadcastRange) throws GameActionException{
		rc.broadcastMessageSignal((data1 | COMMAND_MASK) & ~(~cmd.ordinal() << COMMAND_REMAINING), data2, broadcastRange);
	}
	private static Cmd getCommand(int[] msg){
		return Cmd.values()[msg[0] >>> COMMAND_REMAINING];
	}
	public static boolean isCommand(int[] msg, Cmd cmd){
		return (msg[0] >>> COMMAND_REMAINING) == cmd.ordinal();
	}
	public static int getData1(int rawdata1){
		return rawdata1 & ~COMMAND_MASK;
	}
	public MapLocation[] getSignalingEnemyLocations(){
		MapLocation[] locs = new MapLocation[enemyMessageSignals.size() + enemyBasicSignals.size()];
		int i = 0;
		for(; i < enemyMessageSignals.size(); i++)
			locs[i] = enemyMessageSignals.get(i).getLocation();
		for(int j = 0; j < enemyBasicSignals.size(); j++)
			locs[i+j] = enemyBasicSignals.get(j).getLocation();
		return locs;
	}
}
