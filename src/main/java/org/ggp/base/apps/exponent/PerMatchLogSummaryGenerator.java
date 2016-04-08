package org.ggp.base.apps.exponent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ggp.base.util.logging.LogSummaryGenerator;
import org.ggp.base.util.logging.PerMatchTimeSeriesLogger;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PerMatchLogSummaryGenerator extends LogSummaryGenerator {
    private final int playerPort;

    public PerMatchLogSummaryGenerator(int playerPort) {
        this.playerPort = playerPort;
    }

    @Override
    public String getLogSummary(String matchId) {
        return getSummaryFromLogsDirectory(matchId);
    }

    //Returns info recorded by the perMatchLogger.
    @Override
    public String getSummaryFromLogsDirectory(String matchId) {
        // TODO Implement
        //First, load everything from the directory for the match
        File matchFolder = PerMatchTimeSeriesLogger.getMatchFolder(matchId, playerPort);
        //Wait a few seconds for everything to get written out, because we can...
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            //This is probably bad...
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        Map<String, Map<Long, Double>> timeSeries = loadAllTimeSeries(matchFolder);
        Map<String, List<DoublePair>> pointsTimeSeries = toPointsTimeSeries(timeSeries);

        return toJson(pointsTimeSeries);
    }

    private Map<String, List<DoublePair>> toPointsTimeSeries(
            Map<String, Map<Long, Double>> timeSeries) {
        Map<String, List<DoublePair>> result = Maps.newHashMap();
        for (String key : timeSeries.keySet()) {
            Map<Long, Double> series = timeSeries.get(key);
            result.put(key, toPointSeries(series));
        }
        return result;
    }

    private List<DoublePair> toPointSeries(Map<Long, Double> series) {
        List<DoublePair> result = Lists.newArrayList();
        for (Entry<Long, Double> entry : series.entrySet()) {
            result.add(new DoublePair(entry));
        }
        Collections.sort(result);
        return result;
    }

    private static class DoublePair implements Comparable<DoublePair> {
        public final long x;
        public final double y;
        public DoublePair(long x, double y) {
            this.x = x;
            this.y = y;
        }
        public DoublePair(Entry<Long, Double> entry) {
            this.x = entry.getKey();
            this.y = entry.getValue();
        }
        @Override
        public String toString() {
            return "[" + x + ", " + y + "]";
        }
        @Override
        public int compareTo(DoublePair o) {
            return Long.compare(x, o.x);
        }
    }

    private String toJson(Map<String, List<DoublePair>> pointsTimeSeries) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Entry<String, List<DoublePair>> ts : pointsTimeSeries.entrySet()) {
            sb.append("    \"");
            sb.append(ts.getKey());
            sb.append("\": ");
            sb.append(ts.getValue().toString());
            sb.append(",\n");
        }
        if (sb.length() > 3) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("\n}");
        return sb.toString();
    }

    private String toJsonOriginal(Map<String, List<Double>> timeSeries) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Entry<String, List<Double>> ts : timeSeries.entrySet()) {
            sb.append("    \"");
            sb.append(ts.getKey());
            sb.append("\": ");
            sb.append(ts.getValue().toString());
            sb.append(",\n");
        }
        if (sb.length() > 3) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("\n}");
        return sb.toString();
    }

    private Map<String, Map<Long, Double>> loadAllTimeSeries(File matchFolder) {
        Map<String, Map<Long, Double>> rawTimeSeries = Maps.newHashMap();
        // For now, all files should be time series
        for (File tsFile : matchFolder.listFiles()) {
            try {
                try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(tsFile)))) {
                    Map<Long, Double> recordedTimeSeries = (Map<Long, Double>) in.readObject();
                    rawTimeSeries.put(tsFile.getName().split("\\.")[0], recordedTimeSeries);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return toStandardizedTimeSeries(rawTimeSeries);
    }

    private Map<String, Map<Long, Double>> toStandardizedTimeSeries(
            Map<String, Map<Long, Double>> rawTimeSeries) {
        final long firstRecordedTime = getFirstRecordedTime(rawTimeSeries);
        return Maps.transformValues(rawTimeSeries, new Function<Map<Long, Double>, Map<Long, Double>>() {
            @Override
            public Map<Long, Double> apply(Map<Long, Double> input) {
                Map<Long, Double> newMap = Maps.newHashMapWithExpectedSize(input.size());
                for (long key : input.keySet()) {
                    newMap.put(key - firstRecordedTime, input.get(key));
                }
                return newMap;
            }
        });
    }

    private Map<String, List<Double>> toSynchronizedTimeSeries(
            Map<String, Map<Long, Double>> rawTimeSeries) {
        long firstRecordedTime = getFirstRecordedTime(rawTimeSeries);
        Map<String, List<Double>> results = Maps.newHashMap();
        for (String key : rawTimeSeries.keySet()) {
            Map<Long, Double> rawSeries = rawTimeSeries.get(key);
            List<Double> synchronizedTimeSeries = getSynchronizedTimeSeries(firstRecordedTime, rawSeries);
            results.put(key, synchronizedTimeSeries);
        }
        return results;
    }

    private List<Double> getSynchronizedTimeSeries(long firstRecordedTime,
            Map<Long, Double> rawSeries) {
        List<Double> synchronizedTimeSeries = Lists.newArrayList();
        for (Entry<Long, Double> entry : rawSeries.entrySet()) {
            long time = entry.getKey();
            int newIndex = (int) (time - (firstRecordedTime - 500)) / 1000;
            if (newIndex < 0) {
                throw new IllegalStateException("This should never happen");
            }
            addAtIndex(synchronizedTimeSeries, newIndex, entry.getValue());
        }
        return synchronizedTimeSeries;
    }

    private void addAtIndex(List<Double> synchronizedTimeSeries, int newIndex,
            double value) {
        while (newIndex > synchronizedTimeSeries.size()) {
            if (synchronizedTimeSeries.isEmpty()) {
                synchronizedTimeSeries.add(0.0);
            } else {
                double lastElem = synchronizedTimeSeries.get(synchronizedTimeSeries.size() - 1);
                synchronizedTimeSeries.add(lastElem);
            }
        }
        if (newIndex == synchronizedTimeSeries.size()) {
            synchronizedTimeSeries.add(value);
        } else {
            synchronizedTimeSeries.set(newIndex, value);
        }
    }

    private long getFirstRecordedTime(
            Map<String, Map<Long, Double>> rawTimeSeries) {
        long firstTime = Long.MAX_VALUE;
        for (Map<Long, Double> series : rawTimeSeries.values()) {
            for (long time : series.keySet()) {
                if (time < firstTime) {
                    firstTime = time;
                }
            }
        }
        return firstTime;
    }

    public static void main(String[] args) {
        LogSummaryGenerator gen = new PerMatchLogSummaryGenerator(9147);
        System.out.println(gen.getLogSummary("tiltyard.3pConnectFourv0.1359995009937"));
    }
}
