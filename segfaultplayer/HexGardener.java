package segfaultplayer;
import battlecode.common.*;


public strictfp class HexGardener extends RobotBase
{
	private float[] aArch;
	private MapLocation alphaLoc;
	
	private int[] myPodStatus;
	
	private MapLocation[] myPodLocations;
	
	private Direction[] podDirs = new Direction[] {
			new Direction(0),
			new Direction((float)Math.PI/3.0f),
			new Direction(2.0f*(float)Math.PI/3.0f),
			new Direction(3.0f*(float)Math.PI/3.0f),
			new Direction(4.0f*(float)Math.PI/3.0f),
			new Direction(5.0f*(float)Math.PI/3.0f)
	};

	public HexGardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
		myPodStatus = new int[6];
	}

	public void run() throws GameActionException {

		aArch = CommunicationsHandler.unpack(rc.readBroadcast(1));
		alphaLoc = new MapLocation(aArch[0],aArch[1]);
		
		// first thing we have to do is fine a location for our pod

		findPodLocation();
		
		while(true) {
			Clock.yield();
		}
	}
	
	/**
	 * by the termination of this method, the gardener will be in an optimal (decent)
	 * location to build a hex pod
	 * @throws GameActionException
	 */
	public void findPodLocation() throws GameActionException {
		// for now, simplistic move 15 steps away from archon that spawned us
		Direction dir = alphaLoc.directionTo(rc.getLocation());
		for(int i = 0; i < 15; i ++) {
			if(rc.canMove(dir))
				rc.move(dir);
			Clock.yield();
		}
	}
	
	/**
	 * sets podLocations[] according to podDirs and current location
	 * does not call Clock.yield()
	 * @throws GameActionException
	 */
	public void setPodLocations() throws GameActionException {
		myPodLocations = new MapLocation[6];
		for(int i = 0; i < podDirs.length; i ++) {
			myPodLocations[i] = rc.getLocation().add(podDirs[i],2.05f); // 0.05f buffer
		}
	}
	
	/**
	 * updates myPodStatus according to which potential pod locations are open
	 * does not call Clock.yield()
	 * @throws GameActionException
	 */
	public void updatePodStatus() throws GameActionException {
		TreeInfo[] obstructingTrees = rc.senseNearbyTrees(3.5f);
		RobotInfo[] obstructingRobots = rc.senseNearbyRobots(3.5f);
		for(int i = 0; i < 6; i ++) myPodStatus[i] = 0;
	}
	
	/**
	 * uses indicator dots to illustrate the status of each potential pod location
	 * red = occupied by tree
	 * purple = occupied by unit
	 * blue = F R E E 
	 */
	public void drawPodStatus() throws GameActionException {
		
	}
}