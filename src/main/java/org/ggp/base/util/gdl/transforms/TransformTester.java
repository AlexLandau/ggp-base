package org.ggp.base.util.gdl.transforms;

import java.util.List;

import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.statemachine.verifier.StateMachineVerifier;
import org.ggp.base.validator.StaticValidator;
import org.ggp.base.validator.ValidatorException;


/**
 *
 * @author Sam Schreiber
 *
 */
public class TransformTester {
    public static void main(String args[]) throws InterruptedException {

        final boolean showDiffs = false;
        final ProverStateMachine theReference = new ProverStateMachine();
        final ProverStateMachine theMachine = new ProverStateMachine();

        GameRepository theRepository = GameRepository.getDefaultRepository();
        for(String gameKey : theRepository.getGameKeys()) {
            GdlPool.drainPool();
            if(gameKey.equals("sudoku")) continue; //Probably unavoidable problem w/description
            if(!gameKey.equals("mummymaze1p")) continue; //Can be fixed by forward-restricting domains for variables in constants
            //We need to derive this over the entire description and then forward those restrictions to the model... hmm...
            if(gameKey.contains("laikLee")) continue;
            if(gameKey.contains("test_case_5")) continue;
            //if(gameKey.startsWith("test_case")) continue;
            List<Gdl> description = theRepository.getGame(gameKey).getRules();
            List<Gdl> newDescription = description;
            try {
                StaticValidator.validateDescription(description);
            } catch (ValidatorException e) {
                continue;
            }
            System.out.print(gameKey + ":   ");
            System.out.flush();


            // Choose the transformation(s) to test here
            //description = DeORer.run(description);
            //newDescription = VariableConstrainer.replaceFunctionValuedVariables(description);
            //newDescription = Relationizer.run(description);
            long startTime = System.currentTimeMillis();
            //            ConstantChecker cc = ConstantFinder.getConstants(newDescription);
            int constantCount = 0;
            //            for(SentenceForm form : cc.getConstantSentenceForms())
            //            	constantCount += cc.getTrueSentences(form).size();
            long time = System.currentTimeMillis() - startTime;
            System.out.println("Constants: "+constantCount+"; Time (ms): "+time+"");

            if(description.hashCode() != newDescription.hashCode()) {
                theReference.initialize(description);
                theMachine.initialize(newDescription);
                System.out.println("Detected activation in game " + gameKey + ". Checking consistency: ");
                StateMachineVerifier.checkMachineConsistency(theReference, theMachine, 10000);

                if(showDiffs) {
                    for(Gdl x : newDescription) {
                        if(!description.contains(x))
                            System.out.println("NEW: " + x);
                    }
                    for(Gdl x : description) {
                        if(!newDescription.contains(x))
                            System.out.println("OLD: " + x);
                    }
                }
            }
        }
    }
}
