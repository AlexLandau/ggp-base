package org.ggp.base.util.ruleengine;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.ggp.base.util.GoalTuplePool;
import org.ggp.base.util.GoalTuplePool.GoalTuplePoolNode;
import org.ggp.base.util.ImmutableIntArray;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.Role;

import com.google.common.collect.Lists;


//Intended as a replacement for StateMachine.
//TODO: Remove old exception types? GameDescriptionException?
public interface RuleEngine<Move, State extends RuleEngineState<Move, State>> {

    State getInitialState();

    int getNumRoles();

    List<Role> getRoles();

    boolean isTerminal(State state);

    int getGoal(State state, int roleIndex) throws GameDescriptionException;

    default ImmutableIntArray getGoals(State state) throws GameDescriptionException {
        GoalTuplePoolNode curNode = GoalTuplePool.getInitialNode();
        for (int r = 0; r < getNumRoles(); r++) {
            curNode = curNode.get(getGoal(state, r));
        }
        return curNode.getArray();
    }

    List<Move> getLegalMoves(State state, int roleIndex) throws GameDescriptionException;

    State getNextState(State state, List<Move> jointMoves) throws GameDescriptionException;

    Translator<Move, State> getTranslator();

    @SuppressWarnings("unchecked") //relies on assumptions about Translators
    default State toNativeState(RuleEngineState<?, ?> otherState) {
        if (otherState.getTranslator() == getTranslator()) {
            return (State) otherState;
        }
        Set<GdlSentence> gdlState = otherState.toGdlState();
        return getTranslator().getNativeState(gdlState);
    }

    //TODO: Make this private somehow
    static final Random RANDOM = new Random();
    default List<Move> getRandomJointMove(State state) throws GameDescriptionException {
        int numRoles = getNumRoles();
        List<Move> jointMove = Lists.newArrayListWithCapacity(numRoles);
        for (int r = 0; r < numRoles; r++) {
            List<Move> legalMoves = getLegalMoves(state, r);
            if (legalMoves.size() == 1) {
                jointMove.add(legalMoves.get(0));
            } else {
                int chosenIndex = RANDOM.nextInt(legalMoves.size());
                jointMove.add(legalMoves.get(chosenIndex));
            }
        }
        return jointMove;
    }


    default List<List<Move>> getLegalMovesByRole(State state) throws GameDescriptionException {
        List<List<Move>> legalMovesByRole = Lists.newArrayListWithCapacity(getNumRoles());
        for (int r = 0; r < getNumRoles(); r++) {
            legalMovesByRole.add(getLegalMoves(state, r));
        }
        return legalMovesByRole;
    }

    default List<List<GdlTerm>> getGdlLegalMovesByRole(State state) throws GameDescriptionException {
        List<List<GdlTerm>> legalMovesByRole = Lists.newArrayListWithCapacity(getNumRoles());
        for (int r = 0; r < getNumRoles(); r++) {
            legalMovesByRole.add(getTranslator().getGdlMoves(getLegalMoves(state, r)));
        }
        return legalMovesByRole;
    }

    default ImmutableIntArray doRandomPlayout(State state) throws GameDescriptionException {
        while (!isTerminal(state)) {
            state = getRandomNextState(state);
        }
        return getGoals(state);
    }

    default State getRandomNextState(State state) throws GameDescriptionException {
        List<Move> jointMove = getRandomJointMove(state);
        return getNextState(state, jointMove);
    }

//    default DepthChargeBatchResult performDepthChargeBatch(State state)
//            throws GameDescriptionException {
//        ImmutableIntArray result = doRandomPlayout(state);
//        return new FlatDepthChargeResult(result, 1);
//    }
}
