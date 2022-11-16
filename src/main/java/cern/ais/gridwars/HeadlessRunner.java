package cern.ais.gridwars;

import cern.ais.gridwars.api.bot.PlayerBot;

import java.util.Arrays;
import java.util.List;

public class HeadlessRunner {

    public static int runGame(PlayerBot blueBot, PlayerBot redBot, String blueBotLog, String redBotLog)
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
            return 0;
        }
        if(player.getId() == 0)
        {
            return 1;
        }
        return -1;
    }
}
