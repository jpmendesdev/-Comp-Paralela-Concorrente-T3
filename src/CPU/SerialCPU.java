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
        int count = 0;
        List<String> palavrasList = new ArrayList();
        for (int i = 0; i < 3; i++) {
            System.out.println("Informe a palavra que deseja buscar no livro" + i);
            String palavra = sc.next();
            palavrasList.add(palavra.toLowerCase());
        }
        for (String texto : arrayTextos) {
            BufferedReader br = new BufferedReader(new FileReader(texto));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                String[] everythingArray = everything.split(" ");
                for (String ev : everythingArray) {
                    for (String palavra : palavrasList) {
                        if (palavra.equals(ev.toLowerCase())) {
                            count++;
                        }
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                br.close();
            }
            System.out.println(count + "resultados para o texto" + texto);
            count = 0;
        }
        return count;
    }
}
