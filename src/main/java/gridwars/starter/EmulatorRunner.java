package gridwars.starter;

import cern.ais.gridwars.Emulator;


/**
 * Instantiates the example bots and starts the game emulator.
 */
public class EmulatorRunner {

    public static void main(String[] args) {
//        Emulator.playMatch(new ExpandBot(), new SquirrelBot());
        Emulator.playMatch(new GluttonBot(), new GluttonBot_WorkingGravity6());
//        Emulator.playMatch(new GluttonBot_WorkingGravity5(), new GluttonBot());
    }
}
