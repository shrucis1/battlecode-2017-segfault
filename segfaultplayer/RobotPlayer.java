package segfaultplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
	
    
    
    public static void run(RobotController rc) throws GameActionException {
    	
        int thisID = RobotBase.getAndAssignNextID(rc);
        
        switch (rc.getType()) {
            case ARCHON:
                handleArchon(rc, thisID);
                break;
            case GARDENER:
                handleGardener(rc, thisID);
                break;
            case SOLDIER:
                handleSoldier(rc, thisID);
                break;
            case LUMBERJACK:
                handleLumberjack(rc, thisID);
                break;
            case SCOUT:
            	handleScout(rc, thisID);
            	break;
            default:
            	break;
        }
	}
    
    public static void handleArchon(RobotController rc, int id) throws GameActionException {
    	Archon a = new Archon(rc, id);
    	a.run();
    }
    
    public static void handleGardener(RobotController rc, int id) throws GameActionException {
    	Gardener g = new Gardener(rc, id);
    	g.run();
    }
    
    public static void handleSoldier(RobotController rc, int id) throws GameActionException {
    	Soldier so = new Soldier(rc, id);
    	so.run();
    }
    
    public static void handleLumberjack(RobotController rc, int id) throws GameActionException {
    	Lumberjack lj = new Lumberjack(rc, id);
    	lj.run();
    }
    
    public static void handleScout(RobotController rc, int id) throws GameActionException {
    	Scout sc = new Scout(rc, id);
    	sc.run();
    }
    
}