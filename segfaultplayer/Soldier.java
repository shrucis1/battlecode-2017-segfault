package segfaultplayer;
import battlecode.common.*;

public strictfp class Soldier extends RobotBase
{
	public float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
	public float curdirection = (float) Math.random() * 2 * (float) Math.PI;
	public int ctr = 0;
	
	public Soldier(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		//TODO: Identify closest archon

		try {
			while(true) {
				dailyTasks();
				decideMove();
				decideShoot();
				Clock.yield();
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Soldier Error");
		}
	}

	//daily non-movement/shooting tasks
	public void dailyTasks() throws  GameActionException {
		checkVPWin(); //check if can win game on VPs
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees(); //shake nearby bullet trees
		for(int i=0; i<nearbyTrees.length; i++) {
			if(nearbyTrees[i].getContainedBullets() > 0 && rc.canShake(nearbyTrees[i].getID())) {
				rc.shake(nearbyTrees[i].getID());
				break;
			}
		}
		if(ctr<enemyArchons.length && rc.getLocation().distanceTo(enemyArchons[ctr])<4 && isArchonDead()) //if archon is dead, move to next one
			ctr++;
	}

	//is the enemy archon here dead?
	public boolean isArchonDead() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
		for(int i=0; i<robots.length; i++)
			if(robots[i].getType() == RobotType.ARCHON)
				return false;
		return true;
	}

	//determines movement for the turn
	public void decideMove() throws GameActionException {
		BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
		MapLocation goal;
		if(robots.length>0) //if nearby units, move towards them
			goal = robots[0].getLocation();
		else if(ctr<enemyArchons.length) //elif archons are alive, move towards them
			goal = enemyArchons[ctr];
		else { //move randomly
			if (Math.random() < 0.05)
				curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
			curdirection += curdiff + 2 * (float) Math.PI;
			while (curdirection > 2 * (float) Math.PI)
				curdirection -= 2 * (float) Math.PI;
			goal = rc.getLocation().add(new Direction(curdirection),rc.getType().strideRadius);
		}
		if(nearbyBullets.length>0) //if there are bullets, dodge
			moveWithDodging(goal); //TODO: check to make sure not crowded in by trees
		else //move normally
			pathFind(goal); //TODO: Make method work + change to better get ML
	}

	//determines shooting for the turn
	public void decideShoot() throws GameActionException {
		if(ctr>=enemyArchons.length)
			shoot(null);
		else
			shoot(rc.getLocation().directionTo(enemyArchons[ctr]));
	}

	public void shoot(Direction goal) throws GameActionException {
		shoot(goal, false);
	}

	//Does fire action
	public void shoot(Direction goal, boolean debug) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius);
		if (robots.length > 0) { //there are nearby robots
			//TODO?: Make more efficient? Isn't an issue right now
			RobotType[] priority = {RobotType.SOLDIER, RobotType.TANK, RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.SCOUT, RobotType.ARCHON}; //priority of shooting
			RobotInfo target = null;
			int z = 0;
			while (target == null && (z<priority.length && rc.getRoundNum()>300 || z<priority.length-1)) {
				for (int i = 0; i < robots.length; i++) {
					if (robots[i].getType() == priority[z] && isSingleShotClear(rc.getLocation().directionTo(robots[i].getLocation()))) {
						target = robots[i];
						break;
					}
				}
				z++;
			}
			if (target != null) { //shooting
				Direction tDir = rc.getLocation().directionTo(target.getLocation());
				double[] vPentad = isPentadShotClear(tDir);
				double[] vTriad = isTriadShotClear(tDir);
				if(debug) {
					System.out.println("vPentad: "+vPentad[0]+" "+vPentad[1]);
					System.out.println("vTriad: "+vTriad[0]+" "+vTriad[1]);
				}
				if (rc.canFirePentadShot() && vPentad[1] > vPentad[0]) //does penta do more enemy dmg
					rc.firePentadShot(tDir);
				else if (rc.canFireTriadShot() && vTriad[0] == 0) //is triad safe
					rc.fireTriadShot(tDir);
				else if (rc.canFireSingleShot() && isSingleShotClear(tDir))
					rc.fireSingleShot(tDir);
			}
		}
		else if(rc.getRoundNum()>400 && rc.getTeamBullets()>200 && goal!=null && trees.length>0 && trees[0].getTeam()!=ally) { //are there nearby non-ally trees
			Direction tDir = rc.getLocation().directionTo(trees[0].getLocation());
			if( tDir.equals(goal,(float)(Math.PI/4.0)) && rc.getLocation().distanceTo(trees[0].getLocation())<3.0) { //are they in our way
				if (rc.canFireTriadShot()) //shoot em down
					rc.fireTriadShot(tDir);
				else if (rc.canFireSingleShot())
					rc.fireSingleShot(tDir);
			}
		}
	}
}