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
    LinkedList<RemoveProcess> LRUqueue; // Tracks which process is LRU
    LinkedList<RemoveProcess> FIFOqueue; // Tracks with process is FIFO
    LinkedList<RemoveProcess> randomQueue;

    List<Process> processList;
    List<List<Integer>> pageTable;
    List<Integer> occupiedPages; // records which pages are occupying which pages. 0 indicates untaken.
    List<Integer> occupiedFrames; // records which frames are being occupied. 0 indicates untaken.
    int nextFrameIndex; // indicates which frame index in occupiedFrames should be referenced next.
    int numOccupiedPages; // tracks how many pages are occupied.
    Set<Integer> terminatedProcesses; // records which processes have been terminated already.

    public Machine() {
        this.LRUqueue = new LinkedList<>();
        this.FIFOqueue = new LinkedList<>();
        this.randomQueue = new LinkedList<>();
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
        this.nextFrameIndex = this.numFrames; // Set to highest initially, if reaches -1 loop back to highest
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
                Process p1 = new Process(numPages, numFrames);
                p1.probA = 1;
                p1.probB = 0;
                p1.probC = 0;
                p1.processNumber = 1;
                processList.add(p1);
                break;
            // Job mix 2 - 1/0/0 for each, four processes
            case 2:
                for (int i = 0; i < 4; i++) {
                    Process p2 = new Process(numPages, numFrames);
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
                    Process p3 = new Process(numPages, numFrames);
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
                    Process p4 = new Process(numPages, numFrames);
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
                random(new Scanner(randomNumbers));
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
                    int firstNum = randomScanner.nextInt();
                    int pageIndex = currProcess.nextWord / this.pageSize;

                    System.out.print((processNum) + " references word " + currProcess.nextWord + " (page " + pageIndex +
                    ") at time " + this.cycle + ": ");

                    // Check if the page is occupied.
                    List<Integer> processPageTable = currProcess.pageTable;

                    boolean found = false;
                    RemoveProcess remove = null;
                    for (int i = 0; i < this.LRUqueue.size(); i++) {
                        RemoveProcess p = this.LRUqueue.get(i);
                        if (p.processNumber == currProcess.processNumber && p.pageNumber == pageIndex) {
                            found = true;
                            remove = p;
                            break;
                        }
                    }

                    // Case 1: Unoccupied
                    if (processPageTable.get(pageIndex) == -1 && this.numOccupiedPages < this.numFrames) {
                        currProcess.LRUframe = this.nextFrameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = this.nextFrameIndex;
                        this.nextFrameIndex--;
                        
                        // Page fault: frame unused
                        System.out.print("Fault, using free frame " + (this.nextFrameIndex) +"\n");
                        this.numOccupiedPages++;

                        // Update the LRU queue.
                        RemoveProcess LRUp = new RemoveProcess(processNum, pageIndex);
                        this.LRUqueue.add(LRUp);
                        processPageTable.set(pageIndex, this.nextFrameIndex);

                        // Record residency: add to timesLoaded
                        currProcess.timesLoaded.get(pageIndex).add(cycle);
                        currProcess.numPageFaults++;
                    }
                    
                    // Check if it's occupied by a DIFF number and the numFrames is at max.
                    else if (processPageTable.get(pageIndex) == -1 && this.numOccupiedPages == this.numFrames || !found) 
                    {
                        RemoveProcess evicted = this.LRUqueue.poll();
                        this.nextFrameIndex--;
                        
                        int evictedPageNum = evicted.pageNumber;
                        int evictedProcessNum = evicted.processNumber;
                        int evictedFrameNum = processList.get(evictedProcessNum - 1).pageTable.get(evictedPageNum);

                        System.out.print("Fault, EVICT page " + evictedPageNum + " of " + 
                        evictedProcessNum + " from frame " + evictedFrameNum + "\n");
                        
                        // frameCalled = true;
                        currProcess.LRUframe = this.nextFrameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = this.nextFrameIndex;

                        RemoveProcess LRUp = new RemoveProcess(processNum, pageIndex);
                        this.LRUqueue.add(LRUp);
                        processPageTable.set(pageIndex, evictedFrameNum);

                        // Record residency: add to timesEvicted. 
                        // Also add to newly slotted's timesLoaded
                        currProcess.timesLoaded.get(pageIndex).add(cycle);
                        currProcess.numPageFaults++;

                        Process evictedProcess = processList.get(evictedProcessNum - 1);
                        evictedProcess.timesEvicted.get(evictedPageNum).add(cycle);
                        evictedProcess.numEvictions++;
                        
                    }

                    // Otherwise, hit and proceed as normal. 
                    // Find the hit processNum in the LRUQueue and move it to the front.
                    else {
                        if (remove != null) {
                            int hitIndex = this.LRUqueue.indexOf(remove);
                            RemoveProcess hitProcess = this.LRUqueue.remove(hitIndex);
                            this.LRUqueue.add(hitProcess);
                            System.out.print("Hit in frame " + processPageTable.get(pageIndex));
                        }
                        System.out.print("\n");

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

                    // if (this.cycle == 200)
                    //     System.exit(0);

                } // end while count < 3

                System.out.println();
                if (this.nextFrameIndex == 0)
                    this.nextFrameIndex = this.numFrames; 
                
            } // end for process
        } // end while cycle

        printStats(processList);
    }

    // FIFO: should use a queue
    // LRU
    public void fifo(Scanner randomScanner) {
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
                    int firstNum = randomScanner.nextInt();
                    int pageIndex = currProcess.nextWord / this.pageSize;

                    System.out.print((processNum) + " references word " + currProcess.nextWord + " (page " + pageIndex +
                    ") at time " + this.cycle + ": ");

                    // Check if the page is occupied.
                    List<Integer> processPageTable = currProcess.pageTable;

                    boolean found = false;
                    RemoveProcess remove = null;
                    for (int i = 0; i < this.FIFOqueue.size(); i++) {
                        RemoveProcess p = this.FIFOqueue.get(i);
                        if (p.processNumber == currProcess.processNumber && p.pageNumber == pageIndex) {
                            found = true;
                            remove = p;
                            break;
                        }
                    }

                    // Case 1: Unoccupied
                    if (processPageTable.get(pageIndex) == -1 && this.numOccupiedPages < this.numFrames) {
                        currProcess.LRUframe = this.nextFrameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = this.nextFrameIndex;
                        this.nextFrameIndex--;
                        
                        // Page fault: frame unused
                        System.out.print("Fault, using free frame " + (this.nextFrameIndex) +"\n");
                        this.numOccupiedPages++;

                        // Update the LRU queue.
                        RemoveProcess LRUp = new RemoveProcess(processNum, pageIndex);
                        this.FIFOqueue.add(LRUp);
                        processPageTable.set(pageIndex, this.nextFrameIndex);

                        // Record residency: add to timesLoaded
                        currProcess.timesLoaded.get(pageIndex).add(cycle);
                        currProcess.numPageFaults++;
                    }
                    
                    // Check if it's occupied by a DIFF number and the numFrames is at max.
                    else if (processPageTable.get(pageIndex) == -1 && this.numOccupiedPages == this.numFrames || !found) 
                    {
                        RemoveProcess evicted = this.FIFOqueue.poll();
                        this.nextFrameIndex--;
                        
                        int evictedPageNum = evicted.pageNumber;
                        int evictedProcessNum = evicted.processNumber;
                        int evictedFrameNum = processList.get(evictedProcessNum - 1).pageTable.get(evictedPageNum);

                        System.out.print("Fault, EVICT page " + evictedPageNum + " of " + 
                        evictedProcessNum + " from frame " + evictedFrameNum + "\n");
                        
                        // frameCalled = true;
                        currProcess.LRUframe = this.nextFrameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = this.nextFrameIndex;

                        RemoveProcess LRUp = new RemoveProcess(processNum, pageIndex);
                        this.FIFOqueue.add(LRUp);
                        processPageTable.set(pageIndex, evictedFrameNum);

                        // Record residency: add to timesEvicted. 
                        // Also add to newly slotted's timesLoaded
                        currProcess.timesLoaded.get(pageIndex).add(cycle);
                        currProcess.numPageFaults++;

                        Process evictedProcess = processList.get(evictedProcessNum - 1);
                        evictedProcess.timesEvicted.get(evictedPageNum).add(cycle);
                        evictedProcess.numEvictions++;
                    }

                    // Otherwise, hit and proceed as normal. 
                    // Find the hit processNum in the LRUQueue and move it to the front.
                    else {
                        if (remove != null) {
                            // int hitIndex = this.FIFOqueue.indexOf(remove);
                            // RemoveProcess hitProcess = this.FIFOqueue.remove(hitIndex);
                            // this.FIFOqueue.add(hitProcess);
                            System.out.print("Hit in frame " + processPageTable.get(pageIndex));
                        }
                        System.out.print("\n");

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

                    // if (this.cycle == 200)
                    //     System.exit(0);

                } // end while count < 3

                System.out.println();
                if (this.nextFrameIndex == 0)
                    this.nextFrameIndex = this.numFrames; 
                
            } // end for process
        } // end while cycle

        printStats(processList);
    }

    // Random: pick a random occupied to evict
    public void random(Scanner randomScanner) {
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
                    int pageIndex = currProcess.nextWord / this.pageSize;

                    System.out.print((processNum) + " references word " + currProcess.nextWord + " (page " + pageIndex +
                    ") at time " + this.cycle + ": ");

                    // Check if the page is occupied.
                    List<Integer> processPageTable = currProcess.pageTable;

                    boolean found = false;
                    RemoveProcess remove = null;
                    for (int i = 0; i < this.randomQueue.size(); i++) {
                        RemoveProcess p = this.randomQueue.get(i);
                        if (p.processNumber == currProcess.processNumber && p.pageNumber == pageIndex) {
                            found = true;
                            remove = p;
                            break;
                        }
                    }

                    // Case 1: Unoccupied
                    if (processPageTable.get(pageIndex) == -1 && this.numOccupiedPages < this.numFrames) {
                        currProcess.LRUframe = this.nextFrameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = this.nextFrameIndex;
                        this.nextFrameIndex--;
                        
                        // Page fault: frame unused
                        System.out.print("Fault, using free frame " + (this.nextFrameIndex) +"\n");
                        this.numOccupiedPages++;

                        // Update the LRU queue.
                        RemoveProcess LRUp = new RemoveProcess(processNum, pageIndex);
                        this.randomQueue.add(LRUp);
                        processPageTable.set(pageIndex, this.nextFrameIndex);

                        // Record residency: add to timesLoaded
                        currProcess.timesLoaded.get(pageIndex).add(cycle);
                        currProcess.numPageFaults++;
                    }
                    
                    // Check if it's occupied by a DIFF number and the numFrames is at max.
                    else if (processPageTable.get(pageIndex) == -1 && this.numOccupiedPages == this.numFrames || !found) 
                    {   
                        int randomNum = randomScanner.nextInt();
                        int frameToEvict = ((randomNum + this.numFrames) % this.numFrames);
                        
                        RemoveProcess evicted = null;
                        // search for frameToEvict in the list, don't just use it as an index.
                        this.nextFrameIndex--;

                        int evictPage = -1;
                        int evictProcessNum = -1;
                        int evictFrameNum = -1;
                        boolean foundFrame = false;
                        
                        for (int i = 0; i < this.randomQueue.size(); i++) {
                            RemoveProcess removeP = this.randomQueue.get(i);
                            Process p = this.processList.get(removeP.processNumber - 1);
                            
                            for (int j = 0; j < p.pageTable.size(); j++) {
                                int frame = p.pageTable.get(j);
                                if (frame == frameToEvict) {
                                    evictPage = removeP.pageNumber;
                                    evictProcessNum = p.processNumber;
                                    evictFrameNum = frame;
                                    foundFrame = true;
                                    p.pageTable.set(j, -1);
                                    this.randomQueue.remove(i);
                                    break;
                                }
                            }
                            if (foundFrame)
                                break;
                        }

                        System.out.print("Fault, EVICT page " + evictPage + " of " + 
                        evictProcessNum + " from frame " + evictFrameNum + "\n");
                        
                        // frameCalled = true;
                        currProcess.LRUframe = this.nextFrameIndex;
                        currProcess.LRUpage = pageIndex;
                        currProcess.nextFrame = this.nextFrameIndex;

                        RemoveProcess LRUp = new RemoveProcess(processNum, pageIndex);
                        this.randomQueue.add(LRUp);
                        processPageTable.set(pageIndex, evictFrameNum);

                        // Record residency: add to timesEvicted. 
                        // Also add to newly slotted's timesLoaded
                        currProcess.timesLoaded.get(pageIndex).add(cycle);
                        currProcess.numPageFaults++;

                        Process evictedProcess = processList.get(evictProcessNum - 1);
                        evictedProcess.timesEvicted.get(evictPage).add(cycle);
                        evictedProcess.numEvictions++;
                    }

                    // Otherwise, hit and proceed as normal. 
                    // Find the hit processNum in the LRUQueue and move it to the front.
                    else {
                        System.out.print("Hit in frame " + processPageTable.get(pageIndex));
                        System.out.print("\n");

                    }
                    int firstNum = randomScanner.nextInt();
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

                } // end while count < 3

                System.out.println();
                if (this.nextFrameIndex == 0)
                    this.nextFrameIndex = this.numFrames; 
                
            } // end for process
        } // end while cycle

        printStats(processList);
    }

    public void printStats(List<Process> processList) {
        int totalFaults = 0;
        int totalResidency = 0;
        int totalEvictions = 0;
        for (Process p : processList) {
            int processNum = p.processNumber;
            int numFaults = p.numPageFaults;
            int numEvictions = p.numEvictions;
            
            totalFaults += numFaults;
            totalEvictions += numEvictions;

            List<List<Integer>> timesLoaded = p.timesLoaded;
            List<List<Integer>> timesEvicted = p.timesEvicted;

            int residency = 0;

            for (int i = 0; i < timesLoaded.size(); i++) {
                List<Integer> pageTimesLoaded = timesLoaded.get(i);
                List<Integer> pageTimesEvicted = timesEvicted.get(i);
                
                for (int j = 0; j < pageTimesLoaded.size(); j++) {
                    int timeLoaded = pageTimesLoaded.get(j);
                    int timeEvicted = j >= pageTimesEvicted.size() ? -1 : pageTimesEvicted.get(j);

                    if (timeEvicted != -1)
                        residency += (timeEvicted - timeLoaded);
                }

            }

            boolean undefined = numEvictions == 0 ? true : false;
            double averageResidency = (residency / (double)(numEvictions));
            totalResidency += residency;
            
            if (undefined) {
                System.out.println("Process " + processNum + " had " + numFaults + 
                ". With no evictions, the average residence is undefined.");
            }
            else {
                System.out.println("Process " + processNum + " had " + numFaults + 
                " faults and " + averageResidency + " average residency.");
            }
        }


        double totalAverageResidency = totalResidency / ((double) (totalEvictions));
        System.out.println("The total number of faults is " + totalFaults + 
        " and the overall average residency is " + totalAverageResidency + ".");
    }

}
