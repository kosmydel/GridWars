package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.ArrayList;
import java.util.List;


/**
 * Simple bot that expands into all directions if there is a cell that does not belong to the bot
 */
public class GluttonBot2 implements PlayerBot {

    Coordinates startingPosition = null;

    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        List<Coordinates> myCells = universeView.getMyCells();

        if(startingPosition == null) {
            startingPosition = myCells.get(0);
        }

        for (Coordinates cell : myCells) {
            int currentPopulation = universeView.getPopulation(cell);

            List<MovementCommand.Direction> outerDirections = getOuterDirection(cell);

//            outerDirections = outerDirections.stream().filter(direction -> universeView.getPopulation(cell.getNeighbour(direction)) < 90).collect(Collectors.toList());

            if(currentPopulation / (outerDirections.size() + 1) >= 5) {
                for (MovementCommand.Direction direction : outerDirections) {
                    commandList.add(new MovementCommand(cell, direction, currentPopulation / (outerDirections.size() + 1)));
                }
            }
        }
    }

    public ArrayList<MovementCommand.Direction> getOuterDirection(Coordinates cell) {
        ArrayList<MovementCommand.Direction> outerDirection = new ArrayList<>();

        int start_x = startingPosition.getX();
        int start_y = startingPosition.getY();

        int x = cell.getX();
        int y = cell.getY();

        int v_x = x - start_x;
        int v_y = y - start_y;

        int distancex1 = Math.abs(v_x);
        int distancex2 = Math.abs(49 + v_x);
        int distancey1 = Math.abs(v_y);
        int distancey2 = Math.abs(49 + v_y);

        if(v_x == 0) {
            outerDirection.add(MovementCommand.Direction.LEFT);
            outerDirection.add(MovementCommand.Direction.RIGHT);
        } else {
            if(distancex1 > distancex2)
            {
                outerDirection.add(MovementCommand.Direction.RIGHT);
            }
            else
            {
                outerDirection.add(MovementCommand.Direction.LEFT);
            }
        }
        if (v_y == 0) {
            outerDirection.add(MovementCommand.Direction.DOWN);
            outerDirection.add(MovementCommand.Direction.UP);
        } else {
            if(distancey1 > distancey2)
            {
                outerDirection.add(MovementCommand.Direction.DOWN);
            }
            else
            {
                outerDirection.add(MovementCommand.Direction.UP);
            }
        }

        return outerDirection;
    }
}
