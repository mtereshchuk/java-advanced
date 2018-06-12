package ru.ifmo.rain.tereshchuk.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walk {

    private final Path inputFile, outputFile;

    public Walk(String inputFileName, String outputFileName) throws WalkException {
        try {
            inputFile = Paths.get(inputFileName);
        } catch (InvalidPathException e) {
            throw new WalkException("Incorrect input file name");
        }
        try {
            outputFile = Paths.get(outputFileName);
            if (Files.notExists(outputFile)) {
                try {
                    if (outputFile.getParent() != null) {
                        Files.createDirectories(outputFile.getParent());
                    }
                    Files.createFile(outputFile);
                } catch (IOException e) {
                    throw new WalkException("Incorrect output file");
                }
            }
        } catch (InvalidPathException e) {
            throw new WalkException("Incorrect output file name");
        }
    }

    private void doWalk() throws WalkException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile.toFile()), "UTF-8"))) {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile.toFile()), "UTF-8"))) {
                String fileName = "";
                try {
                    while ((fileName = reader.readLine()) != null) {
                        File curFile = new File(fileName);
                        int hashSum = 0;
                        if (!curFile.exists()) {
                            System.err.println("file " + fileName + " does not exist");
                        } else {
                            hashSum = getHashSum(curFile);
                        }
                        try {
                            writer.printf("%08x %s\n", hashSum, fileName);
                        } catch (Exception e) {
                            throw new WalkException("error writing to output file");
                        }
                    }
                } catch (IOException e) {
                    throw new WalkException("error reading from file " + fileName);
                }
            } catch (IOException e) {
                throw new WalkException("can not open output file");
            }
        } catch (IOException e) {
            throw new WalkException("can not open input file");
        }
    }

    private final int FNV_32_INITIAL = 0x811c9dc5;
    private final int FNV_32_PRIME = 0x01000193;

    private int getHashSum(File file) {
        int hashSum = FNV_32_INITIAL;
        try (FileInputStream is = new FileInputStream(file)) {
            byte[] b = new byte[1024];
            int c = 0;
            while ((c = is.read(b)) >= 0) {
                for (int i = 0; i < c; i++) {
                    hashSum = (hashSum * FNV_32_PRIME) ^ (b[i] & 0xff);
                }
            }
        } catch (IOException e) {
            System.err.println("error reading file " + file.getName());
            hashSum = 0;
        }
        return hashSum;
    }

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                throw new WalkException("incorrect number of arguments");
            }
            Walk walk = new Walk(args[0], args[1]);
            walk.doWalk();
        } catch (WalkException e) {
            System.err.println(e.getMessage());
        }
    }
}