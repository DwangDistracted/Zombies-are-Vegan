package assets;

import java.io.Serializable;

/**
 * Class for Juking Zombie type that can change rows 
 * when moving towards plants.
 * 
 * @author Michael Patsula
 *
 */
public class Juking_Zombie extends Zombie implements Serializable {
	
	private static final int DEFAULT_SPEED = SPEED_LOW;
	private static final int DEFAULT_POWER = ATTACK_LOW;
	private static final int DEFAULT_HP = HEALTH_HIGH;
	private static final ZombieTypes ZOMBIE_TYPE = ZombieTypes.JUK_ZOMBIE;
	private boolean toggleDirection; //true indicates incrementing row, false indicates decreaseing row
	
	
	public Juking_Zombie()	{
		super(DEFAULT_SPEED, DEFAULT_POWER, DEFAULT_HP);
		toggleDirection = true;
	}
	
	/**
	 * returns the name of regular type zombie
	 */
	@Override 
	public String toString() {
		
		return ZOMBIE_TYPE.toString();
	}
	
	/**
	 * Determines the new row of the zombie on for its next move
	 * @param maxRow - The maximum row number within the board
	 * @return -> the new row number 
	 */
	public int getPath(int maxRow)
	{
		if(this.getRow() == maxRow - 1)
		{
			toggleDirection = false;
			return maxRow - 2;
		}
		else if(this.getRow() == 0)
		{
			toggleDirection = true;
			return 1;
		}
		else if(toggleDirection == true)
		{
			return this.getRow() + 1;
		}
		
		//indicates toggleDirection == false
		return this.getRow() - 1;
	}
	public ZombieTypes getZombieType() {
		return ZOMBIE_TYPE;
	}

	@Override
	public int getDefaultSpeed() {
		return DEFAULT_SPEED;
	} 

}
