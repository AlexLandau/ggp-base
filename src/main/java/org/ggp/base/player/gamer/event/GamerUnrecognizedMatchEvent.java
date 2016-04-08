package org.ggp.base.player.gamer.event;

import org.ggp.base.util.observer.Event;

public final class GamerUnrecognizedMatchEvent extends Event
{

    private final String matchId;

    public GamerUnrecognizedMatchEvent(String matchId)
    {
        this.matchId = matchId;
    }

    public String getMatchId()
    {
        return matchId;
    }

    @Override
    public String toString() {
        return "Gamer unrecognized match event: " + matchId;
    }
}
