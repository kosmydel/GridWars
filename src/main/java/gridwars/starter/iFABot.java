package gridwars.starter;

import cern.ais.gridwars.api.bot.PlayerBot;

import java.util.*;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.bot.PlayerBot;
import cern.ais.gridwars.api.command.MovementCommand;

public class iFABot implements PlayerBot {
    private Integer side;
    private Boolean firstTurn = true;
    private Random rand = new Random();

    @Override
    public void getNextCommands(UniverseView universeView, List<MovementCommand> movementCommands) {
        try {
            if(universeView.getCurrentTurn() < 100)
                firstStrategy(universeView, movementCommands);
            else if (universeView.getCurrentTurn() < 500)
                secondStrategy(universeView, movementCommands);
            else if (universeView.getCurrentTurn() < 530)
                firstStrategy(universeView, movementCommands);
            else
                secondStrategy(universeView, movementCommands);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void firstStrategy(UniverseView universeView, List<MovementCommand> movementCommands) {
        List<Coordinates> myCoord = new ArrayList<>(universeView.getMyCells());
        Collections.shuffle(myCoord, rand);
        if(firstTurn) {
            side = universeView.getUniverseSize();
            firstTurn = false;

            for(Coordinates coord : myCoord) {
                int movement = universeView.getPopulation(coord)/4;
                movementCommands.add(new MovementCommand(coord, MovementCommand.Direction.LEFT, movement));
                movementCommands.add(new MovementCommand(coord, MovementCommand.Direction.RIGHT, movement));
                movementCommands.add(new MovementCommand(coord, MovementCommand.Direction.UP, movement));
                movementCommands.add(new MovementCommand(coord, MovementCommand.Direction.DOWN, movement));
            }
        } else {
            for (Coordinates coord : myCoord) {
                int movement = universeView.getPopulation(coord) - (universeView.getPopulation(coord)/5);
                MovementCommand.Direction direction = null;
                direction = getDirection(universeView, coord, direction);
                if (direction != null && canMove(universeView, coord, direction, movement))
                    movementCommands.add(new MovementCommand(coord, direction, movement));
            }
        }
    }

    private void secondStrategy(UniverseView universeView, List<MovementCommand> movementCommands) {
        for(Coordinates coord : getOrderedHeuristic(universeView)) {
            if (universeView.getPopulation(coord) > 1) {
                int movement = universeView.getPopulation(coord)/ 2;
                MovementCommand.Direction direction = getHeuristicalDirection(universeView, coord);
                movementCommands.add(new MovementCommand(coord, direction, movement));
            }
        }
    }

    public int randInt(int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }

    private MovementCommand.Direction getDirection(UniverseView universeView, Coordinates coord, MovementCommand.Direction direction) {
        Boolean doRandom = randInt(1, 100) > 90;
        if(/*!doRandom && */universeView.getPopulation(coord) >= universeView.getMaximumPopulation()/4) {
            direction = getDirection(universeView, coord);
        } else if (universeView.getPopulation(coord) >= 2) {
            direction = getRandomDirection();
        }
        return direction;
    }

    private Boolean canMove(UniverseView universeView, Coordinates myCoord, MovementCommand.Direction dir, int numPeople) {
        Coordinates next = null;
        switch (dir) {
            case DOWN: next = myCoord.getDown(); break;
            case UP: next = myCoord.getUp(); break;
            case LEFT: next = myCoord.getLeft(); break;
            case RIGHT: next = myCoord.getRight(); break;
        }
        int newPeople = numPeople + universeView.getPopulation(next);
        return (!universeView.belongsToMe(next) || newPeople <= universeView.getMaximumPopulation()) && numPeople > 0;
    }

    private MovementCommand.Direction getRandomDirection() {
        switch (rand.nextInt() % 4) {
            case 0: return MovementCommand.Direction.DOWN;
            case 1: return MovementCommand.Direction.LEFT;
            case 2: return MovementCommand.Direction.UP;
            case 3: return MovementCommand.Direction.RIGHT;
            default: return MovementCommand.Direction.RIGHT;

        }
    }

    private MovementCommand.Direction getDirection(UniverseView universeView, Coordinates myCoord) {
        Coordinates theirCoord = getClosest(universeView, myCoord);

        Double distance = getDistance(myCoord, theirCoord);
        MovementCommand.Direction direction = null;

        if(getDistance(myCoord.getDown(), theirCoord) < distance) {
            distance = getDistance(myCoord.getDown(), theirCoord);
            direction = MovementCommand.Direction.DOWN;
        }

        if(getDistance(myCoord.getUp(), theirCoord) < distance) {
            distance = getDistance(myCoord.getUp(), theirCoord);
            direction = MovementCommand.Direction.UP;
        }

        if(getDistance(myCoord.getLeft(), theirCoord) < distance) {
            distance = getDistance(myCoord.getLeft(), theirCoord);
            direction = MovementCommand.Direction.LEFT;
        }

        if(getDistance(myCoord.getRight(), theirCoord) < distance) {
            direction = MovementCommand.Direction.RIGHT;
        }

        return direction;
    }

    private List<Coordinates> getAdversaryCoordinates(UniverseView universeView) {
        ArrayList<Coordinates> adversary = new ArrayList<Coordinates>();

        for(int i = 0; i < side; i++) {
            for(int j = 0; j < side; j++) {
                Coordinates coord = universeView.getCoordinates(i, j);
                if(!(universeView.isEmpty(coord) || universeView.belongsToMe(coord))) {
                    adversary.add(coord);
                }
            }
        }

        return adversary;
    }

    private Double getDistance(Coordinates myCoord, Coordinates theirCoord) {
        return Math.sqrt(Math.pow(myCoord.getX()-theirCoord.getX(), 2) + Math.pow(myCoord.getY() - theirCoord.getY(), 2));
    }

    private Coordinates getClosest(UniverseView universeView, Coordinates myCoord) {
        Double distance = Double.MAX_VALUE;
        Coordinates closest = null;

        for(Coordinates theirCoord : getAdversaryCoordinates(universeView)) {
            Double dist = getDistance(myCoord, theirCoord);
            if(dist < distance) {
                distance = dist;
                closest = theirCoord;
            }
        }

        return closest;
    }

    private Queue<Coordinates> getOrderedHeuristic(UniverseView universeView) {
        PriorityQueue<Coordinates> ret = new PriorityQueue<>(4, new HeuristicComparator(universeView));
        for(Coordinates coord : universeView.getMyCells())
            ret.add(coord);
        return ret;
    }

    private MovementCommand.Direction getHeuristicalDirection(UniverseView universeView, Coordinates coordinates) {
        MovementCommand.Direction ret = null;
        int population = Integer.MAX_VALUE;
        int tmp_p;
        Coordinates tmp_c;

        tmp_c = coordinates.getLeft();
        tmp_p = (universeView.belongsToMe(tmp_c) ? universeView.getPopulation(tmp_c) : -universeView.getPopulation(tmp_c));
        if(tmp_p < population) {
            ret = MovementCommand.Direction.LEFT;
            population = tmp_p;
        }

        tmp_c = coordinates.getDown();
        tmp_p = (universeView.belongsToMe(tmp_c) ? universeView.getPopulation(tmp_c) : -universeView.getPopulation(tmp_c));
        if(tmp_p < population) {
            ret = MovementCommand.Direction.DOWN;
            population = tmp_p;
        }

        tmp_c = coordinates.getUp();
        tmp_p = (universeView.belongsToMe(tmp_c) ? universeView.getPopulation(tmp_c) : -universeView.getPopulation(tmp_c));
        if(tmp_p < population) {
            ret = MovementCommand.Direction.UP;
            population = tmp_p;
        }

        tmp_c = coordinates.getRight();
        tmp_p = (universeView.belongsToMe(tmp_c) ? universeView.getPopulation(tmp_c) : -universeView.getPopulation(tmp_c));
        if(tmp_p < population) {
            ret = MovementCommand.Direction.RIGHT;
        }

        return ret;
    }

    private class HeuristicComparator implements Comparator<Coordinates> {
        private UniverseView universeView;

        public HeuristicComparator(UniverseView universeView) {
            this.universeView = universeView;
        }

        @Override
        public int compare(Coordinates o1, Coordinates o2) {
            int populationNearO1 = 0;
            int populationNearO2 = 0;

            Coordinates o1u = o1.getUp();
            Coordinates o1l = o1.getLeft();
            Coordinates o1d = o1.getDown();
            Coordinates o1r = o1.getRight();

            Coordinates o2u = o2.getUp();
            Coordinates o2l = o2.getLeft();
            Coordinates o2d = o2.getDown();
            Coordinates o2r = o2.getRight();

            populationNearO1 = (universeView.belongsToMe(o1u) ? universeView.getPopulation(o1u) : -universeView.getPopulation(o1u));
            populationNearO1 = (universeView.belongsToMe(o1l) ? universeView.getPopulation(o1l) : -universeView.getPopulation(o1l));
            populationNearO1 = (universeView.belongsToMe(o1d) ? universeView.getPopulation(o1d) : -universeView.getPopulation(o1d));
            populationNearO1 = (universeView.belongsToMe(o1r) ? universeView.getPopulation(o1r) : -universeView.getPopulation(o1r));

            populationNearO2 = (universeView.belongsToMe(o2u) ? universeView.getPopulation(o2u) : -universeView.getPopulation(o2u));
            populationNearO2 = (universeView.belongsToMe(o2l) ? universeView.getPopulation(o2l) : -universeView.getPopulation(o2l));
            populationNearO2 = (universeView.belongsToMe(o2d) ? universeView.getPopulation(o2d) : -universeView.getPopulation(o2d));
            populationNearO2 = (universeView.belongsToMe(o2r) ? universeView.getPopulation(o2r) : -universeView.getPopulation(o2r));

            return populationNearO1 - populationNearO2;
        }
    }
}