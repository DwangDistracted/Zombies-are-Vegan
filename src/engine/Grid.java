package engine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import assets.Plant;
import assets.Zombie;
import assets.ZombieTypes;
import util.Logger;
 
/**
 * Building block for a grid. Contains 1 Plant and N zombies.
 * 
 * @author Derek Shao
 *
 */
public class Grid implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = new Logger("Grid");
	private int row;
	private int col;
	
 	private Plant plant;
 	
 	// used to determine which zombie arrived on the grid first and to attack first by plant
	private Queue<Zombie> zombies;
	
	// used to keep track of the zombie types and the number of zombies present in grid
	private HashMap<ZombieTypes, Integer> zombieTypeCount;
	
	public Grid(int row, int col) {
		this.row = row;
		this.col = col;
		
		zombies = new LinkedList<Zombie>();
		zombieTypeCount = new HashMap<ZombieTypes, Integer>();
	}
	
	/**
	 * Creates a Grid Object as a deep copy of another grid
	 * @param grid
	 * @author David Wang
	 */
	public Grid(Grid grid) {
		this.row = grid.row;
		this.col = grid.col;
		this.plant = grid.plant;
		this.zombies = new LinkedList<Zombie>();
		this.zombies.addAll(grid.zombies);
		this.zombieTypeCount = new HashMap<ZombieTypes, Integer>();
		this.zombieTypeCount.putAll(grid.zombieTypeCount);
	}

	/**
	 * Determines if the current grid is occupied by a plant.
	 * 
	 * @return true if occupied, false otherwise
	 */
	public boolean isOccupied() {
		
		return plant != null;
	}
	
	/**
	 * Set the current grid's plant
	 * 
	 * @param newPlant
	 * @return true if setting plant was successful, false otherwise
	 */
	public boolean setPlant(Plant newPlant) {
		
		if (plant == null) {
			plant = newPlant;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns the plant occupying this grid
	 * 
	 * @return the plant on this grid
	 */
	public Plant getPlant() {
		
		return plant;
	}
	
	/**
	 * Clear the plant on this grid 
	 *
	 */
	public void removePlant() {
		
		plant = null;
	}
	
	/**
	 * Add a zombie to this grid
	 * 
	 * @param zombie
	 * @return true if zombie was added successfully, false otherwise
	 */
	public boolean addZombie(Zombie zombie) {
		if (zombies.add(zombie)) {
		
			zombieTypeCount.put(zombie.getZombieType(), zombieTypeCount.getOrDefault(zombie.getZombieType(), 0) + 1);
			
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * Get the first zombie that arrived on this grid
	 * 
	 * @return the first zombie on this grid, null if no zombies present
	 */
	public Zombie getFirstZombie() {
		
		return zombies.peek();
	}
	
	/**
	 * Remove the first zombie on this grid
	 * Likely used when a zombie was killed
	 * 
	 * @return the zombie that was killed, null if no zombies are present
	 */
	public Zombie removeZombie() {
		if (!zombies.isEmpty()) {
			
			Zombie zombieToRemove = zombies.peek();
			
			if (zombieTypeCount.get(zombieToRemove.getZombieType()) - 1 == 0) {
				zombieTypeCount.remove(zombieToRemove.getZombieType());
			}
			
			return zombies.poll();
		}
		return null;
	}
	
	
	/**
	 * Get the number of zombies in the current grid.
	 * 
	 * @return number of zombies
	 */
	public int getNumberOfZombies() {
		
		return zombies.size();
	}
	
	/**
	 * Get the queue of zombies placed on this grid.
	 * 
	 * @return the queue of zombies in this grid
	 * 
	 */
	public Queue<Zombie> getZombies() {
		
		return zombies;
	}
	
	/**
	 * Get the map of zombie types and its count on this grid
	 * 
	 * @return zombie type and count 
	 */
	public HashMap<ZombieTypes, Integer> getZombieTypeCount() {
		
		return zombieTypeCount;
	}
	
	
	public void updateZombieTypeCount() {
		zombieTypeCount = new HashMap<ZombieTypes, Integer>();
		for (Zombie zombie : zombies) {
			zombieTypeCount.put(zombie.getZombieType(), zombieTypeCount.getOrDefault(zombie.getZombieType(), 0) + 1);
		} 
	}
	
	/**
	 * Get the row this grid is located at
	 * 
	 * @return the row of this grid
	 */
	public int getRow() {
		return row;
	}

	/**
	 * Get the column this grid is located at
	 * 
	 * @return the column of this grid
	 */
	public int getCol() {
		return col;
	}
	
	/**
	 * Get the coordinates that this grid is located at.
	 * index 0 respresents the row, and index 1 respresents the column of the grid
	 * @return the coordinates of this grid
	 */
	public int[] getCoordinates() {
		int[] coord = {row,col};
		return coord;
	}
}