package gridwars.starter;

import cern.ais.gridwars.api.Coordinates;
import cern.ais.gridwars.api.UniverseView;
import cern.ais.gridwars.api.command.MovementCommand;

import java.util.Arrays;
import java.util.List;
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
}
