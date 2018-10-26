package engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import assets.Plant;
import assets.Flower;
import assets.Zombie;
import assets.ZombieTypes;
import main.Main;
import levels.LevelInfo;
import util.Logger;

/**
 * The Primary Game Loop. Instance per level
 * @author david
 */
public class Game {
	private static Logger LOG = new Logger("Game");
	//combat engine
	private Combat combat; 
	//The Level this game is playing
	private LevelInfo levelInfo;
	//The Game Board
	private Board board;
	//The Player's Purse
	private Purse userResources;
	//Whether or not the Game has concluded
	private boolean finished = false;
	//The Zombies that have not yet spawned into the game
	private HashMap<ZombieTypes, Integer> zombieQueue;
	//The number of zombies (total) in the level
	private int numZombies;

	/**
	 * Initializes a Game for a given Level
	 * @param lvl the LevelInfo for the given Level
	 */
	public Game (LevelInfo lvl) {
		//set up config from level config
		board = new Board(lvl.getRows(), lvl.getColumns());
		levelInfo = lvl;
		combat = new Combat(board.getBoard());
		
		zombieQueue = (HashMap<ZombieTypes, Integer>) lvl.getZombies();
		numZombies = zombieQueue.values().stream().mapToInt(Integer::intValue).sum();
		userResources = new Purse(levelInfo.getInitResources());
	}
	
	/**
	 * Starts the Game Loop
	 */
	public void start() {
		while (!finished) {
			if (!playerTurn()) { break; }
			board.displayBoard();
			zombieTurn();		
			if (finished) { break; }
			board.displayBoard();
			doEndOfTurn();
			if (finished) { break; }
		}
	}
	
	/**
	 * Processes a Player's Turn
	 * @return True if the user did not quit. False otherwise
	 */
	private boolean playerTurn() {
		LOG.info("It is your turn. You have " + userResources.getPoints() + " sunshine.");
		LOG.prompt("Press 1 to skip this turn. Press 2 to quit this game.");
		String s = null;
		
		while (true) { //Handling User Input Block
			s = Main.sc.nextLine();
			//Parse User Commands
			if (s.equals("2")) {
				return false; 
			} else if (s.equals("1")) {
				LOG.debug("Skipping turn...");
				break;
			}
		}
		
		//plants action
		List<Plant> plantsInGame = board.getPlantsInGame();
		
		for (Plant plant : plantsInGame) {
			combat.plantAttack(plant);
		}
		
		return true;
	}
	
	/**
	 * Processes the Zombie's Turn.
	 */
	private void zombieTurn() {
		LOG.info("It is the zombie's turn.");
		
		//create a new collection to prevent concurrent modification of Board zombies attribute
		List<Zombie> zombiesInGame = new LinkedList<Zombie>(board.getZombiesInGame());
		Iterator<Zombie> iterator = zombiesInGame.iterator();
		
		while (iterator.hasNext()) {
			Zombie nextZombie = iterator.next();
			//if a zombie has failed to move, it means it is being blocked by a Plant
			if (!nextZombie.move()) {
				combat.zombieAttack(nextZombie, board.getPlant(nextZombie.getRow(), nextZombie.getCol() - 1));
			}

			if (board.hasReachedEnd()) {
				// a zombie has reached the end of the board and player loses
				endGame(false);
				break;
			}
		}
		
		//spawn new zombies
		if (!zombieQueue.isEmpty()) { //there must be zombies to spawn
			Random rand = new Random();
			int zombiesToSpawn = rand.nextInt(numZombies/4);
			
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
	private void doEndOfTurn() {
		
		//economy calculations
		userResources.addPoints(levelInfo.getResPerTurn()); //do default sunshine gain
		userResources.addPoints(board.getNumberOfSF() * Flower.getPoints()); //do sunflower/economy plants sunshine gain
		
		board.displayBoard();
		//did player win?
		if (zombieQueue.values().stream().mapToInt(Integer::intValue).sum() == 0 && board.getNumberOfZombies() == 0) {
			endGame(true);
		}
	}
	
	/**
	 * Ends the Game
	 * @param playerWin True if the player won, false otherwise
	 */
	private void endGame(boolean playerWin) {
		this.finished = true;
		if(playerWin) {
			LOG.info("Player has Won");
		} else {
			LOG.info("Player was eaten by Zombies");
		}
	}
}
