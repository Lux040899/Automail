//Project Team: 88
package automail;

import java.util.HashMap;

public class Building {
	
	
    /** The number of floors in the building **/
    public static int FLOORS;
    
    /** Represents the ground floor location */
    public static final int LOWEST_FLOOR = 1;
    
    /** Represents the mailroom location */
    public static final int MAILROOM_LOCATION = 1;

    /** Represents the Availability of each floor */
    public static HashMap<Integer, Boolean> floorAvailable = new HashMap<Integer, Boolean>();

    /** Represents the number of robots present on each floor */
    public static HashMap<Integer, Integer> floorCount = new HashMap<Integer, Integer>();
}
