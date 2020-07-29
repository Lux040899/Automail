// Project Team: 88
package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.MailItem;
import automail.Robot;
import exceptions.BreakingFragileItemException;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {

    private class Item {
        int destination;
        MailItem mailItem;
        
        public Item(MailItem mailItem) {
            destination = mailItem.getDestFloor();
            this.mailItem = mailItem;
        }
    }
    
    public class ItemComparator implements Comparator<Item> {
        @Override
        public int compare(Item i1, Item i2) {
            int order = 0;
            if (i1.destination > i2.destination) {  // Further before closer
                order = 1;
            } else if (i1.destination < i2.destination) {
                order = -1;
            }
            return order;
        }
    }
    
    private LinkedList<Item> pool;
    private LinkedList<Robot> robots;

    public MailPool(int nrobots){
        // Start empty
        pool = new LinkedList<Item>();
        robots = new LinkedList<Robot>();
    }

    public void addToPool(MailItem mailItem) {
        Item item = new Item(mailItem);
        pool.add(item);
        pool.sort(new ItemComparator());
    }
    
    @Override
    public void step(boolean CAUTION_ENABLED) throws ItemTooHeavyException, BreakingFragileItemException {
        try{
            ListIterator<Robot> i = robots.listIterator();
            while (i.hasNext()) loadRobot(i, CAUTION_ENABLED);
        } catch (Exception e) { 
            throw e; 
        } 
    }
    
    private void loadRobot(ListIterator<Robot> i, boolean CAUTION_ENABLED) throws ItemTooHeavyException, BreakingFragileItemException {
        Robot robot = i.next();
        assert(robot.isEmpty());
        MailItem temp_item;
        // System.out.printf("P: %3d%n", pool.size());
        ListIterator<Item> j = pool.listIterator();

        // loads a robot if there are items left to be delivered in the mail pool
		if(pool.size()>0)
		{
			try 
			{
                //iterates through the mailpool items 
				while(pool.size() > 0)
				{
	                temp_item=j.next().mailItem;  
					if(temp_item.isFragile())
					{ // adding item to fragArm if item is fragile and empty and caution mode is enabled
                        if(!CAUTION_ENABLED)
                        {
                            throw new BreakingFragileItemException();
                        }   
                        else if(robot.isSpecialArmEmpty()){
		               	    robot.addToFragArm(temp_item);
		               	    j.remove();
		               	}
						else
						{
		               	    break; // if item can not be held, exit loop and dispatch
		               	}
	                }
					else
					{
						if(robot.isHandsEmpty())
						{
	                        robot.addToHand(temp_item); // hand first, if item is not fragile as we want higher priority delivered first
	                        j.remove();
	                    }
						else if(robot.getTube()==null)
						{
	                        robot.addToTube(temp_item); // if item is not fragile and hand is full, adds it to tube
	                        j.remove();
	                    }
						else
						{
	                        break; // if item can not be held, exit loop and dispatch
	                    }
	                }
	            }
	            robot.dispatch(); // send the robot off if it has any items to deliver
	            i.remove();       // remove from mailPool queue
			} 
			catch (Exception e) 
			{ 
                throw e; 
            }
    	} 
    }

    @Override
    public void registerWaiting(Robot robot) { // assumes won't be there already
        robots.add(robot);
    }

}


