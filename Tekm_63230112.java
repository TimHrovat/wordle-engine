import java.util.*;
import java.util.Map.Entry;

/**
 * My wordle engine implementation is based on a tree structure which is
 * implemented with the {@link Node} class and is generated from the given
 * Set of words
 * 
 * Each node of the tree contains a character and a weight:
 * - character represents a letter of a word
 * - weight represents the likelihood of a character to be at the specific
 * position
 * - tree depth represents the position of the character (node) in the word
 * 
 * With each game a new first word is generated based on character repetitions
 * in the whole word list
 *
 * @author Tim Hrovat (student number: 63230112)
 */
public class Tekm_63230112 implements Stroj {
    /** only calculated once (used to reset the root node) */
    private Node originalRoot = new Node(null);

    /** changes during each iteration and resets at the start of each round */
    private Node root = new Node(null);

    /** contains letters that are at the correct position <position, char> */
    private HashMap<Integer, Character> confirmedLetters;
    private ArrayList<Character> wrongPosLetters;

    private ArrayList<HashMap<Character, Integer>> charRepsByPos = new ArrayList<>();
    private HashMap<Character, Integer> charReps = new HashMap<>();

    /**
     * the best starting word I found based on the number of letter repetitions in
     * all words at each position
     */
    private String firstGuess = "taerio";
    private String lastGuess;

    private Integer wordLength;
    private Integer wordListSize;

    public void inicializiraj(Set<String> words) {
        this.wordLength = words.iterator().next().length();
        this.wordListSize = words.size();

        // init charRepsByPos
        for (int i = 0; i < this.wordLength; i++) {
            this.charRepsByPos.add(new HashMap<>());
        }

        this.populateTree(words);

        // if we have words that do not have the lenght of 6
        if (this.wordLength != firstGuess.length()) {
            this.firstGuess = this.generateFirstGuess();
        }
    }

    public String poteza(List<Character> response) {
        // start of round
        if (response == null) {
            this.wordListSize--;
            this.root = originalRoot.copy();
            this.confirmedLetters = new HashMap<>();
            this.wrongPosLetters = new ArrayList<>();

            // last word in a list
            if (this.wordListSize == 0) {
                return this.lastGuess = this.root.generateGuess();
            }

            return this.lastGuess = this.firstGuess; // starting word
        }

        if (this.isCorrectGuess(response)) {
            /**
             * !!! IMPORTANT !!!
             *
             * REMOVE THIS LINE IF YOU WANT TO TEST ONE WORD MULTIPLE TIMES ON
             * THE SAME ENGINE INSTANCE
             *
             * {@link Node#removeWord(String)} method is only used because
             * {@link TestirajVse} allows it, it dramatically increases
             * performance because TestirajVse never tests the same word twice
             */
            this.originalRoot.removeWord(this.lastGuess);
            this.update();
            this.firstGuess = this.generateFirstGuess();

            return null;
        }

        this.parseResponse(response);

        return this.lastGuess = this.root.generateGuess(this.wrongPosLetters);
    }

    /**
     * updates character repetitions in order to be able to generate next first
     * guess
     */
    private void update() {
        char[] correctGuess = this.lastGuess.toCharArray();

        int index = 0;
        for (char chr : correctGuess) {
            this.updateCharReps(this.charReps, chr);
            this.updateCharReps(this.charRepsByPos.get(index), chr);

            index++;
        }
    }

    /** updates characters repetitions in given HashMap */
    private void updateCharReps(HashMap<Character, Integer> charReps, char chr) {
        Integer reps = charReps.get(chr) - 1;

        if (reps == 0) {
            charReps.remove(chr);
        } else {
            charReps.put(chr, reps);
        }
    }

    /**
     * generates next best first guess by looking at character repetitions in all
     * words and in each position
     */
    private String generateFirstGuess() {
        Character[] chars = this.mostUsedCharacters();
        List<Integer> fullIndexes = new ArrayList<>();
        Character[] res = new Character[this.wordLength];
        StringBuilder sb = new StringBuilder();

        if (chars == null) {
            // should not happen but just in case...
            return this.originalRoot.generateGuess();
        }

        for (Character chr : chars) {
            Integer max = 0;
            Integer maxIndex = null;

            for (int i = 0; i < this.wordLength; i++) {
                if (fullIndexes.contains(i)) {
                    continue;
                }

                Integer reps = this.charRepsByPos.get(i).getOrDefault(chr, 0);

                if (reps > max) {
                    max = reps;
                    maxIndex = i;
                }
            }

            if (maxIndex != null) {
                fullIndexes.add(maxIndex);
                res[maxIndex] = chr;
            }
        }

        for (Character chr : res) {
            sb.append(chr == null ? 'a' : chr);
        }

        return sb.toString();
    }

    /**
     * returns an array of characters of length = wordLength in ordered by
     * repetitions DESC
     */
    private Character[] mostUsedCharacters() {
        if (this.charReps.isEmpty()) {
            return null;
        }

        List<Entry<Character, Integer>> list = new ArrayList<>(this.charReps.entrySet());
        Character[] res = new Character[this.wordLength];

        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        while (list.size() < this.wordLength) {
            list.addAll(list);
        }

        list = list.subList(0, this.wordLength);

        for (int i = 0; i < this.wordLength; i++) {
            res[i] = list.get(i).getKey();
        }

        return res;
    }

    /** populates originalRoot tree with data */
    private void populateTree(Set<String> words) {
        for (String string : words) {
            char[] chars = string.toCharArray();

            this.populateTree(chars);
        }
    }

    /** insert nodes for each character in a word */
    private void populateTree(char[] chars) {
        Node node = this.originalRoot;

        for (int i = 0; i < chars.length; i++) {
            node = node.addChild(chars[i]);

            // update charReps
            Integer charReps = this.charReps.getOrDefault(chars[i], 0) + 1;
            this.charReps.put(chars[i], charReps);

            // udpate charRepsByPos
            Integer charRepsByPos = this.charRepsByPos
                    .get(i).getOrDefault(chars[i], 0) + 1;
            this.charRepsByPos.get(i).put(chars[i], charRepsByPos);
        }
    }

    /** checks if the correct word was found */
    private boolean isCorrectGuess(List<Character> response) {
        return response.stream().allMatch(chr -> chr == '+');
    }

    /** removes nodes from the tree based on the given response */
    private void parseResponse(List<Character> response) {
        for (int i = 0; i < this.wordLength; i++) {

            Character rspChr = response.get(i);
            Character chr = this.lastGuess.charAt(i);

            switch (rspChr) {
                case '+':
                    if (this.confirmedLetters.get(i) != null) {
                        continue;
                    }

                    int rmIndex = this.wrongPosLetters.indexOf(chr);

                    if (rmIndex != -1) {
                        this.wrongPosLetters.remove(rmIndex);
                    }

                    this.confirmedLetters.put(i, chr);
                    this.root.retain(chr, i);
                    break;
                case 'o':
                    this.wrongPosLetters.add(chr);
                    this.root.remove(chr, i);
                    break;
                case '-':
                    boolean isCorrectOrExists = false;

                    for (int j = 0; j < this.wordLength; j++) {
                        if (chr == this.lastGuess.charAt(j) && response.get(j) != '-') {
                            isCorrectOrExists = true;
                            break;
                        }
                    }

                    if (isCorrectOrExists) {
                        this.root.remove(chr, i);
                        break;
                    }

                    if (!this.confirmedLetters.values().contains(chr)) {
                        this.root.remove(chr);
                    }
                    break;
            }
        }

        this.root.purgeBranches(this.wordLength);
        this.root.update();
    }

    private static class Node {
        private final Character chr;
        private int weight = 0;

        private HashMap<Character, Node> children = new HashMap<>();

        public Node(Character chr) {
            this.chr = chr;
            this.weight++;
        }

        public Node(Character chr, int weight) {
            this.chr = chr;
            this.weight = weight;
        }

        /** generate new guess string */
        public String generateGuess() {
            return this.generateGuess(new StringBuilder(), null).toString();
        }

        /**
         * generate new guess string with respect to letters that are in the
         * word but are at wrong position
         */
        public String generateGuess(ArrayList<Character> wrongPosLetters) {
            return this.generateGuess(new StringBuilder(), wrongPosLetters).toString();
        }

        private StringBuilder generateGuess(
                StringBuilder sb,
                ArrayList<Character> wrongPosLetters) {
            if (this.children.size() == 0) {
                return sb;
            }

            Node next = null;
            if (wrongPosLetters == null) {
                next = this.getNext();
            } else {
                next = this.getNext(wrongPosLetters);
            }

            sb.append(next.chr);

            return next.generateGuess(sb, wrongPosLetters);
        }

        /**
         * returns a node which contains the best next character prediction with respect
         * to letters that are in the word but are at wrong position
         */
        private Node getNext(ArrayList<Character> wrongPosLetters) {
            for (int i = 0; i < wrongPosLetters.size(); i++) {
                Character chr = wrongPosLetters.get(i);

                if (this.children.containsKey(chr)) {
                    wrongPosLetters.remove(i);

                    return this.children.get(chr);
                }
            }

            return this.getNext();
        }

        /** returns a node which contains the best next character prediction */
        public Node getNext() {
            int maxWeight = 0;
            Node maxWeightChild = null;

            for (Node child : this.children.values()) {
                if (child.weight > maxWeight) {
                    maxWeight = child.weight;
                    maxWeightChild = child;
                }
            }

            return maxWeightChild;
        }

        /** adds a child to the node and updates the weight */
        public Node addChild(Character chr) {
            Node child = this.children.get(chr);

            if (child == null) {
                child = new Node(chr);

                this.children.put(chr, child);

                return child;
            }

            child.weight++;

            return child;
        }

        /** removes all occurrences of a character in a tree */
        public void remove(Character chr) {
            this.children.remove(chr);

            this.children.values()
                    .stream()
                    .forEach(node -> node.remove(chr));
        }

        /** removes all occurrences of a character at given tree depth */
        public void remove(Character chr, int depth) {
            if (depth == 0) {
                this.children.remove(chr);
                return;
            }

            this.children.values()
                    .stream()
                    .forEach(node -> node.remove(chr, depth - 1));
        }

        /** retain only given character at given tree depth */
        public void retain(Character chr, int depth) {
            if (depth == 0) {
                Node child = this.children.get(chr);

                this.children = new HashMap<>();

                if (child != null) {
                    this.children.put(chr, child);
                }

                return;
            }

            this.children.values()
                    .stream()
                    .forEach(node -> node.retain(chr, depth - 1));
        }

        /** purges branches that are less than targetDepth deep */
        public void purgeBranches(int targetDepth) {
            this.purgeBranches(0, targetDepth);
        }

        /** purges branches that are less than targetDepth deep */
        private void purgeBranches(int currentDepth, int targetDepth) {
            if (currentDepth >= targetDepth) {
                return;
            }

            Iterator<Node> iterator = children.values().iterator();
            while (iterator.hasNext()) {
                Node child = iterator.next();

                child.purgeBranches(currentDepth + 1, targetDepth);

                if (child.children.isEmpty() && currentDepth + 1 < targetDepth) {
                    iterator.remove();
                }
            }
        }

        /** returns a copy of tree */
        public Node copy() {
            Node copy = new Node(this.chr, this.weight);

            for (Node child : this.children.values()) {
                copy.children.put(child.chr, child.copy());
            }

            return copy;
        }

        /** updates all weights in a tree */
        public void update() {
            int weight = 0;

            for (Node child : this.children.values()) {
                child.update();

                weight += child.weight;
            }

            this.weight = weight == 0 ? 1 : weight;
        }

        /**
         * removes the given word from a tree so it cannot be generated anymore
         * 
         * ONLY USE IF TESTING WITH {@link TestirajVse}
         */
        public void removeWord(String word) {
            if (word.length() == 0) {
                return;
            }

            Node child = this.children.get(word.charAt(0));

            child.weight--;

            if (child.weight == 0) {
                this.children.remove(word.charAt(0));
            }

            child.removeWord(word.substring(1, word.length()));
        }
    }
}
