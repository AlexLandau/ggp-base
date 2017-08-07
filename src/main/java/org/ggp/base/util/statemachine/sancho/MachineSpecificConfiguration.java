package org.ggp.base.util.statemachine.sancho;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Class giving access to machine-specific configuration.
 */
public class MachineSpecificConfiguration
{
//  private static final Logger LOGGER = LogManager.getLogger();

  /**
   * Available configuration items.
   */
  public static enum CfgItem
  {
    /**
     * Player name to report.
     */
    PLAYER_NAME("Sancho 1.61c"),

    /**
     * Location of the working copy - used for fine-grained version logging.
     */
    WC_LOCATION(null),

    /**
     * The number of CPU-intensive threads to use.  By default, we calculate based on the number of available CPUs.
     */
    CPU_INTENSIVE_THREADS(-1),

    /**
     * Whether to bind CPU-intensive threads to vCPUs.
     */
    USE_AFFINITY(true),

    /**
     * Safety margin for submitting moves, in milliseconds.
     */
    SAFETY_MARGIN(2500),

    /**
     * Whether to disable the use of piece heuristics
     */
    DISABLE_PIECE_HEURISTIC(false),

    /**
     * Whether to disable the use of state-similarity detection for node expansion weighting.
     */
    DISABLE_STATE_SIMILARITY_EXPANSION_WEIGHTING(true),

    /**
     * Whether to periodically normalize tree node scores.
     */
    USE_NODE_SCORE_NORMALIZATION(true),

    /**
     * Whether to disable the use of learning (both using stuff already learned and also learning new stuff).
     */
    DISABLE_LEARNING(false),

    /**
     * Whether to disable the use of persisted puzzle plans (auto disabled if learning if disabled).
     */
    DISABLE_SAVED_PLANS(false),

    /**
     * Whether to disable node trimming on pool full (if disabled search stalls until the next move
     * reroots the tree).
     */
    DISABLE_NODE_TRIMMING(false),

    /**
     * Whether to disable greedy rollouts.
     */
    DISABLE_GREEDY_ROLLOUTS(false),

    /**
     * Whether to disable A* for puzzles.
     */
    DISABLE_A_STAR(false),

    /**
     * If positive, used as the rollout sample size, without any dynamic tuning.  Otherwise, dynamically determine the
     * best number of samples per rollout request.
     */
    FIXED_SAMPLE_SIZE(-1),

    /**
     * Explicit transposition table size to use (aka max node count)
     */
    NODE_TABLE_SIZE(2000000),

    /**
     * Whether to enable initial node estimation in all games or just those with negatively latched goals.
     */
    ENABLE_INITIAL_NODE_ESTIMATION(false),

    /**
     * Whether to use UCB tuned (default) or just UCB.
     */
    USE_UCB_TUNED(true),

    /**
     * Whether to use local search.
     */
    USE_LOCAL_SEARCH(true),

    /**
     * Whether RAVE may be used.
     */
    ALLOW_RAVE(true),

    /**
     * Whether hyper-expansion may be used.
     */
    ALLOW_HYPEREXPANSION(true),

    /**
     * Time, in milliseconds, after which we assume that we aren't going to here from the server again - in which case
     * we abort the match.
     */
    DEAD_MATCH_INTERVAL((int)TimeUnit.MINUTES.toMillis(10)),

    /**
     * The tlk.io channel to log to.
     */
    TLKIO_CHANNEL("sanchoggp"),

    /**
     * Full path of file to dump tree to after each move
     */
    TREE_DUMP(null),

    /**
     * Limit on number of MCTS expansions to perform per turn (for testing)
     */
    MAX_ITERATIONS_PER_TURN(-1);


    /**
     * Default value, as a string.
     */
    public final String mDefault;

    private CfgItem(String xiDefault)
    {
      mDefault = xiDefault;
    }

    private CfgItem(int xiDefault)
    {
      mDefault = "" + xiDefault;
    }

    private CfgItem(boolean xiDefault)
    {
      mDefault = xiDefault ? "true" : "false";
    }
  }

  /**
   * Reserved value for TLKIO_CHANNEL which disabled chat integration
   */
  public final static String NO_TLK_CHANNEL = "NONE";

  private static final Properties MACHINE_PROPERTIES = new Properties();
  static
  {
    // Computer is identified by the COMPUTERNAME environment variable (Windows) or HOSTNAME (Linux).
    String lComputerName = System.getenv("COMPUTERNAME");
    if (lComputerName == null)
    {
      lComputerName = System.getenv("HOSTNAME");
    }

    // Special-case for snap-ci, which uses a variety of hosts, but all starting "ct-".
    if ((lComputerName != null) && (lComputerName.startsWith("ct-")))
    {
      lComputerName = "snap-ci";
    }

    if (lComputerName != null)
    {
      try (InputStream lPropStream = new FileInputStream("data/cfg/" + lComputerName + ".properties"))
      {
        MACHINE_PROPERTIES.load(lPropStream);
      }
      catch (IOException lEx)
      {
        System.err.println("Missing/invalid machine-specific configuration for " + lComputerName);
      }
    }
    else
    {
      System.err.println("Failed to identify computer name - no environment variable COMPUTERNAME or HOSTNAME");
    }
  }

  /**
   * @return the specified String configuration value, or the default if not configured.
   *
   * @param xiKey
   */
  public static String getCfgStr(CfgItem xiKey)
  {
    return (MACHINE_PROPERTIES.getProperty(xiKey.toString(), xiKey.mDefault));
  }

  /**
   * @return the specified integer configuration value, or the default if not configured.
   *
   * @param xiKey
   */
  public static int getCfgInt(CfgItem xiKey)
  {
    return Integer.parseInt(getCfgStr(xiKey));
  }

  /**
   * @return the specified boolean configuration value, or the default if not configured.
   *
   * @param xiKey
   */
  public static boolean getCfgBool(CfgItem xiKey)
  {
    return Boolean.parseBoolean(getCfgStr(xiKey));
  }

  /**
   * Log all machine-specific configuration.
   */
  public static void logConfig()
  {
//    LOGGER.info("Running with machine-specific properties:");
    for (Entry<Object, Object> e : MACHINE_PROPERTIES.entrySet())
    {
      // Get the key.
      String lKey = (String)e.getKey();

      // Check that this is a known configuration parameter (and not a typo in the config file).
      try
      {
        CfgItem lItem = CfgItem.valueOf(lKey);
//        LOGGER.info("\t" + lKey + " = " + e.getValue() + " (default: " + lItem.mDefault + ")");
      }
      catch (IllegalArgumentException lEx)
      {
//        LOGGER.warn("Unknown configuration parameter: '" + lKey + "'");
      }
    }
  }


  /**
   * UT-only method for overriding configuration.
   *
   * @param xiKey - the property to override.
   * @param xiValue - the new value.
   */
  public static void utOverrideCfgVal(CfgItem xiKey, boolean xiValue)
  {
    MACHINE_PROPERTIES.setProperty(xiKey.toString(), xiValue ? "true" : "false");
  }
}
