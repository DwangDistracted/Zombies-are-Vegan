package engine;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import assets.Potato_Mine;
import assets.Jalapeno;
import assets.EconomyPlant;
import assets.Plant;
import assets.PlantTypes;
import assets.Zombie;
import assets.ZombieTypes;
import levels.LevelInfo;
import util.Logger;

/**
 * The Primary Game Loop. Instance per level
 * @author David Wang
 */
public class Game implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public enum GameState {
		PLAYING,
		WON,
		LOST
	}
	
	private static Logger LOG = new Logger("Game");
	
	//The Level this game is playing
	private LevelInfo levelInfo;
	
	//The Game Board
	private Board board;
	
	//The Player's Purse
	private Purse userResources;
	
	//The Zombies that have not yet spawned into the game
	private HashMap<ZombieTypes, Integer> zombieQueue;
	
	//The number of zombies (total) in the level
	private int numZombies;
	
	//The number of turns elapsed
	private int numTurns;
	
	//The zombies that are to be removed
	private List<Zombie> zomRemoveBin;

	private GameState gamestate;

	private CommandQueue cQ;
	
	private transient List<GameListener> listeners;
	
	/**
	 * Initializes a Game for a given Level
	 * @param lvl the LevelInfo for the given Level
	 */
	public Game(LevelInfo lvl) {
		//set up config from level config
		board = new Board(lvl.getRows(), lvl.getColumns());
		levelInfo = lvl;
		
		zomRemoveBin = new LinkedList<Zombie>();
		zombieQueue = (HashMap<ZombieTypes, Integer>) lvl.getZombies();
		numZombies = zombieQueue.values().stream().mapToInt(Integer::intValue).sum();
		LOG.debug("Level has " + numZombies + " zombies");
		userResources = new Purse(levelInfo.getInitResources());
		gamestate = GameState.PLAYING;
		numTurns = 0;
		listeners = new ArrayList<>();
		cQ = new CommandQueue(this, listeners);
	}
	
	public void addListener(GameListener gl) {
		listeners.add(gl);
		cQ.setGameListeners(listeners);
	}
	
	/**
	 * Processes a Player's Turn
	 */
	public void playerTurn() {
		//plants action
		List<Plant> plantsInGame = board.getPlantsInGame();
		List<Plant> plantsToRemove = new ArrayList<>(); //holds the plants to that should be removed (mines and jalapenos)
		LOG.debug("Doing Plant Attack Calculations");
		
		for (Plant plant : plantsInGame) {
			LOG.debug("Plant at (" + plant.getRow() + "," + plant.getCol() + ")");
			plant.attack(board);
			
			if (plant.getPlantType() == PlantTypes.POTATOMINE) {
				if (((Potato_Mine)plant).getDischarged()) {
					plantsToRemove.add(plant);
				}
			}
			else if(plant.getPlantType() == PlantTypes.JALAPENO){ //else if plant is jalapeno
				if(((Jalapeno)plant).getDischarged()){
					plantsToRemove.add(plant);
				}
			}
		}
		
		for (Plant plant : plantsToRemove) {
			board.removePlant(plant.getRow(), plant.getCol());
		}
	}
	
	/**
	 * Processes the Zombie's Turn.
	 * @author David Wang; Modified by Derek Shao
	 */
	private void zombieTurn() {
		LOG.debug("It is the zombie's turn.");
		
		//create a new collection to prevent concurrent modification of Board zombies attribute
		List<Zombie> zombiesInGame = new LinkedList<Zombie>(board.getZombiesInGame());
		List<Zombie> zombiesToRemove = new ArrayList<>();
		
		Iterator<Zombie> iterator = zombiesInGame.iterator();
		
		while (iterator.hasNext()) {
			Zombie nextZombie = iterator.next();
			if(!getZomRemoveBin().contains(nextZombie))
			{
				//if a zombie has failed to move, it means it is being blocked by a Plant
				if (!nextZombie.move()) {
					nextZombie.attack(board);
					if(nextZombie.getZombieType() == ZombieTypes.EXP_ZOMBIE){ //if a exploding zombie attacks, it instantly dies
						zombiesToRemove.add(nextZombie);
					}
				}
				int row = nextZombie.getRow();
				if(board.hasReachedEnd(row) && board.isMowerAvaliable(row))
				{
					cQ.registerMow(row); // Keep track of the lawn mowers used
					setZomRemoveBin(board.useLawnMower(row)); //use lawnmower
					for(GameListener g : listeners)
					{
						g.updateMower(row, board.isMowerAvaliable(row)); //update lawn mower image
					}
					board.removeMower(row); 
					board.resetZombieReachedEnd(row);
				}
				else if(board.hasReachedEnd(row) && !board.isMowerAvaliable(row)) {
					// a zombie has reached the end of the board and a lawnmower is not available. player loses
					endGame(false);
					break;
				}
			}
		}
		
		zomRemoveBin.clear(); //clearing the zombie remove bin (not needed anymore)
		
		for (Zombie z : zombiesToRemove) { //remove all exploding zombies that attacked
			board.removeZombie(z.getRow(), z.getCol());
		}
		
		//spawn new zombies
		if (!zombieQueue.isEmpty()) { //there must be zombies to spawn
			Random rand = new Random();
			int zombiesToSpawn = rand.nextInt(numZombies/4 == 0? 2: numZombies/4); //if there aren't enough zombies then spawn up to 1
			
			if (zombiesToSpawn > zombieQueue.values().stream().mapToInt(Integer::intValue).sum()) { 
				//if the random number is larger than the reamining zombies then spawn all remaining zombies
				zombiesToSpawn = zombieQueue.values().stream().mapToInt(Integer::intValue).sum();
			}
			
			LOG.debug("Spawning " + zombiesToSpawn + " zombies");
			
			for(int i = 0; i < zombiesToSpawn; i++)  //spawn zombies
			{
				//determine type of zombie spawn
				List<ZombieTypes> keys = new ArrayList<ZombieTypes>(zombieQueue.keySet());
				ZombieTypes type = keys.get(rand.nextInt(keys.size()));

				LOG.debug("Spawning a " + type.toString());
				
				int rowNumber = rand.nextInt(levelInfo.getRows()); //determines which row the zombie will go down
				Zombie zombie = ZombieTypes.toZombie(type);
				zombie.setListener(board);
				board.placeZombie(zombie, rowNumber, levelInfo.getColumns() - 1); //spawn the zombie
				zombie.setRow(rowNumber);
				zombie.setColumn(levelInfo.getColumns() - 1);
				
				//removes the spawned zombie from the Queue
				int x = zombieQueue.get(type) - 1;
				if (x == 0) {
					zombieQueue.remove(type);
				} else {
					zombieQueue.put(type, x);
				}
			}
		} else {
			LOG.debug("No More Zombies to Spawn");
		}
	}
	
	/**
	 * Tells Combat Engine to handle attack and damage calculations. Adds Resources to Player Purse. Checks if the pLayer has won
	 */
	public void doEndOfTurn() {
		cQ.registerEndTurn(board);
		playerTurn(); //player plants attack
		numTurns++;
		//do the zombie Turn
		zombieTurn();
		
		//economy calculations
		userResources.addPoints(levelInfo.getResPerTurn()); //do default sunshine gain
		for(EconomyPlant p : board.getEconomyPlantsInGame()){ //do economy plants sunshine gain
			userResources.addPoints(p.getPoints());
		}
		
		//did player win?
		if (zombieQueue.values().stream().mapToInt(Integer::intValue).sum() == 0 && board.getNumberOfZombies() == 0) {
			endGame(true);
		}
		
		for (GameListener gl : listeners) {
			gl.updateAllGrids();
			gl.updateEndTurn();
		}
	}
	
	/**
	 * Ends the Game
	 * @param playerWin True if the player won, false otherwise
	 */
	private void endGame(boolean playerWin) {
		if(playerWin) {
			LOG.debug("Player has Won");
			gamestate = GameState.WON;
		} else {
			LOG.debug("Player was eaten by Zombies");
			gamestate = GameState.LOST;
		}
	}
	
	/**
	 * Sets the zombie remove bin.All the zombies within this list
	 * will be removed from the board
	 * @param zom - a list of zombies to be removed from the board
	 */
	public void setZomRemoveBin(List<Zombie> zom)
	{
		zomRemoveBin.addAll(zom);
	}
	/**
	 * Retreives the zombie remove bin
	 * @return - a list of zombies 
	 */
	public List<Zombie> getZomRemoveBin()
	{
		return zomRemoveBin;
	}
	 
	 /**
	  * Get the LevelInfo 
	  * 
	  * @return the level info
	  */
	 public LevelInfo getLevelInfo() {
		 
		 return this.levelInfo;
	 }
	 
	 /**
	  * Get the Board 
	  * 
	  * @return the board
	  */
	 public Board getBoard() {
		 return this.board;
	 }
	 
	 /**
	  * Get the Purse
	  * 
	  * @return the purse
	  */
	 public Purse getPurse() {
		 
		 return this.userResources;
	 }
	 
	 /**
	  * Get the command queue
	  * 
	  * @return the command queue
	  */
	 public CommandQueue getCommandQueue() {
		 
		 return this.cQ;
	 }
	 
	 /**
	  * Returns the current state of the game; Wom, lost, playing
	  * @return
	  */
	 public GameState getState() {
		 return gamestate;
	 }

	 /**
	  * Returns the number of turns elapsed since the start of the turns
	  * @return
	  */
	public int getTurns() {
		return numTurns;
	}
	
	/**
	 * Decrements the number of turns by 1. Used by the undo end turn function
	 */
	public void decrementTurns() {
		this.numTurns--;
	}

	/**
	 * Increments the number of turns by 1. Used by the redo end turn function
	 */
	public void incrementTurns() {
		this.numTurns++;
	}
	
	/**
	 * Plants a plant in a given position.
	 * @param type
	 * @param x
	 * @param y
	 */
	public void placePlant(PlantTypes type, int x, int y) {
		Plant selectedPlant = PlantTypes.toPlant(type);
		if (userResources.canSpend(selectedPlant.getCost())) {
			if (board.placePlant(selectedPlant, x, y)) {
				cQ.registerPlace(type,x,y);
				userResources.spendPoints(selectedPlant.getCost());
				
				for (GameListener gl : listeners) {
					gl.updateGrid(x, y);
					gl.updatePurse();
				}
			}
		} else {
			for (GameListener gl : listeners) {
				gl.updateMessage("Not Enough Points", "You do not have enough funds for: " + selectedPlant.toString());
			}
		}
	}
	
	/**
	 * Removes a Plant from a given position
	 * @param x
	 * @param y
	 */
	public void removePlant(int x, int y) {
		cQ.registerDig(board.getPlant(x, y).getPlantType(),x,y);
		board.removePlant(x, y);
		for (GameListener gl : listeners) {
			gl.updateGrid(x, y);
		}
	}

	/**
	 * Undos the last move
	 */
	public void undo() {
		if (!cQ.undo()) {
			for (GameListener gl : listeners) {
				gl.updateMessage("Cannot Undo", "No more moves to Undo");
			}
		}
	}
	
	/**
	 * Redos the previously undone move
	 */
	public void redo() {
		if (!cQ.redo()) {
			for (GameListener gl : listeners) {
				gl.updateMessage("Cannot Redo", "No more moves to Redo");
			}
		}
	}
	
	/**
	 * Reiniialize transient variables that were not serialized
	 */
	public void reImplementTransientFields() {
		
		this.listeners = new ArrayList<GameListener>();
	}
}
