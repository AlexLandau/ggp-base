package org.ggp.base.player.gamer.statemachine.human.event;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.statemachine.Move;

import com.google.common.collect.Lists;


public final class HumanNewMovesEvent extends Event
{

    private final List<Move> moves;
    private final Move selection;

    public HumanNewMovesEvent(List<Move> moves, Move selection)
    {
        List<Move> sortedMoves = Lists.newArrayList(moves);
        Collections.sort(sortedMoves, new Comparator<Move>(){@Override
            public int compare(Move o1, Move o2) {return o1.toString().compareTo(o2.toString());}});
        this.moves = sortedMoves;
        this.selection = selection;
    }

    public List<Move> getMoves()
    {
        return moves;
    }

    public Move getSelection()
    {
        return selection;
    }

}
