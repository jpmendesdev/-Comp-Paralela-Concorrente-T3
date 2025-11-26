package CPU;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class ParallelalCPU {
    Scanner sc = new Scanner(System.in);
    public int runParallelCPU(String[] arrayTextos) throws IOException, InterruptedException, ExecutionException {
        Scanner sc = new Scanner(System.in);
        List<String> palavrasList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            System.out.println("Informe a palavra que deseja buscar no livro " + i);
            palavrasList.add(sc.next().toLowerCase());
        }
        long startTotal = System.currentTimeMillis();
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (String texto : arrayTextos) {
            long start = System.currentTimeMillis();
            BufferedReader br = new BufferedReader(new FileReader(texto));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            String[] palavras = sb.toString().split("\\W+");
            int chunkSize = palavras.length / numThreads;
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                int startIdx = i * chunkSize;
                int endIdx = (i == numThreads - 1) ? palavras.length : startIdx + chunkSize;
                futures.add(executor.submit(() -> {
                    int localCount = 0;
                    for (int j = startIdx; j < endIdx; j++) {
                        String palavraTexto = palavras[j].toLowerCase();
                        for (String buscada : palavrasList)
                            if (buscada.equals(palavraTexto))
                                localCount++;
                    }
                    return localCount;
                }));
            }
            int total = 0;
            for (Future<Integer> f : futures) total += f.get();
            long end = System.currentTimeMillis();
            System.out.println("Parallel: " + total + " ocorrências no arquivo " + texto +
                    " em " + (end - start) + " ms");
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        long endTotal = System.currentTimeMillis();
        System.out.println("Tempo total Parallel = " + (endTotal - startTotal) + " ms");
        return 0;
    }

}
