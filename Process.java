public class Process {

    int processNumber;
    int processSize;
    int nextWord;
    int numTimesReferenced;

    // Probabilities
    double probA;
    double probB;
    double probC;

    public Process() {
        this.numTimesReferenced = 0;
        this.nextWord = -1;
    }

    public Process(int processSize, int processNumber) {
        this.processSize = processSize;
        this.processNumber = processNumber;
        this.nextWord = (111 * processNumber) % processSize;
    }


}
