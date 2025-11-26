package CPU;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Scanner;

public class SerialCPU {
    Scanner sc = new Scanner(System.in);
    public int runSerial(String[] arrayTextos) throws IOException {
        Scanner sc = new Scanner(System.in);
        List<String> palavrasList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            System.out.println("Informe a palavra que deseja buscar no livro " + i);
            palavrasList.add(sc.next().toLowerCase());
        }
        long startTotal = System.currentTimeMillis();
        for (String texto : arrayTextos) {
            long start = System.currentTimeMillis();
            BufferedReader br = new BufferedReader(new FileReader(texto));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            String[] words = sb.toString().split("\\W+");
            int count = 0;
            for (String w : words)
                for (String buscada : palavrasList)
                    if (buscada.equals(w.toLowerCase()))
                        count++;

            long end = System.currentTimeMillis();
            System.out.println("Serial: " + count + " ocorrências no arquivo " + texto +
                    " em " + (end - start) + " ms");
        }
        long endTotal = System.currentTimeMillis();
        System.out.println("Tempo total Serial = " + (endTotal - startTotal) + " ms");
        return 0;
    }
}