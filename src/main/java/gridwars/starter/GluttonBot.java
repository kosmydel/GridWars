package gridwars.starter;

import cern.ais.gridwars.StdOutputSwitcher;
import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
        try {
            List<Coordinates> myCells = universeView.getMyCells();

            if(startingPosition == null) {
                startingPosition = myCells.get(0);
            }

            currentTurn = universeView.getCurrentTurn();

            for (Coordinates cell : myCells) {
                int currentPopulation = universeView.getPopulation(cell);

                if(currentPopulation <= 5) {
                    continue;
                }

                List<MovementCommand.Direction> okDirections = new ArrayList<>();
                int currentArrVal = arr[cell.getX()][cell.getY()];

                for(MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                    Coordinates neighbour = cell.getNeighbour(dir);
                    int arrVal = arr[neighbour.getX()][neighbour.getY()];
                    if(arrVal == 0 || arrVal > currentArrVal) {
                        okDirections.add(dir);
                    }
                }

                Collections.shuffle(okDirections);

                if(okDirections.size() == 0) continue;

                if(currentArrVal > 0 && currentTurn - currentArrVal > 20) {
                    continue;
                }

                int toMovePopulation = currentPopulation - Math.max(5, (int) (Math.sqrt((double) currentTurn / 10)));
                boolean doneSomething = true;
                while (toMovePopulation > 0 && doneSomething)
                {
                    doneSomething = false;
                    for(int i = 0; i < okDirections.size(); i++) {
                        MovementCommand.Direction dir = okDirections.get(i);
                        Coordinates neighbour = cell.getNeighbour(dir);
                        boolean isMine = universeView.belongsToMe(neighbour);
                        boolean isEmpty = universeView.isEmpty(neighbour);

                        int neighboursPopulation = (isMine || isEmpty) ? universeView.getPopulation(neighbour) : 0;

                        int movePopulation = Math.min(toMovePopulation / (okDirections.size() - i), 100 - neighboursPopulation);

                        if(movePopulation > 0) {
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

    }

    public void move(List<MovementCommand> commandList, Coordinates cell, MovementCommand.Direction direction, int population) {
        commandList.add(new MovementCommand(cell, direction, population));
        if(arr[cell.getX()][cell.getY()] == 0) {
            arr[cell.getX()][cell.getY()] = currentTurn;
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
