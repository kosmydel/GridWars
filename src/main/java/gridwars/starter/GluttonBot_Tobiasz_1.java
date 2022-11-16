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
public class GluttonBot_Tobiasz_1 implements PlayerBot {

    int currentTurn = 1;

    public void getNextCommands(UniverseView universeView, List<MovementCommand> commandList) {
        currentTurn = universeView.getCurrentTurn();
        try {
            strategyBFS(universeView, commandList);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
//    private void strategyMaxFlow(UniverseView universeView, List<MovementCommand> commandList)
//    {
//        Set<Coordinates> sinks = universeView.getMyCells().stream()
//                .flatMap(cell ->
//                    Arrays.stream(MovementCommand.Direction.values())
//                            .map(cell::getNeighbour)
//                            .filter(neighbour ->
//                                    !universeView.belongsToMe(neighbour) || universeView.getPopulation(neighbour) < 5
//                            )
//                ).collect(Collectors.toSet());
//        Map<Coordinates, EnumMap<MovementCommand.Direction, Integer>> transfersOut = new HashMap<>();
//        Map<Coordinates, Integer> capacity = new HashMap<>();
//        Map<Coordinates, Integer> sourceTo = new HashMap<>();
//        for(Coordinates cell : universeView.getMyCells())
//        {
//            int current = universeView.getPopulation(cell);
//            capacity.put(cell, current);
//            if(current > 5)
//            {
//                sourceTo.put(cell, 0);
//            }
//        }
//        Map<Coordinates, Integer> sinkFrom = new HashMap<>();
//        Map<Coordinates, Integer> sinkFromCapa = new HashMap<>();
//        for(Coordinates cell : sinks)
//        {
//
//        }
//
//    }

    private void strategyBFS(UniverseView universeView, List<MovementCommand> commandList)
    {
        int[][] distances = Util.getClosestEdgeDistances(universeView);
        List<Coordinates> myCells = universeView
                .getMyCells()
                .stream()
                .sorted(Comparator.comparingInt(coord -> distances[coord.getX()][coord.getY()]))
                .collect(Collectors.toList());
        int[][] potentialIncoming = new int[universeView.getUniverseSize()][universeView.getUniverseSize()];
        int[][][] potentialOutgoing =
                new int[universeView.getUniverseSize()][universeView.getUniverseSize()][MovementCommand.Direction.values().length];
        int[][] incomingCapacityAvailable = new int[universeView.getUniverseSize()][universeView.getUniverseSize()];
        for (int[] ints : incomingCapacityAvailable) {
            Arrays.fill(ints, 100);
        }
        for(Coordinates cell : universeView.getMyCells())
        {
            incomingCapacityAvailable[cell.getX()][cell.getY()] = universeView.getPopulation(cell) + 100;
            // how much can be sent into the cell
        }
        for(int i = myCells.size() - 1; i >= 0 ; i--)
        {
            Coordinates cell = myCells.get(i);
            int currentDistance = distances[cell.getX()][cell.getY()];
            List<MovementCommand.Direction> lowerNeighbours = Util.getDirectionWhere(dir-> {
               Coordinates neigh = cell.getNeighbour(dir);
               return distances[neigh.getX()][neigh.getY()] < currentDistance;
            });
            lowerNeighbours.sort(Comparator.comparingInt(dir -> {
                Coordinates neigh = cell.getNeighbour(dir);
                return incomingCapacityAvailable[neigh.getX()][neigh.getY()];
            }));
            int currentValue = universeView.getPopulation(cell) +
                    Math.min(potentialIncoming[cell.getX()][cell.getY()] - 5, 0);
            for(int j = 0; j < lowerNeighbours.size(); j++)
            {
                Coordinates neighbour = cell.getNeighbour(lowerNeighbours.get(j));
                int toTransfer = Math.min(incomingCapacityAvailable[neighbour.getX()][neighbour.getY()],
                            currentValue / (lowerNeighbours.size() - j));
                for(int k = j ; k < lowerNeighbours.size(); k++)
                {
                    currentValue -= toTransfer;
                    potentialOutgoing[cell.getX()][cell.getY()][lowerNeighbours.get(k).ordinal()] += toTransfer;
                    incomingCapacityAvailable[neighbour.getX()][neighbour.getY()] -= toTransfer;
                    potentialIncoming[neighbour.getX()][neighbour.getY()] += toTransfer;
                }
            }
        }
        for(Coordinates cell: myCells)
        {
            for(MovementCommand.Direction dir : MovementCommand.Direction.values())
            {
                int outgoing = potentialOutgoing[cell.getX()][cell.getY()][dir.ordinal()];
                if(outgoing > 0)
                {
                    commandList.add(new MovementCommand(cell, dir, outgoing));
                }
            }
        }
    }

}
