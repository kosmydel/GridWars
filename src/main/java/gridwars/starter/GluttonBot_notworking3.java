package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;
import com.sun.source.tree.Tree;

import java.util.*;
//Util.getExpansionDirections(universeView, cell);


/**
 * Simple bot that expands into all directions if there is a cell that does not belong to the bot
 */
public class GluttonBot implements PlayerBot {
    Coordinates startingPosition = null;

    int[][] firstComeFrom = new int[50][50];
    int[][] comeFrom = new int[50][50];

    int[][] gravity = new int[50][50];
    int[][] distanceFromCenter = new int[50][50];
    boolean[][] visited = new boolean[50][50];
    //    int[][] edgeDistance = new int[50][50];
    int[][] neighboursAmount = new int[50][50];
    int[][] enemiesAmount = new int[50][50];
    int currentTurn = 1;

    int[][] universe = new int[50][50];

    int[][] movedPopulation = new int[50][50];

    private final int STRATEGY_CHANGE;
    private final double DENOMINATOR_VALUE;

    private final double LINEAR_TRANSFER_DENOMINATOR;

    private final int LINEAR_TRANSFER_POPULATION_THRESHOLD;
    private final int LINEAR_TRANSFER_TURN_THRESHOLD;

    private final int BFS_GRAVITY_INCREMENT;

    Random r = new Random(42);
    UniverseView universeView;
    public GluttonBot()
    {
        this(200,
                1.0,
                4.0,
                30,
                150,
                1);
    }
    public GluttonBot(int strategy_change,
                      double denominator_value,
                      double linear_transfer_denominator,
                      int linear_transfer_population_threshold,
                      int linear_transfer_turn_threshold,
                      int bfs_gravity_increment)
    {
        STRATEGY_CHANGE = strategy_change;
        DENOMINATOR_VALUE = denominator_value;
        LINEAR_TRANSFER_DENOMINATOR = linear_transfer_denominator;
        LINEAR_TRANSFER_POPULATION_THRESHOLD = linear_transfer_population_threshold;
        LINEAR_TRANSFER_TURN_THRESHOLD = linear_transfer_turn_threshold;
        BFS_GRAVITY_INCREMENT = bfs_gravity_increment;
    }


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
            if(currentTurn < 20 && false) {
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
                move(commandList, universeView.getMyCells().get(0), MovementCommand.Direction.UP, 20);
                move(commandList, universeView.getMyCells().get(0), MovementCommand.Direction.RIGHT, 25);
                move(commandList, universeView.getMyCells().get(0), MovementCommand.Direction.DOWN, 25);
                move(commandList, universeView.getMyCells().get(0), MovementCommand.Direction.LEFT, 25);
            } else if(currentTurn < STRATEGY_CHANGE) {
//                strategy1(universeView, commandList);
                strategy0(universeView, commandList);
            }else {
                strategy2(universeView, commandList);
            }
            long tk = System.currentTimeMillis();
            if(tk-t0 > 50) {
                System.out.println("[!!!] Completed round " + currentTurn + " in " + (tk-t0) + "ms");
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public void strategy0(UniverseView universeView, List<MovementCommand> commandList) {
        LinkedList<Coordinates> queue = new LinkedList<>();
        LinkedList<Coordinates> queue2 = new LinkedList<>();
        visited = new boolean[50][50];
        distanceFromCenter = new int[50][50];

        int i = startingPosition.getX();
        int j = startingPosition.getY();

        queue.add(universeView.getCoordinates(i, j));
        queue2.add(universeView.getCoordinates(i, j));
        visited[i][j] = true;
        distanceFromCenter[i][j] = 200;

        while (!queue.isEmpty()) {
            Coordinates p = queue.poll();
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = p.getNeighbour(dir);

                boolean isMine = universeView.belongsToMe(neighbour);
                boolean isEmpty = universeView.isEmpty(neighbour);

                int x = neighbour.getX();
                int y = neighbour.getY();
                if (!visited[x][y]) {
                    if(!isEmpty && isMine) {
                        queue.add(neighbour);
                        queue2.add(neighbour);
                        visited[x][y] = true;
                        distanceFromCenter[x][y] = distanceFromCenter[p.getX()][p.getY()] - 1;
                    }
                }
            }
        }

        while (!queue2.isEmpty()) {
            Coordinates p = queue2.poll();

            if(gravity[p.getX()][p.getY()] > 6) {
                continue;
            }
            var okDirections = new ArrayList<MovementCommand.Direction>();

            // Find correct neighbours
            int maxNeighbourPopulation = 0;
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = p.getNeighbour(dir);

                boolean isMine = universeView.belongsToMe(neighbour);
                boolean isEmpty = universeView.isEmpty(neighbour);
                int neighbourPopulation = universeView.getPopulation(neighbour);

                int x = neighbour.getX();
                int y = neighbour.getY();
                int alreadyAdded = universeView.getPopulation(x, y);
                maxNeighbourPopulation = Math.max(maxNeighbourPopulation, neighbourPopulation);
                if(distanceFromCenter[x][y] < distanceFromCenter[p.getX()][p.getY()]) {
                    okDirections.add(dir);
                }
            }
            int toMovePopulation = Math.min(universeView.getPopulation(p) + movedPopulation[p.getX()][p.getY()] - 5, universeView.getPopulation(p));

            // Divide our population to neighbours that each of them has maximum
            for (MovementCommand.Direction dir : MovementCommand.Direction.values()) {
                Coordinates neighbour = p.getNeighbour(dir);

                int x = neighbour.getX();
                int y = neighbour.getY();

                boolean isMine = universeView.belongsToMe(neighbour);
                boolean isEmpty = universeView.isEmpty(neighbour);
                int totalNeighbourPopulation = universeView.getPopulation(neighbour) + movedPopulation[neighbour.getX()][neighbour.getY()];

                if(distanceFromCenter[x][y] < distanceFromCenter[p.getX()][p.getY()]) {
                    int toMove = Math.min(maxNeighbourPopulation - totalNeighbourPopulation, toMovePopulation);
                    if(toMove > 0) {
                        toMovePopulation -= toMove;
                        move(commandList, p, dir, toMove);
                    }
                }
            }

            boolean doneSomething = true;
            while (toMovePopulation > 0 && doneSomething) {
                doneSomething = false;
                Collections.shuffle(okDirections);
                for (int c = 0; c < okDirections.size(); c++) {
                    MovementCommand.Direction dir = okDirections.get(0);

                    int movePopulation = Math.min(toMovePopulation / (okDirections.size() - c + 1), universeView.getPopulation(p));

                    if (movePopulation > 0) {
                        toMovePopulation -= movePopulation;
                        doneSomething = true;
                        move(commandList, p, dir, movePopulation);
                        okDirections.remove(0);
                    }
                }
            }
        }
    }

    public void calculateDistanceFromCenter(UniverseView universeView) {

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

                int neighbourPopulation = universeView.getPopulation(neighbour);
                // TODO: tutaj jakos mozna jeszcze poprawic, np na srodku zeby nie zostawalo po malo jednostek tylko po wiecej
                int toMove = Math.min((int)(currentPopulation / (i + DENOMINATOR_VALUE)),
                        (enemiesAmount[x][y] > 0 ? 200 : 100) - neighbourPopulation - movedPopulation[neighbour.getX()][neighbour.getY()]);

                move(commandList, cell, directions.get(0), toMove);
                directions.remove(0);
            }

        }
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
                        gravity[x][y] = gravity[p_x][p_y] + BFS_GRAVITY_INCREMENT;
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

        if(currentTurn < LINEAR_TRANSFER_TURN_THRESHOLD) return;

        for(Coordinates cell : universeView.getMyCells()) {
            int i = cell.getX();
            int j = cell.getY();

            int population = universeView.getPopulation(universeView.getCoordinates(i, j));
            if (population < LINEAR_TRANSFER_POPULATION_THRESHOLD) {
                gravity[i][j] -= population / LINEAR_TRANSFER_DENOMINATOR;
            }
            // TODO: sprawdzic podwyzszanie z drugiej strony
        }
    }

    public int calculateDistance(Coordinates c1, Coordinates c2) {
        int x1 = c1.getX();
        int y1 = c1.getY();
        int x2 = c2.getX();
        int y2 = c2.getY();
        return (int) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
}
