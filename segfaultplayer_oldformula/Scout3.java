package segfaultplayer_oldformula;
import java.util.HashSet;
import java.util.Set;

import battlecode.common.*;


public strictfp class Scout3 extends RobotBase
{

	public Scout3(RobotController rc, int id) throws GameActionException {
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
		float sR = rc.getType().sensorRadius; // D Y N A M I C
		float stride = rc.getType().strideRadius; // C O D E
		System.out.println(rc.getLocation().distanceTo(el));
		boolean isAtEnemy = false;
		int counter = 0;
		Direction dir = rc.getLocation().directionTo(el);
		MapLocation target = el;
		while (true) {
			//System.out.println("DANKDNAKNDAK");
			counter++;
			if(rc.senseNearbyBullets(5f).length>0) {
				moveWithDodging(el);
				Clock.yield();
			} else {
				MapLocation myLoc = rc.getLocation();
				boolean hasShaken = false;
				TreeInfo[] myTrees = rc.senseNearbyTrees(sR);
				// going towards enemy archon when it doesn't have any bullets
				
				if (!isAtEnemy) {
					isAtEnemy = rc.getLocation().distanceTo(el) < rc.getLocation().distanceTo(allyArchons[0]);
					dir = myLoc.directionTo(el);
					// go in random directions after reaching enemy archon
				} else {
					isAtEnemy=true;
					if(counter%15==0) { // change direction
						dir = randomDirection();
						while(!rc.canMove(dir)) {
							dir = randomDirection();
						}
					}
				}
				
				for (TreeInfo k : myTrees) {
					float q = k.radius;
					if(uniqueTrees.add(k.ID)) {
						if(!isAtEnemy) {
							scaledNumTrees += (q*q);
						}
					}
					if(k.containedBullets!=0) {
						if(!hasShaken && rc.canShake(k.ID)){
							rc.shake(k.ID);
							hasShaken = true;
						} else {
							dir = myLoc.directionTo(k.location);
							target = k.location;
							break;
						}
					}
				}

				//broadcast tree data
				if(!isAtEnemy) {
					rc.broadcast(151, uniqueTrees.size());
					rc.broadcast(152, (int)scaledNumTrees);
				}
				//move
				if(rc.canMove(dir, stride)) {
					rc.move(dir, stride);
					if(!isAtEnemy) {
						steps+=1;
						rc.broadcast(150, steps);
					}
					if(rc.getRoundNum() < 100) {
						float broadValue = 
							(uniqueTrees.size() / (steps*(float)Math.PI)) 
							* (allyArchons[0].distanceTo(el) / 100f)
							* (1f / ((float)rc.readBroadcast(12)/1000f));
						int val = (int)(broadValue*1000f);
						System.out.println(broadValue);
						rc.broadcast(13, val);
					} else {
						float broadValue = 
								(uniqueTrees.size() / (steps*(float)Math.PI)) 
								* (allyArchons[0].distanceTo(el) / 100f)
								* (1f / ((float)rc.readBroadcast(12)/500f))
								* (100f / (float)rc.getRoundNum());
						int val = (int)(broadValue*1000f);
						System.out.println(broadValue);
						rc.broadcast(13, val);
					}
					Clock.yield();
				} else {
					pathFind(target);
					Clock.yield();
				}
			}

		}
	}

}