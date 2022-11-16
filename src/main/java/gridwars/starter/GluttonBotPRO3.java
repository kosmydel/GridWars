package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.*;
//Util.getExpansionDirections(universeView, cell);


/**
 * Simple bot that expands into all directions if there is a cell that does not belong to the bot
 */
public class GluttonBotPRO3 implements PlayerBot {
    Coordinates startingPosition = null;

    int[][] firstComeFrom = new int[50][50];
    int[][] comeFrom = new int[50][50];

    int[][] gravity = new int[50][50];
    boolean[][] visited = new boolean[50][50];
    //    int[][] edgeDistance = new int[50][50];
    int[][] neighboursAmount = new int[50][50];
    int[][] enemiesAmount = new int[50][50];
    int currentTurn = 1;

    int[][] universe = new int[50][50];

    int[][] movedPopulation = new int[50][50];

    private static final int STRATEGY_CHANGE = 80;
    private static int DENOMINATOR_VALUE = 1;
    Random r = new Random(42);
    UniverseView universeView;

    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        try {
            long t0 = System.currentTimeMillis();
            this.universeView = universeView;
            currentTurn = universeView.getCurrentTurn();
            generateNeighboursAmount(universeView);
            generateEnemiesAmount(universeView);
            generateGravityMap(universeView);
            movedPopulation = new int[50][50];

            if (startingPosition == null) {
                startingPosition = universeView.getMyCells().get(0);
            }

            universe = new int[50][50];
            for(Coordinates coordinates : universeView.getMyCells()) {
                if(universeView.belongsToMe(coordinates)){
                    universe[coordinates.getX()][coordinates.getY()] = universeView.getPopulation(coordinates);
                }
            }
            int size = 5;
            if(currentTurn < 10 && false) {
                System.out.println("---- turn ---- " + currentTurn);
                for(int i = (startingPosition.getX() - size + 200) % 50; i < (startingPosition.getX() + size + 200)%50; i++){
                    for(int j = (startingPosition.getY() - size + 200)%50; j < (startingPosition.getY() + size + 200)%50; j++) {
                        System.out.print(String.format("% 3d", universe[i][j]) + ", ");
//                        System.out.print(universe[i][j]);
                    }
                    System.out.println("");
                }
            }
            if(currentTurn <= 2) {
                move(commandList, universeView.getMyCells().get(0), MovementCommand.Direction.UP, 25);
                move(commandList, universeView.getMyCells().get(0), MovementCommand.Direction.RIGHT, 25);
                move(commandList, universeView.getMyCells().get(0), MovementCommand.Direction.DOWN, 25);
                move(commandList, universeView.getMyCells().get(0), MovementCommand.Direction.LEFT, 25);
            } else if(currentTurn < STRATEGY_CHANGE) {
                strategy1(universeView, commandList);
            }else {
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
        if(population <= 0) {
            return;
        }
        int x = cell.getX();
        int y = cell.getY();
        Coordinates neighbour = universeView.getCoordinates(x, y).getNeighbour(direction);

        movedPopulation[neighbour.getX()][neighbour.getY()] += population;
        movedPopulation[x][y] -= population;
        if(firstComeFrom[x][y] == 0) {
            firstComeFrom[x][y] = currentTurn;
        }
        comeFrom[x][y] = currentTurn;
        commandList.add(new MovementCommand(cell, direction, population));
    }

    public void strategy1(UniverseView universeView, List<MovementCommand> commandList) {
        List<Coordinates> myCells = universeView.getMyCells();

        for (Coordinates cell : myCells) {
            int x = cell.getX();
            int y = cell.getY();
            int currentPopulation = universeView.getPopulation(cell);

            List<MovementCommand.Direction> okDirections = new ArrayList<>();
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = cell.getNeighbour(dir);
                if(gravity[neighbour.getX()][neighbour.getY()] < gravity[x][y]) {
                    okDirections.add(dir);
                }
            }

            Collections.shuffle(okDirections);

            int toMovePopulation = currentPopulation - Math.max(5, (int) (Math.sqrt((double) currentTurn / 4)));
//            int toMovePopulation = currentPopulation - 5;

            boolean doneSomething = true;
            while (toMovePopulation > 0 && doneSomething) {
                doneSomething = false;
                for (int i = 0; i < okDirections.size(); i++) {
                    MovementCommand.Direction dir = okDirections.get(i);
                    Coordinates neighbour = cell.getNeighbour(dir);

                    boolean isMine = universeView.belongsToMe(neighbour);
                    boolean isEmpty = universeView.isEmpty(neighbour);

                    int neighboursPopulation = (isMine && !isEmpty) ? universeView.getPopulation(neighbour) : 0;

                    int movePopulation = Math.min(toMovePopulation / (okDirections.size() - i), 100 - neighboursPopulation);

                    if (movePopulation > 0 && neighboursPopulation + movePopulation >= 5) {
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
            int x = cell.getX();
            int y = cell.getY();
            int currentPopulation = universeView.getPopulation(cell);

            List<MovementCommand.Direction> directions = new ArrayList<>();
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = cell.getNeighbour(dir);
                if(gravity[neighbour.getX()][neighbour.getY()] < gravity[x][y]) {
                    directions.add(dir);
                }
            }

            Collections.shuffle(directions);

            int i;
            for(i = directions.size(); i > 0; i--){
                if(currentPopulation / (i + DENOMINATOR_VALUE) >= 5) break;
            }

            if(i == 0) continue;

            for(int j = 0; j < i; j++) {
                Coordinates neighbour = cell.getNeighbour(directions.get(0));
                int currentNeighbourPopulation = universeView.getPopulation(neighbour);

                int neighbourPopulation = universeView.getPopulation(neighbour);
                int toMove = Math.min(currentPopulation / (i + DENOMINATOR_VALUE), 101 - neighbourPopulation - movedPopulation[neighbour.getX()][neighbour.getY()]);

                move(commandList, cell, directions.get(0), toMove);
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

    public void generateEnemiesAmount(UniverseView universeView) {
        enemiesAmount = new int[50][50];

        for(Coordinates cell : universeView.getMyCells()) {
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = cell.getNeighbour(dir);
                boolean isMine = universeView.belongsToMe(neighbour);
                boolean isEmpty = universeView.isEmpty(neighbour);
                if(!isEmpty && !isMine) {
                    enemiesAmount[cell.getX()][cell.getY()]++;
                }
            }
        }
    }

//    public int countNeighboursBy(UniverseView universeView, Coordinates cell, boolean isEmptyCondition, boolean isMineCondition) {
//        int result = 0;
//        for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
//            Coordinates neighbour = cell.getNeighbour(dir);
//            boolean isMine = universeView.belongsToMe(neighbour);
//            boolean isEmpty = universeView.isEmpty(neighbour);
//            if(isEmptyCondition == isEmpty && isMineCondition == isMine) {
////            if(!isEmpty && isMine) {
//                result++;
//                neighboursAmount[neighbour.getX()][neighbour.getY()]++;
//            }
//        }
//    }

    public void generateGravityMap(UniverseView universeView) {
        for (int[] row : gravity)
            Arrays.fill(row, -1);


        LinkedList<Coordinates> queue = new LinkedList<>();
        visited = new boolean[50][50];

        for(Coordinates cell : universeView.getMyCells()) {
            int i = cell.getX();
            int j = cell.getY();
            if(neighboursAmount[i][j] >= 0 && neighboursAmount[i][j] < 4) {
                queue.add(universeView.getCoordinates(i, j));
                gravity[i][j] = 0;
                visited[i][j] = true;
            }
        }

        while (!queue.isEmpty()) {
            Coordinates p = queue.poll();
            int p_x = p.getX();
            int p_y = p.getY();
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = p.getNeighbour(dir);

                boolean isMine = universeView.belongsToMe(neighbour);
                boolean isEmpty = universeView.isEmpty(neighbour);

                if (isMine && !isEmpty) {
                    int x = neighbour.getX();
                    int y = neighbour.getY();
                    if (!visited[x][y]) {
                        queue.add(neighbour);
                        gravity[x][y] = gravity[p_x][p_y] + 1;
                        visited[x][y] = true;
                    }
                }
            }
        }


        int maxGravityValue = Integer.MIN_VALUE;
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                maxGravityValue = Math.max(maxGravityValue, gravity[i][j]);
            }
        }


        if(currentTurn < 150) return;
        for(Coordinates cell : universeView.getMyCells()) {
            int i = cell.getX();
            int j = cell.getY();

            int population = universeView.getPopulation(universeView.getCoordinates(i, j));
            // TODO: PARAMETRY
            if (population < 20) {
                gravity[i][j] -= population / 4;
            }
        }
    }



    public boolean isOnEdge(UniverseView universeView, Coordinates cell) {
        return getExpansionDirectories(universeView, cell).size() == 0;
    }

}
