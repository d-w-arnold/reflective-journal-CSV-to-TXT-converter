import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David W. Arnold
 * @version 20/04/2021
 */
public class CSV_To_TXT_Converter
{
    // File extensions.
    private final String TXT_EXT = "txt";
    private final String CSV_EXT = "csv";
    // Custom string separators for writing to output files.
    private final String DELTA_SEP = "--";
    private final String OUTPUT_SEP = " " + DELTA_SEP + " ";
    // The separator character used in the CSV inputs (e.g ',').
    private final char CSV_SEP;
    // The character used to substitute for a separator character in output text
    //  (e.g. use a '/' char in place for a ',' char in output text, as ',' is used as the CSV_SEP instead).
    private final char CSV_SEP_SUB;
    // The directory containing CSV inputs.
    private final String INPUT_DIR;
    // The directory containing TXT outputs.
    private final String OUTPUT_DIR;
    // The directory containing the diff TXT files.
    private final String DIFF_DIR;
    // A temporary directory for the previous TXT outputs.
    private final String TMP_OUTPUT_DIR;
    // The default filename for all TXT outputs.
    private final String OUTPUT_FILE_DEFAULT_NAME;
    // The default filename for all diff TXT outputs.
    private final String DIFF_FILE_DEFAULT_NAME;
    // Number of questions asked each day in my Reflective Journal.
    private final int NUM_OF_QUESTIONS;
    // true: The previous diff TXT files will be deleted, so only the content of the most recent diff TXT files will be available.
    // false: The content of the previous diff TXT files will be added to the most recent diff TXT files.
    private final boolean CLEAR_DIFF_OUTPUTS;
    // true: Concatenated to the beginning of all output lines except, the first line, the OUTPUT_SEP string.
    // false: Do not concatenated to the beginning of any output lines.
    private final boolean INCLUDE_OUTPUT_SEP;

    public CSV_To_TXT_Converter(char sep, char sepSub, String inputDir, String outputDir, String diffDir, String outputFileDefaultName,
                                int numOfQuestions, boolean clearDiffOutputs, boolean includeOutputSep)
    {
        this.CSV_SEP = sep;
        this.CSV_SEP_SUB = sepSub;
        this.INPUT_DIR = "." + File.separator + inputDir;
        this.OUTPUT_DIR = "." + File.separator + outputDir;
        this.DIFF_DIR = "." + File.separator + diffDir;
        this.TMP_OUTPUT_DIR = "." + File.separator + "tmp_" + OUTPUT_DIR;
        this.OUTPUT_FILE_DEFAULT_NAME = outputFileDefaultName;
        this.DIFF_FILE_DEFAULT_NAME = "diff_" + OUTPUT_FILE_DEFAULT_NAME;
        this.NUM_OF_QUESTIONS = numOfQuestions;
        this.CLEAR_DIFF_OUTPUTS = clearDiffOutputs;
        this.INCLUDE_OUTPUT_SEP = includeOutputSep;
    }

    // CSV to TXT Converter, full execution.
    public void start() throws FileNotFoundException
    {
        // Creates the input and output directories, and records whether the output directory was created.
        boolean outputCreated = makeDirectory();

        // Checks if the outputs directory contains at least one TXT file, and deletes any non-TXT files.
        boolean outputContainsTxtFiles = false;
        for (File file : Objects.requireNonNull((new File(OUTPUT_DIR)).listFiles())) {
            if (!getFileExtension(file.toString()).equals(TXT_EXT)) {
                file.delete();
            } else {
                outputContainsTxtFiles = true;
            }
        }

        // If the outputs directory already existed, and so long as the outputs directory contains at least one TXT file.
        if (!outputCreated && outputContainsTxtFiles) {
            // Copy old TXT outputs to the temporary directory.
            try {
                copyDirectory(new File(OUTPUT_DIR), new File(TMP_OUTPUT_DIR), true, TXT_EXT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Delete old TXT outputs.
            deleteDirContents(new File(OUTPUT_DIR));
        }

        // Execute each TXT input to create a TXT output.
        File[] inputContents = new File(readyPath(INPUT_DIR)).listFiles();
        List<File> inputFiles = new ArrayList<>();
        assert inputContents != null;
        if (inputContents.length > 0) {
            for (File file : inputContents) {
                if (!file.isDirectory() && getFileExtension(file.toString()).equals(CSV_EXT)) {
                    inputFiles.add(file);
                }
            }
            if (!inputFiles.isEmpty()) {
                for (final File file : inputFiles) {
                    for (int i = 0; i < NUM_OF_QUESTIONS; i++) {
                        execute(file.toString(), i + 1);
                    }
                }
            } else {
                System.out.println("The '" + INPUT_DIR + "' directory does not contain any " + CSV_EXT.toUpperCase() + " inputs.");
                return;
            }
        } else {
            System.out.println("The '" + INPUT_DIR + "' directory is empty.");
            return;
        }

        // Create diff TXT files, if possible.
        if (!outputCreated && outputContainsTxtFiles) {
            // Creates the diff_outputs directory, if it doesn't already exist.
            File diffPath = new File(readyPath(DIFF_DIR));
            if (!diffPath.exists()) {
                diffPath.mkdir();
            }
            // Whether the user has specified to 'clearDiffOutputs' to be set to true or false.
            if (CLEAR_DIFF_OUTPUTS) {
                // Delete old diff TXT files.
                deleteDirContents(new File(DIFF_DIR));
                // Find diff between previous and most recent TXT outputs.
                for (int i = 0; i < NUM_OF_QUESTIONS; i++) {
                    String oldInputPath = readyPath(TMP_OUTPUT_DIR) + OUTPUT_FILE_DEFAULT_NAME + (i + 1) + "." + TXT_EXT;
                    String newInputPath = readyPath(OUTPUT_DIR) + OUTPUT_FILE_DEFAULT_NAME + (i + 1) + "." + TXT_EXT;
                    String diff_outputPath = readyPath(DIFF_DIR) + DIFF_FILE_DEFAULT_NAME + (i + 1) + "." + TXT_EXT;
                    delta(oldInputPath, newInputPath, diff_outputPath);
                }
            } else {
                // Add the content of the previous diff TXT files to the most recent diff TXT files.
                for (int i = 0; i < NUM_OF_QUESTIONS; i++) {
                    String oldInputPath = readyPath(TMP_OUTPUT_DIR) + OUTPUT_FILE_DEFAULT_NAME + (i + 1) + "." + TXT_EXT;
                    String newInputPath = readyPath(OUTPUT_DIR) + OUTPUT_FILE_DEFAULT_NAME + (i + 1) + "." + TXT_EXT;
                    String diff_outputPath = readyPath(DIFF_DIR) + DIFF_FILE_DEFAULT_NAME + (i + 1) + "." + TXT_EXT;
                    stackDelta(oldInputPath, newInputPath, diff_outputPath);
                }
            }
            // Delete the temporary directory containing the previous TXT outputs.
            deleteDir(new File(TMP_OUTPUT_DIR));
        }

        // Completion message sent to standard output.
        boolean diff_outputExists = new File(DIFF_DIR).exists();
        String completeMsg = "\n" + CSV_EXT.toUpperCase() + " to " + TXT_EXT.toUpperCase() + " Converter complete!\n" +
                "\nPlease see the '" + OUTPUT_DIR + "' directory for the " + TXT_EXT.toUpperCase() + " outputs.\n";
        if (diff_outputExists) {
            System.out.println(completeMsg +
                    "\nPlease see the '" + DIFF_DIR +
                    "' directory for the difference between the old and most recent " +
                    TXT_EXT.toUpperCase() + " outputs.\n");
        } else {
            System.out.println(completeMsg);
        }
    }

    // Creates input and output directories, if they don't already exist.
    private boolean makeDirectory()
    {
        boolean outputCreated = false;
        File inputPath = new File(readyPath(INPUT_DIR));
        if (!inputPath.exists()) {
            inputPath.mkdir();
        }
        File outputPath = new File(readyPath(OUTPUT_DIR));
        if (!outputPath.exists()) {
            outputPath.mkdir();
            outputCreated = true;
        }
        return outputCreated;
    }

    // Copies a directory from a source location to a target location.
    private void copyDirectory(File sourceLocation, File targetLocation, boolean specificExt, String extension) throws IOException
    {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            for (String file : Objects.requireNonNull(sourceLocation.list())) {
                if (getFileExtension(file).equals(extension)) {
                    copyDirectory(new File(sourceLocation, file), new File(targetLocation, file), specificExt, extension);
                }
            }
        } else {
            copyFiles(sourceLocation, targetLocation, specificExt, extension);
        }
    }

    // Copy only a CSV file from a source location to a target location.
    private void copyFiles(File sourceFile, File destFile, boolean specificExt, String extension) throws IOException
    {
        if (specificExt && getFileExtension(sourceFile.toString()).equals(extension)) {
            copyFile(sourceFile, destFile);
        } else if (!specificExt) {
            copyFile(sourceFile, destFile);
        }
    }

    // Returns the file extension of a file.
    private String getFileExtension(String path)
    {
        String fileName = new File(path).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    // Copy a file from a source location to a target location.
    private void copyFile(File sourceFile, File destFile) throws IOException
    {
        if (!sourceFile.exists()) {
            return;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source;
        FileChannel destination;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }
    }

    // Delete all files within a directory.
    private void deleteDirContents(File folder)
    {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
    }

    // Adds a '/' to the end of a path, if not already present.
    private String readyPath(String s)
    {
        if (s.substring(s.length() - 1).equals(File.separator)) {
            return s;
        } else {
            return s + File.separator;
        }
    }

    // Execute a CSV input to create a TXT output.
    private void execute(String filePath, int question)
    {
        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(filePath), CSV_SEP);
            String[] nextLine;
            int y = 0;
            // Read one line at a time
            while ((nextLine = reader.readNext()) != null) {
                if (y == question) {
                    int x = 0;
                    // Get all the answers for a question in the week
                    for (String cell : nextLine) {
                        if (x != 0) {
                            if (cell.equals("")) {
                                x++;
                            } else if (cell.contains("\n")) {
                                String singleOutputSeparator = "\n" + OUTPUT_SEP;
                                writeOutputFile(question, cell.replaceAll("\n", singleOutputSeparator));
                            } else {
                                writeOutputFile(question, cell);
                            }
                        } else {
                            x++;
                        }
                    }
                    break;
                } else {
                    y++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Write a string from a CSV file, and writes it to the bottom of a TXT file.
    private void writeOutputFile(int question, String str)
    {
        try {
            String fullPath = readyPath(OUTPUT_DIR) + OUTPUT_FILE_DEFAULT_NAME + question + "." + TXT_EXT;
            // Include OUTPUT_SEP only if INCLUDE_OUTPUT_SEP == true,
            //  and if it's not the first line of a TXT output file.
            if (INCLUDE_OUTPUT_SEP && new File(fullPath).exists()) {
                str = OUTPUT_SEP + str;
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(fullPath, true));
            // Write the answer for a question in the week to the bottom of the TXT file,
            //  and change all CSV_SEP_SUB to a CSV_SEP before writing.
            out.write(str.replaceAll(String.valueOf(CSV_SEP_SUB), String.valueOf(CSV_SEP)) + "\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Creates diff TXT files from scratch.
    // TODO Sort out diff TXT files not identifying duplicate answers in multiple TXT inputs
    //  (test1.csv and test3.csv inputs being the same answers, test3.csv gets added in the next run,
    //  but added answers not appearing in the diff TXT files).
    private void delta(String oldInputPath, String newInputPath, String diff_outputPath) throws FileNotFoundException
    {
        String sCurrentLine;
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        BufferedReader br1 = new BufferedReader(new FileReader(oldInputPath));
        BufferedReader br2 = new BufferedReader(new FileReader(newInputPath));
        try {
            while ((sCurrentLine = br1.readLine()) != null) {
                list1.add(sCurrentLine);
            }
            while ((sCurrentLine = br2.readLine()) != null) {
                list2.add(sCurrentLine);
            }
            List<String> tmpList = new ArrayList<>(list1);
            BufferedWriter out = new BufferedWriter(new FileWriter(diff_outputPath, true));
            out.write("Content removed from old file: (" + oldInputPath + ")\n\n");
            tmpList.removeAll(list2);
            for (String listEntry : tmpList) {
                out.write(listEntry + "\n");
            }
            out.write("\n" + DELTA_SEP + "\n\n");
            out.write("Content added to new file: (" + newInputPath + ")\n\n");
            tmpList = list2;
            tmpList.removeAll(list1);
            for (String listEntry : tmpList) {
                out.write(listEntry + "\n");
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Creates diff TXT files, including the contents of the previous diff TXT files.
    private void stackDelta(String oldInputPath, String newInputPath, String diff_outputPath) throws FileNotFoundException
    {
        List<String> lines = new ArrayList<>();

        File diff_outputFile = new File(diff_outputPath);
        if (!diff_outputFile.exists()) {
            delta(oldInputPath, newInputPath, diff_outputPath);
            return;
        }

        BufferedReader br = new BufferedReader(new FileReader(diff_outputFile));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            int i = 0;
            int customLine = 0;
            for (String l : lines) {
                if (l.equals(DELTA_SEP)) {
                    customLine = i - 1;
                } else {
                    i++;
                }
            }
            String sCurrentLine;
            List<String> list1 = new ArrayList<>();
            List<String> list2 = new ArrayList<>();
            BufferedReader br1 = new BufferedReader(new FileReader(oldInputPath));
            BufferedReader br2 = new BufferedReader(new FileReader(newInputPath));
            while ((sCurrentLine = br1.readLine()) != null) {
                list1.add(sCurrentLine);
            }
            while ((sCurrentLine = br2.readLine()) != null) {
                list2.add(sCurrentLine);
            }
            List<String> tmpList = new ArrayList<>(list1);
            List<String> tmpListA;
            List<String> tmpListB;
            tmpList.removeAll(list2);
            tmpListA = tmpList;
            tmpList = list2;
            tmpList.removeAll(list1);
            tmpListB = tmpList;
            for (String listEntry : tmpListA) {
                lines.add(customLine, listEntry);
                customLine++;
            }
            lines.addAll(tmpListB);
            List<String> head = lines.subList(0, customLine);
            List<String> tail = lines.subList(customLine, lines.size());
            List<String> tmpHead = removeDiffDuplicates(head, tail);
            List<String> tmpTail = removeDiffDuplicates(tail, head);
            lines = Stream.concat(tmpHead.stream(), tmpTail.stream()).collect(Collectors.toList());
            File x = new File(diff_outputPath);
            if (x.exists()) {
                x.delete();
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(diff_outputPath, true));
            for (String l : lines) {
                out.write(l + "\n");
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Remove entries which have been both added and removed in the same diff TXT file.
    private List<String> removeDiffDuplicates(List<String> targetList, List<String> removerList)
    {
        List<String> tmpHead = new ArrayList<>();
        boolean match;
        for (String entryA : targetList) {
            match = false;
            if (!entryA.equals("")) {
                for (String entryB : removerList) {
                    if (!entryB.equals("")) {
                        if (entryA.equals(entryB)) {
                            match = true;
                        }
                    }
                }
            }
            if (!match) {
                tmpHead.add(entryA);
            }
        }
        return tmpHead;
    }

    // Delete all files within a directory, and the directory itself.
    private void deleteDir(File folder)
    {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDir(file);
            }
        }
        folder.delete();
    }
}
