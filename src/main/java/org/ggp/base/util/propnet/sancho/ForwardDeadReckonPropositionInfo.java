
package org.ggp.base.util.propnet.sancho;

import org.ggp.base.util.gdl.grammar.GdlSentence;

/**
 * @author steve
 *  Meta information about a base proposition
 */
public class ForwardDeadReckonPropositionInfo
{
  /**
   * GDL name of this proposition
   */
  public GdlSentence                  sentence;
  /**
   * Index in the owning propnet's master proposition list
   */
  public int                          index;
}
