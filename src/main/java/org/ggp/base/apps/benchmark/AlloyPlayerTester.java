package org.ggp.base.apps.benchmark;

import java.util.Map;

public class AlloyPlayerTester {

    public static void main(String[] args) throws Exception {
        Map<String, Double> benchmarkScores = PlayerTester.getBenchmarkScores("localhost:9147");
        System.out.println("Benchmark scores for the player on port 9147 are: " + benchmarkScores);
    }

}
