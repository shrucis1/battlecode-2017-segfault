package segfaultplayer;
import java.util.HashSet;
import java.util.Set;

import battlecode.common.*;

//
public strictfp class Scout extends RobotBase
{

	public String alphabet = "abcdefghijklmnopqrstuvwxyz";

	public Scout(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void run() throws GameActionException {
		MapLocation dank = rc.getInitialArchonLocations(enemy)[0];
		System.out.println(dank);
		bulletPath(dank);
	}

	public void bulletPath(MapLocation el) throws GameActionException {
		Set<Integer> uniqueTrees = new HashSet<Integer>();
		float scaledNumTrees = 0;
		int steps = 0;
		float sR = rc.getType().sensorRadius;
		float stride = 1.25f;

		System.out.println("dank Memes");
		System.out.println(rc.getLocation().distanceTo(el));


		while (rc.getLocation().distanceTo(el) > 5.0f) {
			boolean hasMoved = false;
			MapLocation myLoc = rc.getLocation();
			float distance = myLoc.distanceTo(el);
			TreeInfo[] myTrees = rc.senseNearbyTrees(sR);
			Direction dir = myLoc.directionTo(el);
			for (TreeInfo k : myTrees) {
				uniqueTrees.add(k.ID);
				if(k.containedBullets!=0) {
					if(rc.canShake(k.ID)){
						rc.shake(k.ID);
						System.out.println("Got a bullet lolololol");
					} else {
						dir = myLoc.directionTo(k.location);
						break;
					}
				}
			}

			
			//broadcast tree data
			rc.broadcast(151, uniqueTrees.size());
			//rc.broadcast(152, (int)scaledNumTrees);
			//pathfinding
			if(rc.canMove(dir, stride)) {
				rc.move(dir, stride);
				steps+=1;
				rc.broadcast(150, steps);
				Clock.yield();
			} else {
				Clock.yield();
			}
		}
	}
}