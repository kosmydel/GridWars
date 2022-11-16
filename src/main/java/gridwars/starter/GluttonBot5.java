package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.*;


/**
 * Simple bot that expands into all directions if there is a cell that does not belong to the bot
 */
public class GluttonBot implements PlayerBot {
    Coordinates startingPosition = null;
    Random r = new Random();

    int[][] firstComeFrom = new int[50][50];
    int[][] comeFrom = new int[50][50];
    int[][] gravity = new int[50][50];
    int[][] neighboursAmount = new int[50][50];
    int currentTurn = 1;

    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        try {
            long t0 = System.currentTimeMillis();
            currentTurn = universeView.getCurrentTurn();
            generateNeighboursAmount(universeView);
//            strategy2(universeView, commandList);
            if(currentTurn < 100) {
                strategy1(universeView, commandList);
            }else {
                generateGravityMap(universeView);
                strategy2(universeView, commandList);
            }
            long tk = System.currentTimeMillis();
            if(tk-t0 > 50) {
                System.out.println("[!!!] Completed round " + currentTurn + " in " + (tk-t0) + "ms");
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

        for (Coordinates cell : myCells) {
            int currentPopulation = universeView.getPopulation(cell);

            List<MovementCommand.Direction> directions = new ArrayList<>();
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = cell.getNeighbour(dir);
                boolean isMine = universeView.belongsToMe(neighbour);
                boolean isEmpty = universeView.isEmpty(neighbour);
                if(isEmpty || !isMine) {
                    directions.add(dir);
                }
            }
            Collections.shuffle(directions);

            int i;
            for(i = directions.size(); i > 0; i--){
                if(currentPopulation / (i + 1) >= 5) break;
            }

            if(i == 0) continue;

            for(int j = 0; j < i; j++) {
                move(commandList, cell, directions.get(0), currentPopulation / (i + 1));
                directions.remove(0);
            }

        }
    }

    public List<MovementCommand.Direction> getExpansionDirectories(UniverseView universeView, Coordinates cell) {
        List<MovementCommand.Direction> expansionDirections = new ArrayList<>();

        for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
            Coordinates neighbour = cell.getNeighbour(dir);
            boolean isMine = universeView.belongsToMe(neighbour);
            boolean isEmpty = universeView.isEmpty(neighbour);
            if(isEmpty || !isMine) {
                expansionDirections.add(dir);
            }
        }
        return expansionDirections;
    }

    public void generateNeighboursAmount(UniverseView universeView) {
        for (int[] row: neighboursAmount)
            Arrays.fill(row, -1);

        for(Coordinates cell : universeView.getMyCells()) {
            neighboursAmount[cell.getX()][cell.getY()]++;
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = cell.getNeighbour(dir);
                boolean isMine = universeView.belongsToMe(neighbour);
                boolean isEmpty = universeView.isEmpty(neighbour);
                if(!isEmpty && isMine) {
                    neighboursAmount[neighbour.getX()][neighbour.getY()]++;
                }
            }
        }
    }
    public void generateGravityMap(UniverseView universeView) {
        for (int[] row: gravity)
            Arrays.fill(row, -1);

        LinkedList<Coordinates> queue = new LinkedList<>();
        for(int i = 0; i < 50; i++) {
            for(int j = 0; j < 50; j++) {
                if(neighboursAmount[i][j] >= 0 && neighboursAmount[i][j] < 4) {
                    queue.add(universeView.getCoordinates(i, j));
                    gravity[i][j] = 0;
                }
            }
        }

        while(!queue.isEmpty()) {
            Coordinates p = queue.poll();
            int p_x = p.getX();
            int p_y = p.getY();
            for(MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = p.getNeighbour(dir);

                boolean isMine = universeView.belongsToMe(neighbour);
                boolean isEmpty = universeView.isEmpty(neighbour);

                if(isMine && !isEmpty) {
                    int x = neighbour.getX();
                    int y = neighbour.getY();
                    if(gravity[x][y] < 0) {
                        queue.add(neighbour);
                        gravity[x][y] = gravity[p_x][p_y] + 1;
                    }
                }
            }
        }
    }



    public boolean isOnEdge(UniverseView universeView, Coordinates cell) {
        return getExpansionDirectories(universeView, cell).size() == 0;
    }
}
