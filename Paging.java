import java.util.*;
import java.io.*;

public class Paging {

    public static void main(String[] args) throws IOException {
        Machine machine = new Machine();
        
        int machineSize = Integer.parseInt(args[0]);
        int pageSize = Integer.parseInt(args[1]);
        int processSize = Integer.parseInt(args[2]);
        int jobMix = Integer.parseInt(args[3]);
        int numReferences = Integer.parseInt(args[4]);
        String replaceAlgo = args[5];
        int outputMode = Integer.parseInt(args[6]);

        machine.machineSize = machineSize;
        machine.pageSize = pageSize;
        machine.processSize = processSize;
        machine.jobMix = jobMix;
        machine.numReferences = numReferences;
        machine.replaceAlgo = replaceAlgo;
        machine.outputMode = outputMode;
        machine.numFrames = machineSize/pageSize;
        machine.numPages = processSize/pageSize;

        machine.run();
    }

}
