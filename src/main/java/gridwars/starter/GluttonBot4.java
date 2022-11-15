package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Simple bot that expands into all directions if there is a cell that does not belong to the bot
 */
public class GluttonBot4 implements PlayerBot {

    Coordinates startingPosition = null;
    Random r = new Random();
    private class DXDY {
        final int dx;
        final int dy;
        DXDY(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }
    int currentTurn = 1;
    private Map<DXDY,Float> map = Map.ofEntries(
                Map.entry(new DXDY(0,0), 0.25f),
                Map.entry(new DXDY(0,1), 0.25f),
                Map.entry(new DXDY(0,-1), 0.25f),
                Map.entry(new DXDY(1,0), 0.25f),
                Map.entry(new DXDY(-1,0), 0.25f),
                Map.entry(new DXDY(1,1), 0.25f),
                Map.entry(new DXDY(-1,-1), 0.25f),
                Map.entry(new DXDY(1,-1), 0.25f),
                Map.entry(new DXDY(-1,1), 0.25f)
            );
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

                map.entrySet().stream().map(e -> {
                    int dx = e.getKey().dx;
                    int dy = e.getKey().dy;
                    float weight = e.getValue();
                    Coordinates target = cell
                            .getRelative(Math.abs(dx), dx > 0 ? MovementCommand.Direction.RIGHT : MovementCommand.Direction.LEFT)
                            .getRelative(Math.abs(dy), dy > 0 ? MovementCommand.Direction.UP : MovementCommand.Direction.DOWN);
                    return universeView.getPopulation(target) * weight;
                }).collect(Collectors.toList());

                Collections.shuffle(okDirections);

                if(okDirections.size() == 0) continue;

//                if(currentArrVal > 0 && currentTurn - currentArrVal > 20) {
//                    continue;
//                }

                int toMovePopulation = currentPopulation - Math.max(5, (int) (Math.sqrt((double) currentTurn / 4)));
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
