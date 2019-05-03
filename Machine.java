import java.util.*;
import java.io.*;

// java Paging 10 10 20 1 10 lru 0

public class Machine {
    // Datafields
    int machineSize;
    int pageSize;
    int processSize;
    int jobMix;
    int numReferences;
    int cycle; // Tracks current cycle time
    int outputMode; // Strictly for debugging
    String replaceAlgo;
    final static int quantum = 3;

    // Augmented datafields
    int numFrames;
    int numPages;
    int totalRunTime; // Can get this by doing numReferences * processes

    // File
    static File randomNumbers = new File("random-numbers.txt");

    // Data structures
    LinkedList<Process> LRUqueue; // Tracks which process is LRU
    LinkedList<Process> FIFOqueue; // Tracks with process is FIFO

    List<Process> processList;
    List<List<Integer>> pageTable;
    List<Integer> occupiedPages; // records which pages are occupying which pages. 0 indicates untaken.
    List<Integer> occupiedFrames; // records which frames are being occupied. 0 indicates untaken.
    int nextFrameIndex; // indicates which frame index in occupiedFrames should be referenced next.
    int numOccupiedPages; // tracks how many pages are occupied.
    Set<Integer> terminatedProcesses; // records which processes have been terminated already.

    public Machine() {
        this.LRUqueue = new LinkedList<>();
        this.processList = new ArrayList<>();
        this.pageTable = new ArrayList<>();
        this.occupiedPages = new ArrayList<>();
        this.occupiedFrames = new ArrayList<>();
        this.terminatedProcesses = new HashSet<>();
    }

    // Run, depending on the job mix and algorithm supplied
    public void run() throws IOException {
        this.numFrames = this.machineSize / this.pageSize;
        this.numPages = this.processSize / this.pageSize;
        this.numOccupiedPages = 0;
        this.nextFrameIndex = this.numFrames - 1; // Set to highest initially, if reaches -1 loop back to highest
        this.cycle = 1;

        for (int i = 0; i < this.numPages; i++) {
            this.occupiedPages.add(0);
            List<Integer> frameTable = new ArrayList<>();
            for (int j = 0; j < this.numFrames; j++)
                frameTable.add(-1);
            this.pageTable.add(frameTable);
        }

        for (int i = 0; i < this.numFrames; i++) {
            this.occupiedFrames.add(0);
        }
        
        switch (jobMix) {
            // Job mix 1 - 1/0/0, one process
            case 1:
                Process p1 = new Process(numFrames);
                p1.probA = 1;
                p1.probB = 0;
                p1.probC = 0;
                p1.processNumber = 1;
                processList.add(p1);
                break;
            // Job mix 2 - 1/0/0 for each, four processes
            case 2:
                for (int i = 0; i < 4; i++) {
                    Process p2 = new Process(numFrames);
                    p2.probA = 1;
                    p2.probB = 0;
                    p2.probC = 0;
                    p2.processNumber = i + 1;
                    processList.add(p2);
                }
                break;
            // Job mix 3 - 0/0/0 (random), four processes
            case 3:
                for (int i = 0; i < 4; i++) {
                    Process p3 = new Process(numFrames);
                    p3.probA = 0;
                    p3.probB = 0;
                    p3.probC = 0;
                    p3.processNumber = i + 1;
                    processList.add(p3);
                }
                break;
            // Job mix 4 - different probs for each, four processes
            case 4:
                for (int i = 0; i < 4; i++) {
                    Process p4 = new Process(numFrames);
                    p4.processNumber = i + 1;
                    switch (i) {
                        case 0:
                            p4.probA = .75;
                            p4.probB = .25;
                            p4.probC = 0;
                            break;
                        case 1:
                            p4.probA = .75;
                            p4.probB = 0;
                            p4.probC = .25;
                            break;
                        case 2:
                            p4.probA = .75;
                            p4.probB = .125;
                            p4.probC = .125;
                            break;
                        case 3:
                            p4.probA = .5;
                            p4.probB = .125;
                            p4.probC = .125;
                            break;
                    }
                    this.processList.add(p4);
                } // end for
                break;
        } // end switch jobMix

        this.totalRunTime = this.numReferences * this.processList.size();

        switch (replaceAlgo) {
            case "lru":
                System.out.println("RUNNING LRU");
                LRU(new Scanner(randomNumbers));
                break;
            case "random":
                System.out.println("RUNNING RANDOM");
                // random(new Scanner(randomNumbers));
                break;
            case "fifo":
                System.out.println("RUNNING FIFO");
                fifo(new Scanner(randomNumbers));
                break;
        }

    } // end run method
    
    // LRU
    public void LRU(Scanner randomScanner) {
        // Main process loop
        
        while (this.cycle <= this.totalRunTime) {
            for (int processNum = 1; processNum <= this.processList.size(); processNum++) {
                int count = 0;
                Process currProcess = this.processList.get(processNum - 1);

                // only calculate this if first time running
                if (currProcess.nextWord == -1) {
                    int firstWord = (111 * processNum + processSize) % processSize; 
                    currProcess.nextWord = firstWord;
                }

                // Skip if terminated
                if (terminatedProcesses.contains(processNum))
                    continue;

                while (count++ < Machine.quantum && this.cycle <= this.totalRunTime) {
                    int frameIndex = this.nextFrameIndex;
                    int firstNum = randomScanner.nextInt();
                    int pageIndex = currProcess.nextWord / this.pageSize;

                    System.out.print((processNum) + " references word " + currProcess.nextWord + " (page " + pageIndex +
                    ") at time " + this.cycle + ": ");

                    // Check if the page is occupied.
                    List<Integer> frameTable = this.pageTable.get(pageIndex);
                    // System.out.println(frameTable);

                    if (frameTable.get(frameIndex) == -1 && this.numOccupiedPages < this.numFrames) {
                        currProcess.LRUframe = frameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = frameIndex;

                        // Page fault: frame unused
                        System.out.print("Fault, using free frame " + (frameIndex) +"\n");
                        this.numOccupiedPages++;
                        // Update the LRU queue.
                      
                        this.LRUqueue.add(currProcess);
                        frameTable.set(frameIndex, processNum);
                    }
                    
                    // Check if it's occupied by a DIFF number and the numFrames is at max.
                    else if ((frameTable.get(frameIndex) != (processNum) && this.numOccupiedPages == this.numFrames) ||
                    (frameTable.get(frameIndex) == -1 && this.numOccupiedPages < this.numFrames)) {
                        Process evicted = this.LRUqueue.poll();
                        System.out.print("Fault, EVICT page " + evicted.LRUpage + " of " + 
                        evicted.processNumber + " from frame " + evicted.LRUframe + "\n");
                        
                        currProcess.LRUframe = frameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = frameIndex;

                        this.LRUqueue.add(currProcess);
                        frameTable.set(frameIndex, processNum);
                    }

                    // Otherwise, hit and proceed as normal. 
                    // Find the hit processNum in the LRUQueue and move it to the front.
                    else {
                        
                        for (int i = 0; i < this.LRUqueue.size(); i++) {
                            Process p = this.LRUqueue.get(i);
                            if (p.processNumber == currProcess.processNumber) {
                                Process hitProcess = this.LRUqueue.remove(i);
                                this.LRUqueue.add(hitProcess);
                                frameTable.set(frameIndex, currProcess.processNumber);
                                System.out.print("Hit in frame " + frameIndex + "\n");
                            }
                        }

                    }

                    double y = firstNum / (Integer.MAX_VALUE + 1d);
                    
                    int nextWord;
                    double A = currProcess.probA;
                    double B = currProcess.probB;
                    double C = currProcess.probC;
                    if (y < A) {
                        nextWord = (currProcess.nextWord + 1 + processSize) % processSize;
                        currProcess.nextWord = nextWord;
                    } else if (y < A + B) {
                        nextWord = (currProcess.nextWord - 5 + processSize) % processSize;
                        currProcess.nextWord = nextWord;
                    } else if (y < A + B + C) {
                        nextWord = (currProcess.nextWord + 4 + processSize) % processSize;
                        currProcess.nextWord = nextWord;
                    } else if (y >= A + B + C) {
                        nextWord = randomScanner.nextInt() % (this.processSize);
                        currProcess.nextWord = nextWord;
                    }
                    
                    this.cycle++;

                    // Increment the numTimesReferenced of the process in processList.
                    // If the limit is reached, add it to terminatedProcesses
                    currProcess.numTimesReferenced++;
                    if (currProcess.numTimesReferenced == numReferences) {
                        terminatedProcesses.add(processNum);
                        break;
                    }

                    if (this.cycle == 8)
                        System.exit(0);
    
                } // end while count < 3
                
                this.nextFrameIndex--;
                if (this.nextFrameIndex == -1)
                    this.nextFrameIndex = this.numFrames - 1;                
                
            } // end for process
        } // end while cycle

    }

    // Random: pick a random occupied to evict
    public void random(Scanner randomScanner) {
        
    }

    // FIFO: should use a queue
    public void fifo(Scanner randomScanner) {
        while (this.cycle <= this.totalRunTime) {
            for (int processNum = 1; processNum <= this.processList.size(); processNum++) {
                int count = 0;
                Process currProcess = this.processList.get(processNum - 1);

                // only calculate this if first time running
                if (currProcess.nextWord == -1) {
                    int firstWord = (111 * processNum + processSize) % processSize; 
                    currProcess.nextWord = firstWord;
                }

                // Skip if terminated
                if (terminatedProcesses.contains(processNum))
                    continue;

                while (count++ < Machine.quantum && this.cycle <= this.totalRunTime) {
                    int frameIndex = this.nextFrameIndex;
                    int firstNum = randomScanner.nextInt();
                    int pageIndex = currProcess.nextWord / this.pageSize;

                    System.out.print((processNum) + " references word " + currProcess.nextWord + " (page " + pageIndex +
                    ") at time " + this.cycle + ": ");

                    // Check if the page is occupied.
                    List<Integer> frameTable = this.pageTable.get(pageIndex);
                    // System.out.println(frameTable);

                    if (frameTable.get(frameIndex) == -1 && this.numOccupiedPages < this.numFrames) {
                        currProcess.LRUframe = frameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = frameIndex;

                        // Page fault: frame unused
                        System.out.print("Fault, using free frame " + (frameIndex) +"\n");
                        this.numOccupiedPages++;
                        // Update the LRU queue.
                      
                        this.LRUqueue.add(currProcess);
                        frameTable.set(frameIndex, processNum);
                    }
                    
                    // Check if it's occupied by a DIFF number and the numFrames is at max.
                    else if ((frameTable.get(frameIndex) != (processNum) && this.numOccupiedPages == this.numFrames) ||
                    (frameTable.get(frameIndex) == -1 && this.numOccupiedPages < this.numFrames)) {
                        Process evicted = this.LRUqueue.poll();
                        System.out.print("Fault, EVICT page " + evicted.LRUpage + " of " + 
                        evicted.processNumber + " from frame " + evicted.LRUframe + "\n");
                        
                        currProcess.LRUframe = frameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = frameIndex;

                        this.LRUqueue.add(currProcess);
                        frameTable.set(frameIndex, processNum);
                    }

                    // Otherwise, hit and proceed as normal. 
                    // Find the hit processNum in the LRUQueue and move it to the front.
                    else {
                        
                        System.out.print("Hit in frame " + frameIndex + "\n");

                        // for (int i = 0; i < this.LRUqueue.size(); i++) {
                        //     Process p = this.LRUqueue.get(i);
                        //     if (p.processNumber == currProcess.processNumber) {
                        //         Process hitProcess = this.LRUqueue.remove(i);
                        //         this.LRUqueue.add(hitProcess);
                        //         frameTable.set(frameIndex, currProcess.processNumber);
                        //         System.out.print("Hit in frame " + frameIndex + "\n");
                        //     }
                        // }

                    }

                    double y = firstNum / (Integer.MAX_VALUE + 1d);
                    
                    int nextWord;
                    double A = currProcess.probA;
                    double B = currProcess.probB;
                    double C = currProcess.probC;
                    if (y < A) {
                        nextWord = (currProcess.nextWord + 1 + processSize) % processSize;
                        currProcess.nextWord = nextWord;
                    } else if (y < A + B) {
                        nextWord = (currProcess.nextWord - 5 + processSize) % processSize;
                        currProcess.nextWord = nextWord;
                    } else if (y < A + B + C) {
                        nextWord = (currProcess.nextWord + 4 + processSize) % processSize;
                        currProcess.nextWord = nextWord;
                    } else if (y >= A + B + C) {
                        nextWord = randomScanner.nextInt() % (this.processSize);
                        currProcess.nextWord = nextWord;
                    }
                    
                    this.cycle++;

                    // Increment the numTimesReferenced of the process in processList.
                    // If the limit is reached, add it to terminatedProcesses
                    currProcess.numTimesReferenced++;
                    if (currProcess.numTimesReferenced == numReferences) {
                        terminatedProcesses.add(processNum);
                        break;
                    }

                    // if (this.cycle == 8)
                    //     System.exit(0);
    
                } // end while count < 3
                
                this.nextFrameIndex--;
                if (this.nextFrameIndex == -1)
                    this.nextFrameIndex = this.numFrames - 1;                
                
            } // end for process
        } // end while cycle
    }

    public void printStats() {
        
    }

}
