package group13.broadcast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdditionTest {
    @Test
    void threePlusTwoEqualsFive () {
        Addition adder = new Addition();
        assertEquals(5, adder.add(3, 2));
    }
}