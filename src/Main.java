import CPU.SerialCPU;
import CPU.ParallelalCPU;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        Scanner sc = new Scanner(System.in);
        String texto1 = "src/Amostras/DonQuixote-388208.txt";
        String texto2 = "src/Amostras/Dracula-165307.txt";
        String texto3 = "src/Amostras/MobyDick-217452.txt";
        String[] arrayTextos = {texto1,texto2,texto3};
        SerialCPU scp = new SerialCPU();
        scp.runSerial(arrayTextos);
        ParallelalCPU pcp = new ParallelalCPU();
        pcp.runParallelCPU(arrayTextos);
        }
    }