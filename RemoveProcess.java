import java.util.*;
public class RemoveProcess  {

    int processNumber;
    int pageNumber;

    public RemoveProcess(int processNumber, int pageNumber) {
        this.processNumber = processNumber;
        this.pageNumber = pageNumber;        
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("process number: " + processNumber + "\n");
        sb.append("page number: " + pageNumber);
        return sb.toString();
    }

}
