package com.project.DiseaseDiagnosis;

/**
 * This software is based on Aho-Corasick string searching algorithm which constructs a
 * finite state machine (DFA) based on input keywords. The algorithm locates elements of a finite set
 * of strings within an input DNA Sequence text and Diagnose chances of Diseases.
 * It also compares the efficiency of sequential and parallel approaches.
 */


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Class Main contains main method and helper methods. Also, it has transition map, output map, failure map and list
 * of characters named alphabet. Transition map and failure map are used in finite state machine and
 * output map is used for finding keywords in DNA Sequence.
 */
public class Main {

    /**
     * runtime is used to Calculate Memory used by the Program
     */
    private static final Runtime runtime = Runtime.getRuntime();

    /**
     * Initializing Required variables
     */
    private static String[] keywords = null;
    private static File keywordsFile = null;
    private static File textFile = null;
    private static BufferedReader bufferedReader = null;
    private static final int BLOCK_LENGTH = 20000;
    static int maxLengthKeyword;

    /**
     * transitionMap stores pairs (current state, input symbol) -> new state
     */
    static Map<Key, Integer> transitionMap = new LinkedHashMap<>();

    /**
     * outputMap defines states in which certain keywords are found
     */
    static Map<Integer, List<String>> outputMap = new LinkedHashMap<>();

    /**
     * failureMap stores pairs (current state -> failure state) according to Aho-Corasick algorithm
     */
    static Map<Integer, Integer> failureMap = new LinkedHashMap<>();

    /**
     * diseaseRanges Stores the Nucleotide Repeat Disease Ranges
     */
    private static final HashMap<String, HashMap<String, ArrayList<Pair>>> diseaseRanges = new HashMap<>();

    /**
     * alphabet stores all characters from keywords and DNA Sequence (i.e A, C, T, G)
     */
    private static List<Character> alphabet = new ArrayList<>();

    /**
     * Main method reads input keywords and DNA Sequence from two files. It produces both Terminal and file outputs:
     * Terminal - performance (time spent constructing DFA, time spent locating all keywords, memory usage)
     * file - Count of keywords that were located in the DNA Sequence and Disease Diagnosis results.
     */
    public String mainFunc(File dnaSequence) throws IOException, InterruptedException {

        readInputKeywords();
        String returnData = "";
        long startTime1 = System.nanoTime();        // Measuring time
        preprocessingStage();
        long currentTime1 = System.nanoTime() - startTime1;
        returnData += "Construction of finite state machine (DFA) is done in " + currentTime1 / 1000000.0 + " ms." + "\n";

        long startTime2 = System.nanoTime();
        returnData = sequentialProcessing(returnData, dnaSequence);
        long currentTime2 = System.nanoTime() - startTime2;
        returnData += "Finding all keywords in DNA Sequence(in Sequential Manner) is done in " + currentTime2 / 1000000.0 + " ms." + "\n";

        List<String> dataBlocks = divideDNASequenceIntoBlocks(dnaSequence);

        long startTime3 = System.nanoTime();
        returnData = parallelProcessing(dataBlocks, returnData);
        long currentTime3 = System.nanoTime() - startTime3;

        returnData += "Finding all keywords in DNA Sequence(in Parallel Manner) is done in " + currentTime3 / 1000000.0 + " ms." + "\n";

        int kib = 1024;

        returnData += "Total memory used: " + (runtime.totalMemory() - runtime.freeMemory()) / kib + " KiB." + "\n";

        return returnData;
    }

    /** This static block is used to load the data of nucleotide repeat diseases ranges into diseaseRanges map
     * which is used later for disease diagnosis */
    static {
        try {
            File diseaseRangesFile = new File("src/main/resources/NucleotideRepeatDiseaseRanges");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(diseaseRangesFile));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] contents = line.split(",");
                String diseaseName = contents[0];
                String pattern = contents[1];
                ArrayList<Pair> ranges = new ArrayList<>();
                for (int i = 2; i < 5; i++) {
                    Pair currPair = new Pair();
                    String[] range = contents[i].split("-");
                    currPair.setLow(Integer.parseInt(range[0]));
                    if (range[1].compareTo("inf") == 0) {
                        currPair.setHigh(Integer.MAX_VALUE);
                    } else {
                        currPair.setHigh(Integer.parseInt(range[1]));
                    }
                    ranges.add(currPair);
                }
                if (!diseaseRanges.containsKey(pattern)) {
                    HashMap<String, ArrayList<Pair>> newTable = new HashMap<>();
                    newTable.put(diseaseName, ranges);
                    diseaseRanges.put(pattern, newTable);
                } else {
                    assert !diseaseRanges.get(pattern).containsKey(diseaseName) : "Repeated Disease Names for the same pattern";
                    diseaseRanges.get(pattern).put(diseaseName, ranges);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("File with Nucleotide Repeat Diseases Ranges does not exist!");
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * readInputKeywords reads all the keywords from the input file. It also removes empty and duplicate keywords
     */
    private static void readInputKeywords() throws IOException {
        try {
            keywordsFile = new File("src/main/resources/keywords.txt");
            bufferedReader = new BufferedReader(new FileReader(keywordsFile));
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            stringBuilder.append(line);
            keywords = stringBuilder.toString().split(",");

            /* Remove empty strings and duplicates from keywords */
            List<String> list = new ArrayList<>(Arrays.asList(keywords));
            list.removeAll(Arrays.asList("", null));
            LinkedHashSet<String> ls = new LinkedHashSet<>(list);
            keywords = ls.toArray(new String[0]);

        } catch (FileNotFoundException e) {
            System.err.println("File with keywords does not exist!");
            System.exit(-1);
        }
        if (keywordsFile.length() == 0) {
            System.err.println("File with keywords cannot be empty!");
            System.exit(-1);
        }
    }

    /**
     * readDnaSequence loads the input DNA Sequence into a buffered reader
     */
    private static void readDnaSequence(File dnaSequence) {
        try {
            textFile = dnaSequence;
            bufferedReader = new BufferedReader(new FileReader(textFile));
        } catch (FileNotFoundException e) {
            System.err.println("File with DNA Sequence does not exist!");
            System.exit(-1);
        }
        if (textFile.length() == 0) {
            System.err.println("File with DNA Sequence cannot be empty!");
            System.exit(-1);
        }
    }

    /**
     * printOutput is used to print the Disease Diagnosis results by using countMap and comparing
     * it with the Nucleotide Repeat disease ranges
     */
    private static String printOutput(Map<String, Integer> countMap, String returnData) {
        returnData += String.format("%15s %15s %15s %30s \n", "Keyword", "Count", "Disease Name", "Disease Diagnosis Result");
        for (Map.Entry<String, Integer> i : countMap.entrySet()) {
            String pattern = i.getKey();
            int count = i.getValue();
            if (diseaseRanges.containsKey(pattern)) {
                for (Map.Entry<String, ArrayList<Pair>> data : diseaseRanges.get(pattern).entrySet()) {
                    String diseaseName = data.getKey();
                    ArrayList<Pair> ranges = data.getValue();
                    String[] results = {"NormalRange", "Pre-mutedRange", "DiseaseAffected"};
                    int result = 3;
                    for (int j = 0; (j < ranges.size()) && result == 3; j++) {
                        if (count >= ranges.get(j).getLow() && count <= ranges.get(j).getHigh()) {
                            result = j;
                        }
                    }
                    if (result < 3) {
                        returnData += String.format("%15s %15s %15s %30s \n", pattern, count, diseaseName, results[result]);
                    }
                }
            }
        }
        return returnData;
    }

    /**
     * divideDNASequenceIntoBlocks splits the input DNA sequence into blocks. The blocks have to
     * partially overlap to allow pattern-matching across a boundary, which is equal to the
     * length of the longest pattern in the dictionary minus 1 character.
     */
    private static List<String> divideDNASequenceIntoBlocks(File dnaSequence) throws IOException {
        readDnaSequence(dnaSequence);
        List<String> dataBlocks = new ArrayList<>();
        int charsRead;
        char[] characters = new char[BLOCK_LENGTH];
        while ((charsRead = bufferedReader.read(characters, 0, BLOCK_LENGTH)) != -1) {
            String currBlock = new String(characters, 0, charsRead);
            currBlock = currBlock.replaceAll("\\r|\\n", "");
            dataBlocks.add(currBlock);
        }
        maxLengthKeyword = getMaximumLengthKeyword();
        for (int i = 0; i < dataBlocks.size() - 1; i++) {
            String currBlock = dataBlocks.get(i);
            currBlock += dataBlocks.get(i + 1).substring(0, Math.min(maxLengthKeyword - 1, dataBlocks.get(i + 1).length()));
            dataBlocks.set(i, currBlock);
        }
        return dataBlocks;
    }

    /**
     * getMaximumLengthKeyword
     *
     * @return The length of the longest keyword
     */
    private static int getMaximumLengthKeyword() {
        int ans = 0;
        for (String keyword : keywords) {
            if (keyword.length() > ans) {
                ans = keyword.length();
            }
        }
        return ans;
    }

    /**
     * preprocessingStage is used to construct a Finite state automata for pattern matching from the
     * set of keywords and adding failure links
     */
    private static void preprocessingStage() {
        FiniteStateAutomata finiteStateAutomata = new FiniteStateAutomata();
        finiteStateAutomata.gotoFunction(keywords);
        finiteStateAutomata.failureFunction();
        transitionMap = finiteStateAutomata.getTransitionMap();
        outputMap = finiteStateAutomata.getOutputMap();
        alphabet = finiteStateAutomata.getAlphabet();
        failureMap = finiteStateAutomata.getFailureMap();
    }

    /**
     * Finding all occurrences of keywords in DNA Sequence using finite state machine (DFA) in a sequential manner
     * Reading input text line by line. This way, instead of working with whole DNA Sequence at once,
     * we process only one line at a time (send it to constructed DFA). It saves a lot of memory.
     */
    private static String sequentialProcessing(String returnData,File dnaSequence1) throws IOException {
        readDnaSequence(dnaSequence1);
        int currState = 0, position = 0;
        HashMap<String, Integer> countMap = new HashMap<>();
        String dnaSequence;
        while ((dnaSequence = bufferedReader.readLine()) != null) {
            for (int i = 0; i < dnaSequence.length(); i++) {
                char symbol = dnaSequence.charAt(i);
                boolean isNextStatePresent = false;
                Key key = new Key(currState, symbol);
                if (transitionMap.containsKey(key)) {
                    currState = transitionMap.get(key);
                    isNextStatePresent = true;
                }
                if (!isNextStatePresent) {
                    if (!alphabet.contains(symbol)) {
                        alphabet.add(symbol);
                        Key key1 = new Key(0, symbol);
                        transitionMap.put(key1, 0);
                    }
                    if (currState != 0) {        /*if state == 0, algorithm stays in state 0*/
                        while (true) {
                            int failureState = failureMap.get(currState);
                            key.state = failureState;
                            if (transitionMap.containsKey(key)) {
                                currState = transitionMap.get(key);
                                break;
                            } else {
                                currState = failureState;
                            }
                        }
                    }
                }
                if (outputMap.containsKey(currState)) {
                    for (String pattern : outputMap.get(currState)) {
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
            position = position + dnaSequence.length();
        }
        returnData = printOutput(countMap, returnData);
        return  returnData;
    }

    /**
     * Finding all occurrences of keywords in DNA Sequence using finite state machine (DFA)
     * by dividing the DNA sequence into different blocks and computing each block parallel
     */
    private static String parallelProcessing(List<String> dataBlocks, String returnData) throws IOException, InterruptedException {
        ParallelExecutionThread[] threads = new ParallelExecutionThread[dataBlocks.size()];
        for (int i = 0; i < dataBlocks.size(); i++) {
            threads[i] = new ParallelExecutionThread(i, dataBlocks.get(i));
            threads[i].start();
        }
        HashMap<String, Integer> countMap = new HashMap<>();
        for (int i = 0; i < dataBlocks.size(); i++) {
            threads[i].join();
            HashMap<String, Integer> currCountMap = threads[i].getCountMap();
            for (String keyword : keywords) {
                if (currCountMap.containsKey(keyword)) {
                    int count = currCountMap.get(keyword);
                    if (countMap.containsKey(keyword)) {
                        int currCount = countMap.get(keyword);
                        countMap.remove(keyword);
                        countMap.put(keyword, currCount + count);
                    } else {
                        countMap.put(keyword, count);
                    }
                }
            }
        }
        returnData = printOutput(countMap, returnData);
        return returnData;
    }

}
