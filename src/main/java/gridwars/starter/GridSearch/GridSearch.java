package gridwars.starter.GridSearch;

import cern.ais.gridwars.HeadlessRunner;
import cern.ais.gridwars.api.bot.PlayerBot;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GridSearch {

    private static final int rounds = 5;
    private static final int groupSize = 10;
    public static Map<String, Object> run(Map<String, List<Object>> hyperparameterSpace,
                                          Function<Map<String, Object>, Supplier<? extends PlayerBot>>  botGenerator,
                                          List<Supplier<? extends PlayerBot>> initialBots)
    {
        var spaceCopy = new HashMap<>(hyperparameterSpace);
        var currentParameterSet = expandProduct(spaceCopy)
                .filter(params ->
                        initialBots
                                .stream()
                                .flatMap(sup -> IntStream.range(0, rounds).mapToObj(_ -> sup))
                                .parallel()
                                .mapToDouble(testingBot ->
                                        botWon(botGenerator.apply(params), testingBot)).average().orElse(0) > 0.5)
                .collect(Collectors.toList());
        while(currentParameterSet.size() > 1)
        {
            List<List<Map<String,Object>>> splits = new LinkedList<>();
            for(int i=0;i<currentParameterSet.size();i+= groupSize)
            {
                int j = Math.min(i + groupSize, currentParameterSet.size());
                splits.add(currentParameterSet.subList(i, j));
            }
            currentParameterSet = splits
                    .stream()
                    .parallel()
                    .map(list -> processGroup(list, botGenerator))
                    .collect(Collectors.toList());
        }
        return currentParameterSet.get(0);
    }
    private static Map<String, Object> processGroup(List<Map<String, Object>> group,
                                                    Function<Map<String, Object>, Supplier<? extends PlayerBot>>  botGenerator)
    {
        var winningBots = IntStream.range(0, group.size()).boxed()
                .flatMap(i -> IntStream.range(i+1, group.size()).boxed()
                        .map(j -> new int[]{i, j}))
                .flatMap(arr -> IntStream.range(0, rounds).boxed()
                        .map(_ -> arr))
                .parallel()
                .map(arr -> {
                    try {
                        int i = arr[0];
                        int j = arr[1];
                        PlayerBot redBot = botGenerator.apply(group.get(i)).get();
                        PlayerBot blueBot = botGenerator.apply(group.get(j)).get();


                        var result=  HeadlessRunner.runGame(blueBot, redBot, "./bot2.log", "./bot1.log");
                        if(result == HeadlessRunner.GAME_RESULT.RED_WON)
                            return i;
                        if(result == HeadlessRunner.GAME_RESULT.BLUE_WON)
                            return j;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return -1;
                }).filter(i -> i >= 0).collect(Collectors.toList());
        int[] counts = new int[group.size()];
        for(int id: winningBots)
        {
            counts[id]++;
        }
        return group.get(IntStream.range(0, group.size()).boxed().max(Comparator.comparingInt(x -> counts[x])).orElse(0));
    }

    private static Stream<Map<String,Object>> expandProduct(Map<String, List<Object>> space)
    {
        Optional<String> currentKeyOptional = space.keySet().stream().findAny();
        if(currentKeyOptional.isEmpty())
            return Stream.of(new HashMap<>());
        String currentKey = currentKeyOptional.get();
        Collection<Object> currentParameters = space.get(currentKey);
        space.remove(currentKey);
        return expandProduct(space).flatMap(
                parameters -> currentParameters.stream().map(
                        parameter ->
                        {
                            var copy = new HashMap<>(parameters);
                            copy.put(currentKey, parameter);
                            return copy;
                        })
                );
    }

    private static double botWon(Supplier<? extends PlayerBot> testedBot, Supplier<? extends PlayerBot> testingBot)
    {
        return HeadlessRunner.runGame(testedBot.get(), testingBot.get(), "./bot1.log", "./bot2.log")
                == HeadlessRunner.GAME_RESULT.BLUE_WON ? 1 : 0;
    }


}
