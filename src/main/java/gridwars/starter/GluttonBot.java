package gridwars.starter;

import cern.ais.gridwars.StdOutputSwitcher;
import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Simple bot that expands into all directions if there is a cell that does not belong to the bot
 */
public class GluttonBot implements PlayerBot {
    Coordinates startingPosition = null;
    Random r = new Random();

    int[][] arr = new int[50][50];
    int currentTurn = 1;

    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        if (universeView.getCurrentTurn() < 100) {
            try {
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
                    int currentArrVal = arr[cell.getX()][cell.getY()];

                    for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                        Coordinates neighbour = cell.getNeighbour(dir);
                        int arrVal = arr[neighbour.getX()][neighbour.getY()];

                        boolean isMine = universeView.belongsToMe(neighbour);
                        boolean isEmpty = universeView.isEmpty(neighbour);

                        if (arrVal == 0 || arrVal > currentArrVal || (!isEmpty && !isMine)) {
                            okDirections.add(dir);
                        }
                    }

                    Collections.shuffle(okDirections);

                    if (okDirections.size() == 0) continue;

//                if(currentArrVal > 0 && currentTurn - currentArrVal > 20) {
//                    continue;
//                }

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
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            try {
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
                                    (int)((double) (currentPopulation - neighboursPopulation) / 2.0),
                                    100 - neighboursPopulation);
                            if (movePopulation > 0) {
                                currentPopulation -= movePopulation;
                                doneSomething = true;
                                move(commandList, cell, dir, movePopulation);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void move(List<MovementCommand> commandList, Coordinates cell, MovementCommand.Direction direction, int population) {
        commandList.add(new MovementCommand(cell, direction, population));
        if(arr[cell.getX()][cell.getY()] == 0) {
            arr[cell.getX()][cell.getY()] = currentTurn;
        }
    }
}
