package gridwars.starter;

import cern.ais.gridwars.Emulator;


/**
 * Instantiates the example bots and starts the game emulator.
 */
public class EmulatorRunner {

    public static void main(String[] args) {
        Emulator.playMatch(new GluttonBot(), new GluttonBot());
        Emulator.playMatch(new GluttonBot(), new iFABot());
    }
}
