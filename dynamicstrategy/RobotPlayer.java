package dynamicstrategy;
import java.util.Random;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Random rand;
    static int myID;
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        rand = new Random();
        int timeSeed = rand.nextInt();
        rand = new Random(timeSeed + rc.getID());
        
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SCOUT:
            	runScout();
            	break;
            default:
            	break;
        }
	}
    
    static void runArchon() throws GameActionException {
    	// T=1
    	// determine center
    	if(rc.readBroadcast(0) == 0) {
    		System.out.println("Approx center has not been located, locating it...");
    		float[] center = locateApproxCenter();
    		System.out.println("roughly @ (" + center[0] + "," + center[1] + ")");
    		rc.broadcast(0, pack(center[0], center[1]));
    	}
    	// determine alpha
    	int preID = rc.readBroadcast(500) + 1;
    	rc.broadcast(500, preID);
    	float[] center = unpack(rc.readBroadcast(0));
    	System.out.println("Read center as " + center[0] + ", " + center[1]);
    	float myDist = rc.getLocation().distanceSquaredTo(new MapLocation(center[0],center[1]));
    	System.out.println("My coords: " + rc.getLocation().x + ", " + rc.getLocation().y);
    	System.out.println("PreID: " + preID + ", My dist to center^2: " + myDist);
    	rc.broadcast(500+preID, (int)(myDist));
    	Clock.yield();
    	// T=2
    	int rank = 0;
    	int numArchons = rc.readBroadcast(500);
        for(int i = 0; i < numArchons; i ++) {
    		if((int)(myDist) > rc.readBroadcast(501+i))
    			rank ++;
    	}
    	System.out.println("Self-identified as rank #" + rank);
    	boolean isAlpha = (rank == 0);
    	if(isAlpha) {
    		System.out.println("I am the Alpha, broadcasting my coords");
    		rc.broadcast(1, pack(rc.getLocation().x,rc.getLocation().y));
    	}
    	// all archons assess their surroundings
    	TreeInfo[] nearbyTrees  = rc.senseNearbyTrees();
    	float[] treeMassByDirection = new float[16];
    	// tree mass by direction represents roughly area of a tree in a given direction,
    	// giving additional weight to closer trees (think inverse of a moment of inertia)
    	Direction dir;
    	MapLocation myLocation = rc.getLocation();
    	float inDeg, dist;
    	int realDir;
    	for(TreeInfo ti : nearbyTrees) {
    		dir = myLocation.directionTo(ti.getLocation());
    		inDeg = dir.getAngleDegrees() + 11.25f;
    		while(inDeg < 360f) inDeg += 360f;
    		while(inDeg > 360f) inDeg -= 360f;
    		realDir = (int)(inDeg/22.5f);
    		dist = myLocation.distanceTo(ti.getLocation());
    		treeMassByDirection[realDir] += (ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    		if(ti.radius > (dist * (float)Math.PI/8.0f)) {
        		treeMassByDirection[(realDir+1)%16] += (0.25f * ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
        		treeMassByDirection[(realDir+15)%16] += (0.25f * ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    		}
    	}
    	float totalTreeMassFactor = 0.0f;
    	for(int i = 0; i < 16; i ++) {
    		totalTreeMassFactor += treeMassByDirection[i];
    	}
    	System.out.println("Total Tree Mass Factor: " + totalTreeMassFactor);
    	for(int i = 0; i < 16; i ++) {
    		dir = new Direction(i*(float)Math.PI/8.0f);
    		for(dist = 2.49f; dist < 10.0f; dist += 2.49f) {
    			if(!rc.onTheMap(myLocation.add(dir,dist))) {
    				// boundary in this direction, pretend it's a huge tree
    				treeMassByDirection[i] += 35 * (10.0f-dist)*(10.0f-dist);
    			}
    		}
    	}
    	for(int i = 0; i < 16; i ++) {
    		System.out.println("In direction " + i + ", " + treeMassByDirection[i]);
    	}
    	float[] smoothedTreeMassByDirection = new float[16];
    	for(int i = 0; i < 16; i ++) {
    		smoothedTreeMassByDirection[i] += 4.0f * treeMassByDirection[i];
    		smoothedTreeMassByDirection[i] += 2.0f * treeMassByDirection[(15+i)%16];
    		smoothedTreeMassByDirection[i] += 2.0f * treeMassByDirection[(17+i)%16];
    		smoothedTreeMassByDirection[i] += 1.0f * treeMassByDirection[(14+i)%16];
    		smoothedTreeMassByDirection[i] += 1.0f * treeMassByDirection[(18+i)%16];
    		smoothedTreeMassByDirection[i] += 0.5f * treeMassByDirection[(13+i)%16];
    		smoothedTreeMassByDirection[i] += 0.5f * treeMassByDirection[(19+i)%16];
    		smoothedTreeMassByDirection[i] += 0.25f * treeMassByDirection[(12+i)%16];
    		smoothedTreeMassByDirection[i] += 0.25f * treeMassByDirection[(20+i)%16];
    		smoothedTreeMassByDirection[i] += 0.125f * treeMassByDirection[(11+i)%16];
    		smoothedTreeMassByDirection[i] += 0.125f * treeMassByDirection[(21+i)%16];
    		smoothedTreeMassByDirection[i] += 0.0625f * treeMassByDirection[(10+i)%16];
    		smoothedTreeMassByDirection[i] += 0.0625f * treeMassByDirection[(22+i)%16];
    		smoothedTreeMassByDirection[i] += 0.03125f * treeMassByDirection[(9+i)%16];
    		smoothedTreeMassByDirection[i] += 0.03125f * treeMassByDirection[(23+i)%16];
    	}
    	
    	int bestDirection = 0;
    	for(int i = 1; i < 16; i ++) {
    		if(smoothedTreeMassByDirection[i] < smoothedTreeMassByDirection[bestDirection]) {
    			bestDirection = i;
    		}
    	}
    	int secondBestDirection = 0;
    	for(int i = 1; i < 16; i ++) {
    		if(i == bestDirection) continue;
    		if(smoothedTreeMassByDirection[i] < smoothedTreeMassByDirection[secondBestDirection]) {
    			secondBestDirection = i;
    		}
    	}
    	
    	if(isAlpha) {
    		Direction buildDir = findBuildDirection(bestDirection*(float)Math.PI/8.0f,RobotType.GARDENER);
    		System.out.println("bestDirection is " + bestDirection);
    		if(buildDir != null) {
    			rc.buildRobot(RobotType.GARDENER, buildDir);
    			rc.broadcast(400+rank, 1);
    		}
    		else
    			System.out.println("BIG PROBLEM!!! EDGE CASE!!! DIRECTION IS BADDDDD");
    		
        	rc.broadcast(201, rc.readBroadcast(201) + 1); // add urgent order for scout
    	}
    	
    	
    	Clock.yield();
    	// T=3
    	// gardener starts building a scout
    	
    	Clock.yield();
    	// T=4+
    	
    	// now we have non-alpha ones build gardeners
    	if(!isAlpha) {
    		Direction buildDir = findBuildDirection(bestDirection*(float)Math.PI/8.0f,RobotType.GARDENER);
    		System.out.println("bestDirection is " + bestDirection);
    		if(buildDir != null) {
    			rc.buildRobot(RobotType.GARDENER, buildDir);
    		}
    		else
    			System.out.println("BIG PROBLEM!!! EDGE CASE!!! DIRECTION IS BADDDDD");
    	}
    	

    	if(isAlpha) {
    		
    		
    		if(totalTreeMassFactor < 5.0f) {
    			// very light, tree/soldier centric approach
    			
    		}
    		else if(totalTreeMassFactor < 150.0f) {
    			// medium ish
            	rc.broadcast(202, rc.readBroadcast(202) + 1); // add urgent order for lumberjack
            	
    		}
    		else {
    			// we gotta get these trees out of the way

            	rc.broadcast(202, rc.readBroadcast(202) + 3); // add urgent order for lumberjacks
            	
            	//TODO: finish alpha logic for managing the game from this point on
            	// by tweaking spawnrates etc.
    		}
    		
    		
    	} else {
	    	Clock.yield();
	    	for(int t = 0; true; t++) {
	    		if(t%50 == rank) {
	    			if(rand.nextDouble() < Math.max(25.0/t, 0.1)) { // lower chance to spawn new ones as time goes on
	    				float tdir = rand.nextFloat()*2.0f*(float)Math.PI;
	    				for(float j = 0.0f; j < 2.0f*(float)Math.PI; j+=(float)Math.PI/16.0) {
	    					if(rc.canBuildRobot(RobotType.GARDENER, new Direction(tdir + j))) {
	    						rc.buildRobot(RobotType.GARDENER, new Direction(tdir+j));
	    						break;
	    					}
	    				}
	    			}
	    		}
	    		Clock.yield();
	    		// other than occasionally spawn gardeners, do nothing
	    	}
    	}
    }
    
    
    
    /*
     * Returns approximate center in following format:
     * float[] {x, y}
     */
    static float[] locateApproxCenter() throws GameActionException {
    	MapLocation[] initArchonLocsA = rc.getInitialArchonLocations(Team.A);
    	MapLocation[] initArchonLocsB = rc.getInitialArchonLocations(Team.B);
    	MapLocation[] initArchonLocs = new MapLocation[2 * initArchonLocsA.length];
    	int t = 0;
    	for(MapLocation ml : initArchonLocsA)
    		initArchonLocs[t++] = ml;
    	for(MapLocation ml : initArchonLocsB)
    		initArchonLocs[t++] = ml;
    	float netX = 0.0f;
    	float netY = 0.0f;
    	for(MapLocation ml : initArchonLocs) {
    		netX += ml.x;
    		netY += ml.y;
    	}
    	
    	return new float[] {netX/initArchonLocs.length, netY/initArchonLocs.length};
    }
    
    static void runGardener() throws GameActionException {
    	myID = rc.readBroadcast(101);
    	rc.broadcast(101, myID + 1);
    	System.out.println("Gardener #" + myID + " spawned");
    	gardenerFactory();
    }
    
    
    
    // MAJOR TODO: add order receiving code into gardener
    // both URGENT and regular
    static void gardenerFactory() throws GameActionException {

        int DONOTBUILDHERE = rand.nextInt(4);
        boolean clear = false;
        boolean corners = false;
        boolean[] cs = new boolean[4];
        boolean[] hasPlantedCardinal = new boolean[4];
        int numCardinalLeft = 4;
        
        for(int i=0; i<4; i++)
            cs[i] = false;
        int status = 0;
        
        Direction curDir = randomDirection();
        
        int wanderTurns = 0;
        
        // get to clear area
        while(!clear) {
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees(3.5f);
			RobotInfo[] nearbyRobots = rc.senseNearbyRobots(3.5f);
			if (nearbyTrees.length == 0 && nearbyRobots.length == 0
					&& rc.onTheMap(rc.getLocation(), 3.5f) == true)
				clear = true;
			if(!clear) {
				for(int i = 0; i < 10 && !rc.canMove(curDir); i ++)
					curDir = randomDirection();
				if(rc.canMove(curDir))
					rc.move(curDir);
				Clock.yield();
				wanderTurns ++;
			}
        }
        
        // now construct our "pod" / "factory"
        boolean doneBuilding = false;
        while(!doneBuilding) {
        	gardenerWaterLowest();
			if (!corners) { // build diagonals first, left-top,
							// left-bottom, right-top, right-bottom
				if (cs[0] == false && rc.hasTreeBuildRequirements()) {
					if (status == 0) {
						attemptmove(new Direction((float) (Math.PI)));
						Clock.yield();
						attemptmove(new Direction((float) (Math.PI)));
						status = 1;
					}
					if (rc.canPlantTree(new Direction((float) (Math.PI / 2.0 + .1)))) {
						rc.plantTree(new Direction((float) (Math.PI / 2.0 + .1)));
						cs[0] = true;
					}
				} else if (cs[2] == false && rc.hasTreeBuildRequirements()) {
					// attemptmove(new Direction((float)(Math.PI)));
					if (rc.canPlantTree(new Direction((float) (Math.PI * 3 / 2.0 - .1)))) {
						rc.plantTree(new Direction((float) (Math.PI * 3 / 2.0 - .1)));
						attemptmove(new Direction((float) (0.0)));
						Clock.yield();
						attemptmove(new Direction((float) (0.0)));
						cs[2] = true;
					}
				} else if (cs[1] == false && rc.hasTreeBuildRequirements()) {
					if (status == 1) {
						attemptmove(new Direction((float) (0.0)));
						Clock.yield();
						attemptmove(new Direction((float) (0.0)));
						status = 2;
					}
					if (rc.canPlantTree(new Direction((float) (Math.PI / 2.0 - .1)))) {
						rc.plantTree(new Direction((float) (Math.PI / 2.0 - .1)));
						cs[1] = true;
					}
				} else if (cs[3] == false && rc.hasTreeBuildRequirements()) {
					// attemptmove(new Direction((float)(0.0)));
					if (rc.canPlantTree(new Direction((float) (Math.PI * 3 / 2.0 + .1)))) {
						rc.plantTree(new Direction((float) (Math.PI * 3 / 2.0 + .1)));
						attemptmove(new Direction((float) (Math.PI)));
						Clock.yield();
						attemptmove(new Direction((float) (Math.PI)));
						cs[3] = true;
					}
				}
				if (cs[0] == true && cs[1] == true && cs[2] == true && cs[3] == true)
					corners = true;
			} else { // build cardinal directions, skip over hole to
						// build units
				for (int i = 0; i < 4; i++) {
					if (!hasPlantedCardinal[i] && DONOTBUILDHERE != i
							&& rc.canPlantTree(new Direction((float) (0.0 + Math.PI / 2.0 * i)))) {
						rc.plantTree(new Direction((float) (0.0 + Math.PI / 2.0 * i)));
						hasPlantedCardinal[i] = true;
						numCardinalLeft --;
					}
				}
			}
			if(numCardinalLeft == 0)
				doneBuilding = true;
			Clock.yield();
        }
        
        // whoopie, now we're done building
        while(true) {
        	gardenerWaterLowest();
        	Clock.yield();
        }
    }
    
    private static void gardenerWaterLowest() throws GameActionException {
		TreeInfo[] myTrees = rc.senseNearbyTrees(2.0f);

		if (myTrees.length > 0) { // Waters lowest
			double hp = 100.0;
			int water = 0;
			for (int i = 0; i < myTrees.length; i++) {
				if ((double) myTrees[i].getHealth() < hp) {
					hp = (double) myTrees[i].getHealth();
					water = i;
				}
			}
			rc.water(myTrees[water].getID());
		}
    }
    	
    private static void attemptmove(Direction direction) throws GameActionException {
    	if(rc.canMove(direction)) {
    		rc.move(direction);
    	}
	}

	/*	while(true) {
    		Direction dir = randomDirection();
    		for(int i = 0; i < 10 && !rc.canBuildRobot(RobotType.SCOUT, dir); i++) {
    			dir = randomDirection();
    		}
    		if(rc.canBuildRobot(RobotType.SCOUT, dir)) {
    			rc.buildRobot(RobotType.SCOUT, dir);
        		while(true)
        			Clock.yield();
    		}
    		Clock.yield();
    	}
    }*/
    
    static void runScout() throws GameActionException {
    	
    	myID = rc.readBroadcast(102);
    	rc.broadcast(102, myID+1);
    	System.out.println("Scout #" + myID + " spawned");
    	
    	Team enemy = rc.getTeam().opponent();
    	int steps = 0;
    	int totalTrees = 0;
    	MapLocation myLocation;
    	
    	if(myID == 0) {
    		System.out.println("I am initial scout");
    		// Mission: harass enemy archon, finding path while on way
    		// added tree numbers
    		// TODO: add path finding logic here
    		float cdist;
    		do {
    			myLocation = rc.getLocation();
    			MapLocation[] initArchLocs = rc.getInitialArchonLocations(enemy);
	    		MapLocation closest = initArchLocs[0];
	    		cdist = myLocation.distanceTo(closest);
	    		for(int i = 1; i < initArchLocs.length; i ++) {
	    			if(initArchLocs[i].distanceTo(myLocation) < cdist) {
	    				cdist = initArchLocs[i].distanceTo(myLocation);
	    				closest = initArchLocs[i];
	    			}
	    		}
	    		Direction dir = myLocation.directionTo(closest);
	    		if(rc.canMove(dir)) 
	    			rc.move(dir);
	    		else if(rc.canMove(dir.rotateRightDegrees(68.3f)))
	    			rc.move(dir.rotateRightDegrees(68.3f));
	    		else if(rc.canMove(dir.rotateLeftDegrees(68.3f)))
	    			rc.move(dir.rotateLeftDegrees(68.3f));
	    		else if(rc.canMove(dir.rotateRightDegrees(130.0f)))
	    			rc.move(dir.rotateRightDegrees(130.0f));
	    		else if(rc.canMove(dir.rotateLeftDegrees(130.0f)))
	    			rc.move(dir.rotateLeftDegrees(130.0f));
	    		Clock.yield();
    		} while(cdist > 8.5f);
    		System.out.println("I'm " + (cdist-2.5f) + " away from a starting enemy archon location");
    		// now within 6.0 of starting enemy archon location
    		
    		RobotInfo[] nearbyRobots;
    		TreeInfo[] nearbyTrees;
    		Direction mdir = randomDirection();
    		while(true) {
    			
    			myLocation = rc.getLocation();
    			nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
    			//tree data
    			if (steps<50) {
        			nearbyTrees = rc.senseNearbyTrees(myLocation, 10f, Team.NEUTRAL);
	    			totalTrees += nearbyTrees.length;
	    			steps+=1;
	    			rc.broadcast(150, steps);
	    			rc.broadcast(151, totalTrees);
    			}
    			//end tree data
    			// priorities for scout harassment:
    			// closest enemy gardener
    			// closest enemy archon
    			if(nearbyRobots.length > 0) {
    				RobotInfo closestGardener = null;
    				RobotInfo closestArchon = null;
    				for(RobotInfo ri : nearbyRobots) {
    					if(ri.getType() == RobotType.GARDENER) {
    						if(closestGardener == null) {
    							closestGardener = ri;
    						} else if(ri.getLocation().distanceTo(myLocation) < closestGardener.getLocation().distanceTo(myLocation)) {
    							closestGardener = ri;
    						}
    					} else if(ri.getType() == RobotType.ARCHON) {
    						if(closestArchon == null) {
    							closestArchon = ri;
    						} else if(ri.getLocation().distanceTo(myLocation) < closestArchon.getLocation().distanceTo(myLocation)) {
    							closestArchon = ri;
    						}
    					}
    				}
    				if(closestGardener != null) {
    					// attack closestGardener
    					float distTo = closestGardener.getLocation().distanceTo(myLocation);
    					Direction towardsTarget = myLocation.directionTo(closestGardener.getLocation());
    					if(distTo < 3.0f) { // 2 is used up by radius
    						if(rc.canFireSingleShot()) {
    							rc.fireSingleShot(towardsTarget);
    						}
    					} else {
    						// move closer
    						if(distTo < 4.5f) { // 2.0f (radius) + 2.5f (stride dist) 
    							if(rc.canMove(towardsTarget, distTo-2.5f)) {
    								rc.move(towardsTarget, distTo-2.5f);
    							}
    							else {
    	    						if(rc.canFireSingleShot()) {
    	    							rc.fireSingleShot(towardsTarget);
    	    						}
    							}
    						} else {
    							if(rc.canMove(towardsTarget)) {
    								rc.move(towardsTarget);
    							}
    							else if(rc.canMove(towardsTarget.rotateRightDegrees(90.0f))) {
    								rc.move(towardsTarget.rotateRightDegrees(90.0f));
    							}
    							else if(rc.canMove(towardsTarget.rotateLeftDegrees(90.0f))) {
    								rc.move(towardsTarget.rotateLeftDegrees(90.0f));
    							}
    						}
    					}
    				}
    				else {
    					// attack closestArchon
    					float distTo = closestArchon.getLocation().distanceTo(myLocation);
    					Direction towardsTarget = myLocation.directionTo(closestArchon.getLocation());
    					if(distTo < 4.0f) { // 3 is used up by radius
    						if(rc.canFireSingleShot()) {
    							rc.fireSingleShot(towardsTarget);
    						}
    					} else {
    						// move closer
    						if(distTo < 5.5f) { // 3.0f (radius) + 2.5f (stride dist) 
    							if(rc.canMove(towardsTarget, distTo-2.5f)) {
    								rc.move(towardsTarget, distTo-2.5f);
    							}
    							else {
    	    						if(rc.canFireSingleShot()) {
    	    							rc.fireSingleShot(towardsTarget);
    	    						}
    							}
    						} else {
    							if(rc.canMove(towardsTarget)) {
    								rc.move(towardsTarget);
    							}
    							else if(rc.canMove(towardsTarget.rotateRightDegrees(90.0f))) {
    								rc.move(towardsTarget.rotateRightDegrees(90.0f));
    							}
    							else if(rc.canMove(towardsTarget.rotateLeftDegrees(90.0f))) {
    								rc.move(towardsTarget.rotateLeftDegrees(90.0f));
    							}
    						}
    					}
    				}
    				Clock.yield();
    			}
    			else {
    				// wander around until this is no longer the case
    				while(nearbyRobots.length == 0) { 
    					if(rc.canMove(mdir)) {
    						rc.move(mdir);
    					}
    					else {
    						for(int i = 0; !rc.canMove(mdir) && i < 25; i ++) {
    							mdir = randomDirection();
    						}
    						if(rc.canMove(mdir)) {
    							rc.move(mdir);
    						}
    					}
    					Clock.yield();
    					nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
    				}
    			}
    			
    		}
    	}
    	
    	
    	//}catch(Exception e){
    	//	e.printStackTrace();
    	//}
    }
    
    
    
    // TODO: repurpose circle scout code
    
    /*
    
    static void circleScout() throws GameActionException {
    	float[] alphalocation = unpack(rc.readBroadcast(1));
    	MapLocation al = new MapLocation(alphalocation[0], alphalocation[1]);
		//get sufficient distance away from archon
    	float[] centerlocation = unpack(rc.readBroadcast(0));
    	MapLocation cl = new MapLocation(centerlocation[0], centerlocation[1]);
    	MapLocation myLoc = rc.getLocation();
    	Direction dir = myLoc.directionTo(cl);
    	float mdist = myLoc.distanceTo(al);
    	System.out.println("Circlescout, about to move away");
    	while(mdist < 10) { // circle radius @17
    		System.out.println("Still in loop");
    		if(mdist > 14.5) {
    			if(rc.canMove(dir, 17.0f-mdist)) {
    				rc.move(dir, 17.0f-mdist);
    			}
    		}
    		else if(rc.canMove(dir)) {
    			rc.move(dir);
    		}
    		
    		Clock.yield();
    		myLoc = rc.getLocation();
    		mdist = myLoc.distanceTo(al);
    	}
    	
    	
    	for(int i = 0; i < 43; i ++) {
    		myLoc = rc.getLocation();
    		dir = myLoc.directionTo(al);
    		dir = dir.rotateRightDegrees(85.78f); // with stride radius 2.5, this keeps in circle
    		if(rc.canMove(dir)) {
    			rc.move(dir);
    		}
    		else {
    			System.out.println("Cannot move (circle) in dir " + dir);
    		}
    		Clock.yield();
    	}
    	// guaranteed circle radius @ almost exactly 17.0f
    	
    	
    	// for now, perpetually circle
    	
		//circle around archon
		float theta = 7.5f; // 48 * 7.5 = 360 degrees
		Direction alphaToScout = al.directionTo(rc.getLocation());
		MapLocation newLoc = rc.getLocation();
		int[] totaltrees = new int[48];
		int sumtrees = 0;
		for (int i=0; i<48; i++) {
			//sense trees (a simplistic approach)
			if (rc.onTheMap(rc.getLocation(), 10.0f)) {
    			TreeInfo[] mytrees = rc.senseNearbyTrees();
    			sumtrees += mytrees.length;
    			totaltrees[i] = mytrees.length;
    			System.out.println(mytrees.length);
			}
			// move scout
			alphaToScout = alphaToScout.rotateRightDegrees(theta);
			System.out.println(alphaToScout);
			newLoc = al.add(alphaToScout, 19.0f);
			//make sure this is less than 2.5, but it should be
			if (rc.canMove(newLoc)) {
				System.out.println("MOVING BRUH");
				System.out.println(newLoc);
				rc.move(newLoc);
			} else {
				System.out.println("RIP CANT MOVE THIS SHOULD NEVER HAPPEN UNLESS WE REACH THE EDGE");
			}
            Clock.yield();
		}
		rc.broadcast(151, (int)sumtrees);
    }
    */
    
    
    // ====================== HELPER =======================
    
    
    public static int pack(float x, float y) {
		return (int)((((int)(x*10))/10.0*100000) + (int)(y*10));
	}
    
	public static float[] unpack(int p) {
		float[] ret = new float[2];
		ret[0] = ((p / 10000) / 10.0f);
		ret[1] = ((p % 10000) / 10.0f);
		return ret;
	}
	
	
	public static boolean canAfford(RobotType rt) {
		return rc.getTeamBullets() > rt.bulletCost;
	}
	
	
	// checks canBuild in up to pi/8 in either direction (increments of pi/64)
	public static Direction findBuildDirection(float angle, RobotType rt) {
		Direction dir = new Direction(angle);
		if(rc.canBuildRobot(rt, new Direction(angle)))
			return new Direction(angle); // cue "that was easy"
		for(int i = 1; i <= 8; i ++) {
			dir = new Direction(angle + (i*(float)Math.PI/64.0f));
			if(rc.canBuildRobot(rt,dir))
				return dir;
			dir = new Direction(angle - (i*(float)Math.PI/64.0f));
			if(rc.canBuildRobot(rt,dir))
				return dir;
		}
		return null; // ruh roh
	}

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction(rand.nextFloat() * 2 * (float)Math.PI);
    }
}
