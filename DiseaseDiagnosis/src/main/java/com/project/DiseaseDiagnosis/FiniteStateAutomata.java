package com.project.DiseaseDiagnosis;

import java.lang.String;
import java.util.*;

/**
 * Class FiniteStateAutomata is used by main method for constructing finite state machine. It
 * returns transition map, output map, alphabet and failure map.
 */
public class FiniteStateAutomata {

    private final Map<Key, Integer> transitionMap = new LinkedHashMap<>();
    private final Map<Integer, List<String>> outputMap = new LinkedHashMap<>();
    private final List<Character> alphabet = new ArrayList<>();
    private final Map<Integer, Integer> failureMap = new LinkedHashMap<>();

    //side list of states used for constructing failure map
    private final LinkedList<Integer> listOfStates = new LinkedList<>();
    int newState = 0;

    /**
     * Method gotoFunction calls method enterKeyword for all input keywords. After that,
     * it completes constructing transition map. Transitions are created
     * based on all keywords.
     *
     * @param keywords array of input keywords
     */
    public void gotoFunction(String[] keywords) {
        for (String keyword : keywords) {
            enterKeyword(keyword);
        }
        for (char symbol : alphabet) {

            /**
             * For all input symbols where transitionMap (0,input symbol) -> fail,
             * put transitionMap (0,input symbol) -> 0.
             */
            Key key = new Key(0, symbol);
            if (!transitionMap.containsKey(key)) {
                transitionMap.put(key, 0);
            }
        }
    }

    /**
     * Method enterKeyword is a side method used by gotoFunction. It constructs transition map and
     * output map from keywords.
     *
     * @param keyword .
     */
    public void enterKeyword(String keyword) {
        for (int i = 0; i < keyword.length(); i++) {
            if (!alphabet.contains(keyword.charAt(i))) {
                alphabet.add(keyword.charAt(i));
            }
        }

        /* First we find the longest keyword's prefix already defined */
        int state = 0;
        int currIndex = 0;
        Key key = new Key(state, keyword.charAt(currIndex));
        while (transitionMap.containsKey(key) && currIndex < keyword.length()) {
            state = transitionMap.get(key);
            currIndex = currIndex + 1;
            key.state = state;
            if(currIndex < keyword.length()) {
                key.symbol = keyword.charAt(currIndex);
            }
        }

        /* Define transitions for the rest of the keyword */
        for (int i = currIndex; i < keyword.length(); i++) {
            newState = newState + 1;
            Key currKey = new Key(state, keyword.charAt(i));
            transitionMap.put(currKey, newState);
            state = newState;
        }

        List<String> listOfKeyword = new ArrayList<>();
        listOfKeyword.add(keyword);
        outputMap.put(state, listOfKeyword);
    }

    /**
     * Method failureFunction constructs failure map according to Aho-Corasick algorithm.
     * Failure map is consulted when there is no defined transition for a certain pair (state, input symbol)
     * in the transition map. Every state has its own failure state. Only start state (0)
     * doesn't have defined failure transition - start state has transition back into 0 for all
     * undefined transitions (0,a).
     */
    public void failureFunction() {

        /**
         * Starts with states s that have defined transitions (0, input symbol) -> s. Those states
         * have f(s) = 0, their failure state is start state 0. All failure functions for other states
         * are generated based on states that have smaller depth.
         * Depth(s) - number of transitions from 0 to s
         */
        for (char symbol : alphabet) {
            Key key = new Key(0, symbol);
            int nextState = transitionMap.get(key);
            if (nextState != 0) {
                listOfStates.addLast(nextState);
                failureMap.put(nextState, 0);
            }
        }

        while (!listOfStates.isEmpty()) {
            int currState = listOfStates.removeFirst();
            for (char symbol : alphabet) {
                Key key = new Key(currState, symbol);
                if (transitionMap.containsKey(key)) {
                    int nextState = transitionMap.get(key);
                    listOfStates.addLast(nextState);
                    int failureState = failureMap.get(currState);
                    while (!transitionMap.containsKey(new Key(failureState, symbol))) {
                        failureState = failureMap.get(failureState);
                    }
                    Key k = new Key(failureState, symbol);
                    failureMap.put(nextState, transitionMap.get(k));

                    /*In output map, every state, except its own keyword,
                    now gets keyword of its failure state.*/
                    List<String> list1 = outputMap.get(nextState);
                    List<String> list2 = outputMap.get(transitionMap.get(k));
                    if (list1 != null && list2 != null) {
                        list1.addAll(list2);
                    } else if (list1 == null && list2 != null) {
                        outputMap.remove(nextState);
                        outputMap.put(nextState, list2);
                    }
                }
            }
        }
    }

    public Map<Key, Integer> getTransitionMap() {
        return transitionMap;
    }

    public Map<Integer, List<String>> getOutputMap() {
        return outputMap;
    }

    public List<Character> getAlphabet() {
        return alphabet;
    }

    public Map<Integer, Integer> getFailureMap() {
        return failureMap;
    }
}
