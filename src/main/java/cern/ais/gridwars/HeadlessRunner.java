package cern.ais.gridwars;

import cern.ais.gridwars.api.bot.PlayerBot;

import java.util.Arrays;
import java.util.List;

public class HeadlessRunner {

    public enum GAME_RESULT {
        RED_WON,
        BLUE_WON,
        DRAW
    }
    public static GAME_RESULT runGame(PlayerBot blueBot, PlayerBot redBot, String blueBotLog, String redBotLog)
    {
        List<Player> playerList = Arrays.asList(
                new Player(0, blueBot,new FileLogger(blueBotLog)),
                new Player(1, redBot, new FileLogger(redBotLog)));
        Game game = new Game(playerList, ((player, turn, binaryGameStatus) -> {}));
        game.startUp();
        while (!game.isFinished())
            game.nextTurn();
        //game.getUniverse()
        game.getWinner();
        Player player =  game.getWinner();
        if(player == null)
        {
            return GAME_RESULT.DRAW;
        }
        if(player.getId() == 0)
        {
            return GAME_RESULT.BLUE_WON;
        }
        return GAME_RESULT.RED_WON;
    }
}
