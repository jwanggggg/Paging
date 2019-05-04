import java.util.*;

public class Process {
    // Datafields
    int processNumber;
    int processSize;
    int nextWord;
    int numTimesReferenced;
    int LRUpage;
    int LRUframe;
    int nextFrame;
    List<Integer> pageTable; // Holds a frametable for each page
    List<List<Integer>> timesLoaded; // Records times loaded
    List<List<Integer>> timesEvicted; // Records times evicted

    static int globalFrame;

    // Probabilities
    double probA;
    double probB;
    double probC;

    // Statistics
    int numPageFaults; // Number of times the process was faulted.
    int numEvictions; // Number of times the process was evicted.

    public Process(int numPages, int numFrames) {
        this.numTimesReferenced = 0;
        this.nextWord = -1;
        this.pageTable = new ArrayList<>();
        this.timesLoaded = new ArrayList<>();
        this.timesEvicted = new ArrayList<>();

        for (int i = 0; i < numPages; i++) {
            this.pageTable.add(-1);
            this.timesLoaded.add(new ArrayList<>());
            this.timesEvicted.add(new ArrayList<>());
        }
    
    }

}
