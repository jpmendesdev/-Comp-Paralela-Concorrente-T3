package GPU;
import org.jocl.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.CRC32;
import static org.jocl.CL.*;

public class ParallelalGPU {
    private static cl_device_id chooseDevice() {
        cl_platform_id[] platforms = new cl_platform_id[1];
        clGetPlatformIDs(platforms.length, platforms, null);
        for (cl_platform_id platform : platforms) {
            cl_device_id[] devices = new cl_device_id[1];
            int[] numDevices = new int[1];
            int err = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 1, devices, numDevices);
            if (err == CL_SUCCESS && numDevices[0] > 0) {
                return devices[0];
            }
        }
        for (cl_platform_id platform : platforms) {
            cl_device_id[] devices = new cl_device_id[1];
            int[] numDevices = new int[1];
            int err = clGetDeviceIDs(platform, CL_DEVICE_TYPE_CPU, 1, devices, numDevices);
            if (err == CL_SUCCESS && numDevices[0] > 0) {
                return devices[0];
            }
        }
        return null;
    }
    private static int crc32Int(String s) {
        CRC32 crc = new CRC32();
        crc.update(s.getBytes(StandardCharsets.UTF_8));
        long v = crc.getValue();
        return (int) v;
    }

    // Tokeniza e retorna array de ints (hashes)
    private static int[] tokenizeToHashes(String text) {
        String[] tokens = text.split("\\W+");
        int[] hashes = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i].toLowerCase();
            hashes[i] = crc32Int(t);
        }
        return hashes;
    }
    private static String readFileAsString(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
    public int runParallelGPU(String[] arrayTextos) throws IOException {
        Scanner sc = new Scanner(System.in);
        List<String> palavrasList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            System.out.println("Informe a palavra que deseja buscar no livro " + i);
            palavrasList.add(sc.next().toLowerCase());
        }
        final int[] targetHashes = new int[palavrasList.size()];
        for (int i = 0; i < palavrasList.size(); i++) {
            targetHashes[i] = crc32Int(palavrasList.get(i));
        }

        String programSource =
                "__kernel void matchKernel(__global const int *wordHashes, " +
                        "                          __global const int *targetHashes, " +
                        "                          const int targetCount, " +
                        "                          __global int *out) {" +
                        "    int gid = get_global_id(0);" +
                        "    int w = wordHashes[gid];" +
                        "    int found = 0;" +
                        "    for (int i = 0; i < targetCount; i++) {" +
                        "        if (w == targetHashes[i]) { found = 1; break; }" +
                        "    }" +
                        "    out[gid] = found;" +
                        "}";
        CL.setExceptionsEnabled(true);
        cl_device_id device = chooseDevice();
        if (device == null) {
            throw new RuntimeException("Nenhum dispositivo OpenCL encontrado.");
        }
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, getPlatformOfDevice(device));
        cl_context context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue commandQueue = clCreateCommandQueue(context, device, 0, null);
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{programSource}, null, null);
        int err = clBuildProgram(program, 0, null, null, null, null);
        if (err != CL_SUCCESS) {
            long[] logSize = new long[1];
            clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, 0, null, logSize);
            byte[] logData = new byte[(int) logSize[0]];
            clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, logSize[0], Pointer.to(logData), null);
            String log = new String(logData, 0, logData.length - 1);
            throw new RuntimeException("Erro ao compilar OpenCL:\n" + log);
        }
        cl_kernel kernel = clCreateKernel(program, "matchKernel", null);
        for (String texto : arrayTextos) {
            System.out.println("Processando (GPU) arquivo: " + texto);
            long start = System.nanoTime();
            String fileContent = readFileAsString(texto);
            int[] wordsHashes = tokenizeToHashes(fileContent);
            int n = wordsHashes.length;
            Pointer ptrWords = Pointer.to(wordsHashes);
            Pointer ptrTargets = Pointer.to(targetHashes);
            int[] out = new int[n];
            Pointer ptrOut = Pointer.to(out);
            cl_mem memWords = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_int * n, ptrWords, null);
            cl_mem memTargets = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_int * targetHashes.length, ptrTargets, null);
            cl_mem memOut = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                    Sizeof.cl_int * n, null, null);
            clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memWords));
            clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memTargets));
            clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{targetHashes.length}));
            clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memOut));
            long[] globalWorkSize = new long[]{n};
            clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, null, 0, null, null);
            clEnqueueReadBuffer(commandQueue, memOut, CL_TRUE, 0, Sizeof.cl_int * n, ptrOut, 0, null, null);
            int total = 0;
            for (int v : out) total += v;
            long end = System.nanoTime();
            long elapsedMs = (end - start) / 1_000_000;
            System.out.println("ParallelGPU: " + total + " ocorrências em " + elapsedMs + " ms (inclui preproc)");
            clReleaseMemObject(memWords);
            clReleaseMemObject(memTargets);
            clReleaseMemObject(memOut);
        }
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
        return 0;
    }

    private static cl_platform_id getPlatformOfDevice(cl_device_id device) {
        int[] numPlatformsArray = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        for (cl_platform_id p : platforms) {
            // get devices for platform
            int[] numDevicesArray = new int[1];
            int res = clGetDeviceIDs(p, CL_DEVICE_TYPE_ALL, 0, null, numDevicesArray);
            int num = (numDevicesArray[0]);
            cl_device_id[] devs = new cl_device_id[num];
            clGetDeviceIDs(p, CL_DEVICE_TYPE_ALL, num, devs, null);
            for (cl_device_id d : devs) {
                if (d.equals(device)) {
                    return p;
                }
            }
        }
        return null;
    }
}
