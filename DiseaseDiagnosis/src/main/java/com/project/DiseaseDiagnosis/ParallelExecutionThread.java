package com.project.DiseaseDiagnosis;

import java.util.HashMap;

public class ParallelExecutionThread extends Thread {

    private final HashMap<String, Integer> countMap = new HashMap<>();

    private final String dataBlock;

    private final int threadNumber;

    public ParallelExecutionThread(int threadNumber, String dataBlock) {
        this.dataBlock = dataBlock;
        this.threadNumber = threadNumber;
    }

    public void run() {
        try {
            int currState = 0;
            for (int i = 0; i < dataBlock.length(); i++) {
                char symbol = dataBlock.charAt(i);
                boolean isNextStatePresent = false;
                Key key = new Key(currState, symbol);
                if (Main.transitionMap.containsKey(key)) {
                    currState = Main.transitionMap.get(key);
                    isNextStatePresent = true;
                }
                if (!isNextStatePresent) {
                    if (currState != 0) {
                        while (true) {
                            int failureState = Main.failureMap.get(currState);
                            key.state = failureState;
                            if (Main.transitionMap.containsKey(key)) {
                                currState = Main.transitionMap.get(key);
                                break;
                            } else {
                                currState = failureState;
                            }
                        }
                    }
                }
                if (Main.outputMap.containsKey(currState)) {
                    if (threadNumber == 0 || (i >= Main.maxLengthKeyword)) {
                        for (String pattern : Main.outputMap.get(currState)) {
                            if (countMap.containsKey(pattern)) {
                                int currCount = countMap.get(pattern);
                                countMap.remove(pattern);
                                countMap.put(pattern, currCount + 1);
                            } else {
                                countMap.put(pattern, 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, Integer> getCountMap() {
        return countMap;
    }
}
