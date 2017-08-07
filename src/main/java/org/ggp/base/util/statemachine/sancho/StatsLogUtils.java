package org.ggp.base.util.statemachine.sancho;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for logging statistics.
 */
public class StatsLogUtils
{
  /**
   * Graph types in the visualisation.
   */
  public static enum Graph
  {
    /**
     * Null-graph.  Used for series that aren't directly plotted.
     */
    NONE,

    /**
     * Expected score.
     */
    SCORE,

    /**
     * Memory usage.
     */
    MEM,

    /**
     * Node pool usage.
     */
    NODE,

    /**
     * GC time.
     */
    GC,

    /**
     * Performance.
     */
    PERF,

    /**
     * Iteration timings.
     */
    TIMES,
  }

  /**
   * Series types.
   */
  public static enum SeriesType
  {
    /**
     * Raw series.  Y-values plotted are the recorded y-values.
     */
    RAW,

    /**
     * Difference series.  Y-values plotted are the difference between the consecutive recorded y-values.  So, a series
     * with y-values 1,2,2,3,4 would be plotted as -,1,0,1,1.
     */
    DIFF,

    /**
     * Rate series.  Y-values plotted are the difference between the consecutive recorded y-values divided by the
     * difference between the corresponding x-values.
     */
    RATE;
  }

  /**
   * Statistics series for logging.
   */
  public static enum Series
  {
    /**
     * Expected score.
     */
    SCORE           (Graph.SCORE, 0, SeriesType.RAW,  true,  "Expected score"),

    /**
     * Currently used heap (bytes).
     */
    MEM_USED        (Graph.MEM,   0, SeriesType.RAW,  false, "Mem used"),

    /**
     * Memory allocated from the O.S. for heap usage (bytes).
     */
    MEM_COMMITTED   (Graph.MEM,   0, SeriesType.RAW,  false, "Mem committed"),

    /**
     * Maximum configured heap size (bytes).
     */
    MEM_MAX         (Graph.MEM,   0, SeriesType.RAW,  false, "Mem max"),

    /**
     * Number of MCTS tree nodes allocated.
     */
    POOL_USAGE      (Graph.MEM,   1, SeriesType.RAW,  false, "Pool usage"),

    /**
     * Allocation rate (100KB/s).
     */
    MEM_ALLOC_RATE  (Graph.MEM,   1, SeriesType.RATE, false, "Mem alloc. rate (100KB/s)"),

    /**
     * Garbage collection time (ms).
     */
    GC_TIME         (Graph.GC,    0, SeriesType.DIFF, false, "Time"),

    /**
     * Garbage collection count.
     */
    GC_COUNT        (Graph.GC,    1, SeriesType.DIFF, false, "Count"),

    /**
     * Node expansions.
     */
    NODE_EXPANSIONS (Graph.PERF,  0, SeriesType.RATE, false, "Node expansions"),

    /**
     * Sample rate (rollouts per node expansion).
     */
    SAMPLE_RATE     (Graph.PERF,  1, SeriesType.RAW,  true,  "Sample rate"),

    /**
     * Percentage of iteration time spent up to the end of the MCTS Select phase.
     */
    STACKED_SELECT  (Graph.TIMES, 0, SeriesType.RAW,  false, "Select"),

    /**
     * Percentage of iteration time spent up to the end of the MCTS Expand phase.
     */
    STACKED_EXPAND  (Graph.TIMES, 0, SeriesType.RAW,  false, "Expand"),

    /**
     * Percentage of iteration time spent up to the point where a pipeline slot is available.
     */
    STACKED_GET_SLOT(Graph.TIMES, 0, SeriesType.RAW,  false, "Get Slot"),

    /**
     * Percentage of iteration time spent up to the point of dequeuing requests for rollout.
     */
    STACKED_QUEUE   (Graph.TIMES, 0, SeriesType.RAW,  false, "Queue"),

    /**
     * Percentage of iteration time spent up to the end of the MCTS Rollout phase.
     */
    STACKED_ROLLOUT (Graph.TIMES, 0, SeriesType.RAW,  false, "Rollout"),

    /**
     * Percentage of iteration time spent up to the point of dequeuing requests for back-propagation.
     */
    STACKED_QUEUE2  (Graph.TIMES, 0, SeriesType.RAW,  false, "Queue2"),

    /**
     * Percentage of iteration time spent up to the end of the MCTS Back-Propagation phase.
     */
    STACKED_BACKPROP(Graph.TIMES, 0, SeriesType.RAW,  false, "Back-prop"),

    /**
     * Node expansions.
     */
    NODE_RE_EXPANSIONS(Graph.NODE,  0, SeriesType.RATE, false, "Node re-expansions"),

    /**
     * Node expansions.
     */
    TRANSITION_RATE (Graph.NODE,  1, SeriesType.RAW, false, "Transposition rate"),

    /**
     * The current turn (0 during meta-gaming).
     */
    TURN            (Graph.NONE,  1, SeriesType.RAW,  true,  "Turn");

    /**
     * Fixed data defining the series.
     */
    private final Graph      mGraph;
    private final int        mAxis;
    private final SeriesType mSeriesType;
    private final boolean    mStep;
    private final String     mName;

    /**
     * Temporary variables for converting series data.
     *
     * For use by the log summariser only.
     */
    private final Vector<Long>   mXValues;
    private final Vector<String> mYValues;
    private long                 mLastXValue;
    private long                 mLastYValue;

//    private static final Logger STATS_LOGGER = LogManager.getLogger("stats");

    private static final Pattern LINE_PATTERN = Pattern.compile("^([^,]+),(\\d+),(\\d+)");

    private Series(Graph xiGraph, int xiAxis, SeriesType xiSeriesType, boolean xiStep, String xiName)
    {
      mGraph      = xiGraph;
      mAxis       = xiAxis;
      mSeriesType = xiSeriesType;
      mStep       = xiStep;
      mName       = xiName;

      mXValues    = new Vector<>();
      mYValues    = new Vector<>();
      mLastXValue = 0;
      mLastYValue = 0;
    }

    /**
     * Log a data point.
     *
     * @param xiValue  - the y-value.
     */
    public void logDataPoint(long xiValue)
    {
      logDataPoint(System.currentTimeMillis(), xiValue);
    }

    /**
     * Log a data point.
     *
     * @param xiTime   - the x-value (usually a time in ms).
     * @param xiValue  - the y-value.
     */
    public void logDataPoint(long xiTime, long xiValue)
    {
      StringBuffer lLogBuf = new StringBuffer();
      logDataPoint(lLogBuf, xiTime, xiValue);
//      STATS_LOGGER.info(lLogBuf.toString());
    }

    /**
     * Log a data point.
     *
     * @param xiBuffer - the buffer to append to.
     * @param xiTime   - the x-value (usually a time in ms).
     * @param xiValue  - the y-value.
     */
    public void logDataPoint(StringBuffer xiBuffer, long xiTime, long xiValue)
    {
      xiBuffer.append(this);
      xiBuffer.append(',');
      xiBuffer.append(xiTime);
      xiBuffer.append(',');
      xiBuffer.append(xiValue);
      xiBuffer.append('\n');
    }

    /**
     * Load a data point into this series from file.
     *
     * For use by the log summariser only.
     *
     * @param lLine - the line in the file.
     */
    public static void loadDataPoint(String lLine)
    {
      Matcher lMatcher = LINE_PATTERN.matcher(lLine);
      if (lMatcher.matches())
      {
        try
        {
          Series lSeries = Series.valueOf(lMatcher.group(1));
          lSeries.addDataPoint(Long.parseLong(lMatcher.group(2)), Long.parseLong(lMatcher.group(3)));
        }
        catch (IllegalArgumentException lEx)
        {
          // Must be a data series from an old version - ignore.
        }
      }
    }

    private void addDataPoint(long xiXValue, long xiYValue)
    {
      assert(mXValues.size() == mYValues.size()) : mXValues.size() + " X-values but " + mYValues.size() + " Y-values";

      switch (mSeriesType)
      {
        case RAW:
        {
          mXValues.add(xiXValue);
          mYValues.add("" + xiYValue);
        }
        break;

        case DIFF:
        {
          mXValues.add(xiXValue);
          mYValues.add("" + (xiYValue - mLastYValue));
        }
        break;

        case RATE:
        {
          mXValues.add(xiXValue);
          mYValues.add((xiXValue == mLastXValue) ?
                                            "0" :
                                            "" + ((double)(xiYValue - mLastYValue) / (double)(xiXValue - mLastXValue)));
        }
        break;

        default:
          throw new IllegalStateException("Invalid series type: " + mSeriesType);
      }

      mLastXValue = xiXValue;
      mLastYValue = xiYValue;
    }

    /**
     * Append details of this Series to a JSON buffer.
     *
     * For use by the log summariser only.
     *
     * @param xiBuffer - the buffer.
     */
    public void appendToJSON(StringBuffer xiBuffer)
    {
      assert(mXValues.size() == mYValues.size()) : mXValues.size() + " X-values but " + mYValues.size() + " Y-values";

      xiBuffer.append("{\"showon\":\"");
      xiBuffer.append(mGraph);
      xiBuffer.append("\",\"type\":\"line\",\"name\":\"");
      xiBuffer.append(mName);
      xiBuffer.append("\",\"yAxis\":");
      xiBuffer.append(mAxis);
      if (mStep)
      {
        xiBuffer.append(",\"step\":\"left\"");
      }
      xiBuffer.append(",\"data\":[");
      int lStart = (mSeriesType == SeriesType.RAW) ? 0 : 1;
      for (int lii = lStart; lii < mXValues.size(); lii++)
      {
        xiBuffer.append('[');
        xiBuffer.append(mXValues.get(lii));
        xiBuffer.append(',');
        xiBuffer.append(mYValues.get(lii));
        xiBuffer.append(']');
        xiBuffer.append(',');
      }
      xiBuffer.setLength(xiBuffer.length() - 1);
      xiBuffer.append("]}");
    }

    /**
     * @return whether the series is empty.
     *
     * For use by the log summariser only.
     */
    public boolean isEmpty()
    {
      assert(mXValues.size() == mYValues.size()) : mXValues.size() + " X-values but " + mYValues.size() + " Y-values";

      int lThreshold = (mSeriesType == SeriesType.RAW) ? 1 : 2;
      return mXValues.size() < lThreshold;
    }

    /**
     * Clear all data points from the series.
     *
     * For use by the log summariser only.
     */
    public void reset()
    {
      mXValues.clear();
      mYValues.clear();
      mXValues.trimToSize();
      mYValues.trimToSize();
      mLastXValue = 0;
      mLastYValue = 0;

      assert(mXValues.size() == 0) : "X-values not cleared";
      assert(mYValues.size() == 0) : "Y-values not cleared";
    }
  }
}
