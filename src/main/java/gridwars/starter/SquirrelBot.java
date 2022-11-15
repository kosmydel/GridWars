package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Simple bot that expands into all directions if there is a cell that does not belong to the bot
 */
public class SquirrelBot implements PlayerBot {

    Coordinates startingPosition = null;

    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        List<Coordinates> myCells = universeView.getMyCells();

        if(startingPosition == null) {
            startingPosition = myCells.get(0);
        }

        for (Coordinates cell : myCells) {
            int currentPopulation = universeView.getPopulation(cell);
            currentPopulation -= 5;

            List<MovementCommand.Direction> outerDirections = getOuterDirection(cell);

            outerDirections = outerDirections.stream().filter(direction -> universeView.getPopulation(cell.getNeighbour(direction)) < 20).collect(Collectors.toList());


            if(currentPopulation / (outerDirections.size()) >= 5) {
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

        int distance1 = Math.abs(x - start_x) + Math.abs(y - start_y);
        int distance2 = Math.abs(x + 49 - start_x) + Math.abs(y + 49 - start_y);

        int v_x = x - start_x;
        int v_y = y - start_y;

        if(v_x == 0) {
            outerDirection.add(MovementCommand.Direction.LEFT);
            outerDirection.add(MovementCommand.Direction.RIGHT);
        }
        if (v_y == 0) {
            outerDirection.add(MovementCommand.Direction.DOWN);
            outerDirection.add(MovementCommand.Direction.UP);
        }

        if (distance1 > distance2) {
            if(v_x > 0) {
                outerDirection.add(MovementCommand.Direction.LEFT);
            } else if (v_x < 0) {
                outerDirection.add(MovementCommand.Direction.RIGHT);
            }

            if(v_y > 0) {
                outerDirection.add(MovementCommand.Direction.DOWN);
            } else if(v_y < 0) {
                outerDirection.add(MovementCommand.Direction.UP);
            }
        } else {
            if(v_x < 0) {
                outerDirection.add(MovementCommand.Direction.LEFT);
            } else if (v_x > 0) {
                outerDirection.add(MovementCommand.Direction.RIGHT);
            }

            if(v_y > 0) {
                outerDirection.add(MovementCommand.Direction.DOWN);
            } else if(v_y < 0) {
                outerDirection.add(MovementCommand.Direction.UP);
            }
        }
        return outerDirection;
    }
}
