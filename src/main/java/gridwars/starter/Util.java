package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Util {
    public static List<MovementCommand.Direction> getExpansionDirections(UniverseView universeView, Coordinates cell) {
        return Arrays.stream(MovementCommand.Direction.values())
                .filter(dir -> universeView.isEmpty(cell.getNeighbour(dir)))
                .collect(Collectors.toList());
    }

    public static boolean isOnEdge(UniverseView universeView, Coordinates cell) {
        return getExpansionDirections(universeView, cell).size() == 0;
    }

    public static int[][] getClosestEdgeDistances(UniverseView universeView)
    {
        int[][] distances = new int[universeView.getUniverseSize()][universeView.getUniverseSize()];
        for (int i = 0; i < universeView.getUniverseSize(); i++)
        {
            for(int j = 0; j < universeView.getUniverseSize(); j++)
                distances[i][j]= Integer.MIN_VALUE;
        }
        Queue<Coordinates> coordinatesQueue = new LinkedList<>();
        for(Coordinates cell : universeView.getMyCells())
        {
            if(universeView.getPopulation(cell) < 5)
                continue;
            if(Arrays.stream(MovementCommand.Direction.values())
                    .anyMatch(dir -> {
                        Coordinates neighbour = cell.getNeighbour(dir);
                        return !universeView.belongsToMe(neighbour) || universeView.getPopulation(neighbour) < 5;
                    }))
            {
                distances[cell.getX()][cell.getY()] = 0;
                coordinatesQueue.add(cell);
            }
        }
        while (!coordinatesQueue.isEmpty())
        {
            Coordinates current = coordinatesQueue.remove();
            int currentDistance = distances[current.getX()][current.getY()];
            for(MovementCommand.Direction dir : MovementCommand.Direction.values())
            {
                Coordinates neighbor = current.getNeighbour(dir);
                if(!universeView.belongsToMe(neighbor) || universeView.getPopulation(neighbor) == 0)
                    continue;
                if(distances[neighbor.getX()][neighbor.getY()] != Integer.MIN_VALUE)
                    continue;
                distances[neighbor.getX()][neighbor.getY()] = currentDistance + 1;
                coordinatesQueue.add(neighbor);
            }
        }
        return distances;
    }

    public static List<MovementCommand.Direction> getDirectionWhere(Predicate<MovementCommand.Direction> predicate)
    {
        return Arrays.stream(MovementCommand.Direction.values()).filter(predicate).collect(Collectors.toList());
    }
    public static List<Integer> splitEvenly(int value, int into)
    {
        int divided = value / into;
        int remainder = value - divided * into;
        List<Integer> output = new ArrayList<>(into);
        for(int i = 0 ; i< into; i++)
        {
            output.add(divided + i < remainder ? 1 : 0);
        }
        Collections.shuffle(output);
        return output;
    }
}
