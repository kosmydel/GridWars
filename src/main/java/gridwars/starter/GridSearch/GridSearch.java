package gridwars.starter.GridSearch;

import cern.ais.gridwars.HeadlessRunner;
import cern.ais.gridwars.api.bot.PlayerBot;
import gridwars.starter.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GridSearch {

    private static final int rounds = 3;
    private static final int groupSize = 10;
    private static final double initial_threshold = 0.5;

    public static void main(String[] args) {
        var hyper = Map.of(
                "STRATEGY_CHANGE", List.of((Object)100, 105, 110, 115, 120, 125, 130, 135, 140, 145),
                "DENOMINATOR_VALUE", List.of((Object) 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 5.0, 7.0, 10.0),
                "LINEAR_TRANSFER_DENOMINATOR", List.of((Object)1.0,2.0,4.0,6.0,8.0,10.0),
                "LINEAR_TRANSFER_POPULATION_THRESHOLD", List.of((Object)10,20,30,40,50),
                "LINEAR_TRANSFER_TURN_THRESHOLD", List.of((Object)110,120,130,140,150,160),
                "BFS_GRAVITY_INCREMENT", List.of((Object)1,2,3,5,7)
        );
        var result = run(hyper,
                parameters -> (() -> new GluttonBot(
                        (Integer) parameters.get("STRATEGY_CHANGE"),
                        (Double) parameters.get("DENOMINATOR_VALUE"),
                        (Double) parameters.get("LINEAR_TRANSFER_DENOMINATOR"),
                        (Integer) parameters.get("LINEAR_TRANSFER_POPULATION_THRESHOLD"),
                        (Integer) parameters.get("LINEAR_TRANSFER_TURN_THRESHOLD"),
                        (Integer) parameters.get("BFS_GRAVITY_INCREMENT")
                )),
                List.of(GluttonBotPRO4::new,
                        GluttonBot_WorkingGravity6::new,
                        GluttonBot6::new));
        for(var param : result.entrySet())
        {
            System.out.println("["+ param.getKey()+ "]=" + param.getValue());
        }
    }
    public static Map<String, Object> run(Map<String, List<Object>> hyperparameterSpace,
                                          Function<Map<String, Object>, Supplier<? extends PlayerBot>>  botGenerator,
                                          List<Supplier<? extends PlayerBot>> initialBots)
    {
        var spaceCopy = new HashMap<>(hyperparameterSpace);
        var currentParameterSet = expandProduct(spaceCopy)
                .filter(params ->
                        initialBots
                                .stream()
                                .flatMap(sup -> IntStream.range(0, rounds).mapToObj(a -> sup))
                                .parallel()
                                .mapToDouble(testingBot ->
                                        botWon(botGenerator.apply(params), testingBot)).average().orElse(0) > initial_threshold)
                .collect(Collectors.toList());
        while(currentParameterSet.size() > 1)
        {
            System.out.println("cases left: " + currentParameterSet.size());
            List<List<Map<String,Object>>> splits = new LinkedList<>();
            for(int i=0;i<currentParameterSet.size();i+= groupSize)
            {
                int j = Math.min(i + groupSize, currentParameterSet.size());
                splits.add(currentParameterSet.subList(i, j));
            }
            currentParameterSet = IntStream.range(0, splits.size())
                    .boxed()
                    .map(index -> {
                        System.out.println("Processing group: " + index);
                        return processGroup(splits.get(index), botGenerator);
                    })
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
                        .map(a -> arr))
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

    private static Random random = new Random();
    private static double botWon(Supplier<? extends PlayerBot> testedBot, Supplier<? extends PlayerBot> testingBot)
    {
        if(random.nextBoolean())
        {
            return HeadlessRunner.runGame(testedBot.get(), testingBot.get(), "./bot1.log", "./bot2.log")
                    == HeadlessRunner.GAME_RESULT.BLUE_WON ? 1 : 0;
        }
        else
        {
            return HeadlessRunner.runGame(testingBot.get(), testedBot.get(), "./bot1.log", "./bot2.log")
                    == HeadlessRunner.GAME_RESULT.RED_WON ? 1 : 0;
        }
    }


}
