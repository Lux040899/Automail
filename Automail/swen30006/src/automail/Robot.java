//Project team: 88 
package automail;

import exceptions.BreakingFragileItemException;
import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;

import java.util.Map;
import java.util.TreeMap;

/**
 * The robot delivers mail!
 */
public class Robot {
	
    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;

    IMailDelivery delivery;
    protected final String id;
    /** Possible states the robot can be in */
    public enum RobotState { DELIVERING, WAITING, RETURNING, WRAPPING, UNWRAPPING }
    public RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private IMailPool mailPool;
    private boolean receivedDispatch;
    
    private MailItem deliveryItem = null;
    private MailItem tube = null;
    private MailItem fragItem = null;
    
    private int deliveryCounter;
    private int fragCounter;

    private boolean cautionMode;
    private boolean fragDelivery;
    private boolean isWrapped;

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public Robot(IMailDelivery delivery, IMailPool mailPool){
    	id = "R" + hashCode();
        // current_state = RobotState.WAITING;
    	current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
        this.fragCounter = 0;
        this.cautionMode = false;
        this.fragDelivery = false;
        this.isWrapped = false;
    }
    
    public void dispatch() {
    	receivedDispatch = true;
    }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException 
    {
        switch(current_state) 
        {
            case UNWRAPPING:
                isWrapped = false;
                delivery.deliver(this.fragItem);
                fragDelivery = false;
                fragItem = null;
                fragCounter++;
                Building.floorAvailable.put(current_floor, true);
                cautionMode = false;
                nextMove();
                break;
            case WRAPPING:
                if(!isWrapped)
                {
                    isWrapped = true;
                }
                else
                {
                    receivedDispatch = false;
                    deliveryCounter = 0; // reset delivery counter
                    fragCounter = 0;
                    setInitRoute();
                    changeState(RobotState.DELIVERING);
                }
                break;
    		/** This state is triggered when the robot is returning to the mailroom after a delivery */
    		case RETURNING:
    			/** If its current position is at the mailroom, then the robot should change state */
                if(current_floor == Building.MAILROOM_LOCATION)
                {
                    if (tube != null) 
                    {
                		mailPool.addToPool(tube);
                        System.out.printf("T: %3d >  +addToPool [%s]%n", Clock.Time(), tube.toString());
                        tube = null;
                	}
        			/** Tell the sorter the robot is ready */
        			mailPool.registerWaiting(this);
                	changeState(RobotState.WAITING);
                } 
                else 
                {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                }
                break;
    		case WAITING:
                /** If the Robot has recieved the items to deliver and is waiting in the mailroom then start the delivery */
                if(!isEmpty() && receivedDispatch)
                {
                    //Check if Robot has received a fragile item, if yes begin wrapping
                    if(this.fragItem != null && !isWrapped)
                    {
                        changeState(RobotState.WRAPPING);
                        break;
                    }

                    // If Robot does not receive a fragile item, dispatch and change state to DELIVERING instantly.
                    receivedDispatch = false;
                    deliveryCounter = 0; // reset delivery counter
                    fragCounter = 0;
                    setInitRoute();
                    changeState(RobotState.DELIVERING);
                }
                break;
    		case DELIVERING:
                if(current_floor == destination_floor)
                { // If already here drop off either way
                    /** Delivery complete, report this to the simulator! */
                    if(!fragDelivery)
                    {
                        if(current_floor == Building.MAILROOM_LOCATION
                           && !Building.floorAvailable.get(current_floor))
                        {
                            break;
                        }
                        delivery.deliver(deliveryItem);
                        deliveryItem = null;
                        deliveryCounter++;
                    }
                    else
                    {
                        if(isWrapped)
                        {
                            if(current_floor == Building.MAILROOM_LOCATION
                               || Building.floorCount.get(current_floor) == 1)
                            {
                                changeState(RobotState.UNWRAPPING);
                            }
                            
                            break;
                        }
                    }
                    if(deliveryCounter > 2 || fragCounter > 1)
                    {  // Implies a simulation bug
                    	throw new ExcessiveDeliveryException();
                    }
                    nextMove();
                } 
                else 
                {
	        		/** The robot is not at the destination yet, move towards it! */
                    moveTowards(destination_floor);

                    /**Checks whether the robot has reached the destination floor for its fragile delivery
                     * If so, puts the floor on lockdown. Makes availablity of floor false.
                     * */
                    if(fragDelivery && current_floor == destination_floor)
                    {
                        Building.floorAvailable.put(current_floor, false);
                    }
    			}
                break;
    	}
    }

    private void nextMove()
    {
        /** Check if want to return, i.e. if there is no item remaining to deliver*/
        if(isEmpty())
        {
        	changeState(RobotState.RETURNING);
        }
        else
        {
            /** If there is another item, set the robot's route to the location to deliver the item */
            if(deliveryItem == null && tube != null)
            {
                deliveryItem = tube;
                tube = null;
            }
            setRoute();
            changeState(RobotState.DELIVERING);
        }
    }

    /**
     * Sets the route for the robot
     */
    private void setInitRoute() 
    {
        /** Set the destination floor */
        if(this.deliveryItem == null 
           || (this.fragItem != null
               && this.fragItem.getDestFloor() >= this.deliveryItem.getDestFloor()))
        {
            setFragDelivery();
        }
        else
        {
            setDelivery();
        }
    }

    private void setRoute()
    {
        //Setting the route and destination of delivery based on furthest distance 
        if(this.deliveryItem == null
           || (this.fragItem != null
               && this.fragItem.getDestFloor() - current_floor <= this.deliveryItem.getDestFloor() - current_floor))
        {
            setFragDelivery();
        }
        else
        {
            setDelivery();
        }
    }

    private void setFragDelivery()
    {
        this.destination_floor = fragItem.getDestFloor();
        this.fragDelivery = true;
        if(destination_floor == Building.MAILROOM_LOCATION)
        {
            Building.floorAvailable.put(Building.MAILROOM_LOCATION, false);
        }
    }

    private void setDelivery()
    {
        this.destination_floor = deliveryItem.getDestFloor();
        this.fragDelivery = false;
    }

    /**
     * Generic function that moves the robot towards the destination
     * @param destination the floor towards which the robot is moving
     */
    private void moveTowards(int destination) 
    {
        int pseudo_current_floor = current_floor;

        //Deciding whether to move up or down the building based on the delivery destination
        if(pseudo_current_floor < destination)
        {
            pseudo_current_floor++;
        } 
        else 
        {
            pseudo_current_floor--;
        }

        //Robot moves to the desired floor only if the floor is not being used for a fragile delivery
        if(Building.floorAvailable.get(pseudo_current_floor)) 
        {
            Building.floorCount.put(current_floor, Building.floorCount.get(current_floor) - 1);
            current_floor = pseudo_current_floor;
            Building.floorCount.put(current_floor, Building.floorCount.get(current_floor) + 1);
        }
    }

    private String getIdTube() 
    {
    	return String.format("%s(%1d)", id, (tube == null ? 0 : 1));
    }
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(RobotState nextState){
    	assert(!(deliveryItem == null && tube != null));
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
        current_state = nextState;

        // Printing items which are to be delivered by robot
    	if(nextState == RobotState.DELIVERING){
            if(deliveryItem != null)
            {
                System.out.printf("T: %3d > %9s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
            }

            if(fragItem != null)
            {
                System.out.printf("T: %3d > %9s-> [%s]%n", Clock.Time(), getIdTube(), fragItem.toString());
            }
    	}
    }

	public MailItem getTube() {
		return tube;
	}
    
	static private int count = 0;
	static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

	@Override
	public int hashCode() {
		Integer hash0 = super.hashCode();
		Integer hash = hashMap.get(hash0);
		if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
		return hash;
	}

	public boolean isEmpty() {
		return (deliveryItem == null && tube == null && fragItem == null);
	}

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException, BreakingFragileItemException {
		assert(deliveryItem == null);
		if(mailItem.fragile) throw new BreakingFragileItemException();
		deliveryItem = mailItem;
		if (deliveryItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}

	public void addToTube(MailItem mailItem) throws ItemTooHeavyException, BreakingFragileItemException {
		assert(tube == null);
		if(mailItem.fragile) throw new BreakingFragileItemException();
		tube = mailItem;
		if (tube.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
    }

    public void addToFragArm(MailItem mailItem) throws ItemTooHeavyException, BreakingFragileItemException {
        assert(fragItem == null);
        assert(mailItem.fragile);
        cautionMode=true; //Enabling caution mode
        fragItem = mailItem;
        if (fragItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
    }

    
    public int getDeliveryCounter()
    {
        return this.deliveryCounter;
    }

    public int getFragCounter()
    {
        return this.fragCounter;
    }

    public boolean isCaution()
    {
        return this.cautionMode;
    }
    public boolean isSpecialArmEmpty(){
        return this.fragItem == null;
    }
    public boolean isHandsEmpty(){
        return this.deliveryItem == null;
    }
}
