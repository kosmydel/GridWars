package gridwars.starter;

import cern.ais.gridwars.Emulator;


/**
 * Instantiates the example bots and starts the game emulator.
 */
public class EmulatorRunner {

    public static void main(String[] args) {
//        Emulator.playMatch(new ExpandBot(), new SquirrelBot());
        Emulator.playMatch(new GluttonBotPRO5_T_tuned(), new GluttonBotPRO5_T_tuned());
//        Emulator.playMatch(new GluttonBot_WorkingGravity5(), new GluttonBot());
    }
}
