package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.*;

import gridwars.starter.Util;
//Util.getExpansionDirections(universeView, cell);


/**
 * Simple bot that expands into all directions if there is a cell that does not belong to the bot
 */
public class GluttonBot implements PlayerBot {
    Coordinates startingPosition = null;
    Random r = new Random();

    int[][] firstComeFrom = new int[50][50];
    int[][] comeFrom = new int[50][50];
    int currentTurn = 1;

    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        currentTurn = universeView.getCurrentTurn();
        try {
            if(currentTurn % 100 < 90) {
                strategy1(universeView, commandList);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void move(List<MovementCommand> commandList, Coordinates cell, MovementCommand.Direction direction, int population) {
        commandList.add(new MovementCommand(cell, direction, population));
        if(firstComeFrom[cell.getX()][cell.getY()] == 0) {
            firstComeFrom[cell.getX()][cell.getY()] = currentTurn;
        }
        comeFrom[cell.getX()][cell.getY()] = currentTurn;
    }

    public void strategy1(UniverseView universeView, List<MovementCommand> commandList) {
        List<Coordinates> myCells = universeView.getMyCells();

        if (startingPosition == null) {
            startingPosition = myCells.get(0);
        }

        currentTurn = universeView.getCurrentTurn();

        for (Coordinates cell : myCells) {
            int currentPopulation = universeView.getPopulation(cell);

            if (currentPopulation <= 5) {
                continue;
            }

            List<MovementCommand.Direction> okDirections = new ArrayList<>();
            int currentArrVal = firstComeFrom[cell.getX()][cell.getY()];

            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = cell.getNeighbour(dir);
                int arrVal = firstComeFrom[neighbour.getX()][neighbour.getY()];

                boolean isMine = universeView.belongsToMe(neighbour);
                boolean isEmpty = universeView.isEmpty(neighbour);

                if (arrVal == 0 || arrVal > currentArrVal || (!isEmpty && !isMine)) {
                    okDirections.add(dir);
                }
            }

            Collections.shuffle(okDirections);

            if (okDirections.size() == 0) continue;

            int toMovePopulation = currentPopulation - Math.max(5, (int) (Math.sqrt((double) currentTurn / 4)));
            boolean doneSomething = true;
            while (toMovePopulation > 0 && doneSomething) {
                doneSomething = false;
                for (int i = 0; i < okDirections.size(); i++) {
                    MovementCommand.Direction dir = okDirections.get(i);
                    Coordinates neighbour = cell.getNeighbour(dir);
                    boolean isMine = universeView.belongsToMe(neighbour);
                    boolean isEmpty = universeView.isEmpty(neighbour);

                    int neighboursPopulation = (isMine || isEmpty) ? universeView.getPopulation(neighbour) : 0;

                    int movePopulation = Math.min(toMovePopulation / (okDirections.size() - i), 100 - neighboursPopulation);

                    if (movePopulation > 0) {
                        toMovePopulation -= movePopulation;
                        doneSomething = true;
                        move(commandList, cell, dir, movePopulation);
                    }
                }
            }
        }
    }

    public void strategy2(UniverseView universeView, List<MovementCommand> commandList) {
        List<Coordinates> myCells = universeView.getMyCells();

        if (startingPosition == null) {
            startingPosition = myCells.get(0);
        }

        currentTurn = universeView.getCurrentTurn();

        for (Coordinates cell : myCells) {
            int currentPopulation = universeView.getPopulation(cell);

            if (currentPopulation <= 5) {
                continue;
            }
            boolean doneSomething = true;
            while (doneSomething) {
                doneSomething = false;
                for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                    Coordinates neighbour = cell.getNeighbour(dir);
                    boolean isMine = universeView.belongsToMe(neighbour);
                    boolean isEmpty = universeView.isEmpty(neighbour);

                    int neighboursPopulation = (isMine || isEmpty) ? universeView.getPopulation(neighbour) : 0;
                    if(currentPopulation < neighboursPopulation) {
                        continue;
                    }
                    int movePopulation = Math.min(
                            (int)((double) (currentPopulation - neighboursPopulation) / 10.0),
                            100 - neighboursPopulation);
                    if (movePopulation > 0) {
                        currentPopulation -= movePopulation;
                        doneSomething = true;
                        move(commandList, cell, dir, movePopulation);
                    }
                }
            }
        }
    }


}
