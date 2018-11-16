package unittests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import engine.*;
import assets.*;

import org.junit.jupiter.api.Test;

public class CombatTest {
	
	@Test
	public void testPlantAttack() {
		Combat c = new Combat(new Grid[1][1]);
		Board b = new Board(7, 1);
		Peashooter p = new Peashooter();
		Regular_Zombie z = new Regular_Zombie();
		b.placePlant(p, 0, 0);
		b.placeZombie(z, 7, 0);
		c.plantAttack(p);
		assertTrue("Zombie is dead", z.isAlive());
	}
	
	@Test
	public void testZombieAttack() {
		Combat c = new Combat(new Grid[1][1]);
		Board b = new Board(7, 1);
		Peashooter p = new Peashooter();
		Regular_Zombie z = new Regular_Zombie();
		b.placePlant(p, 0, 1);
		b.placeZombie(z, 7, 1);
		assertTrue("True", c.zombieAttack(z, p));
	}
	
}
