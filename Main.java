import au.com.bytecode.opencsv.CSVReader;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David W. Arnold
 * @version 31/08/2019
 */
public class Main
{
    // The Java main method, used to start the execution of the CSV to TXT Converter.
    public static void main(String[] args) throws FileNotFoundException
    {
        // The directory containing *.csv inputs.
        String input = "." + File.separator + "inputs";

        // The directory containing *.txt outputs.
        String output = "." + File.separator + "outputs";

        // The directory containing the diff *.txt files.
        String diff = "." + File.separator + "diff_outputs";

        // The separator used for *.csv inputs, because of this when needing to use a ',' in my Reflective Journal, I would use a '/' instead.
        char separator = ',';

        // Number of questions asked each day in my Reflective Journal.
        int numberOfQuestions = 22;

        // True: The previous diff *.txt files be deleted, so only the content of the most recent diff *.txt files will be available.
        // False: The content of the previous diff *.txt files will be added to the most recent diff *.txt files.
        boolean clearDiffOutputs = false;

        // TODO Implement a boolean, to decide whether *.txt outputs feature a line separator concatenated to the beginning of all line except the first line.
        // Different types of separators used, all in one place, so when used multiple times throughout the java file them stay consistent.
        String deltaSeparator = "--";
        String outputSeparator = " " + deltaSeparator + " ";
        String singleOutputSeparator = "\n" + outputSeparator;

        // Call the executeFiles method.
        executeFiles(input, output, diff, separator, numberOfQuestions, clearDiffOutputs, outputSeparator, singleOutputSeparator, deltaSeparator);
    }

    // The full execution of the CSV to TXT Converter.
    private static void executeFiles(
            String input,
            String output,
            String diff,
            char separator,
            int numOfQuestions,
            boolean clearDiffOutputs,
            String outputSeparator,
            String singleOutputSeparator,
            String deltaSeparator) throws FileNotFoundException
    {
        // A temporary directory for the previous *.txt outputs.
        String tmp = "." + File.separator + "tmp_outputs";

        // Creates the input and output directories, and records whether the output directory was created.
        boolean outputCreated = makeDirectory(input, output);

        // Checks if the outputs directory contains at least one *.txt file, and deletes any non *.txt files.
        boolean outputContainsTxtFiles = false;
        for (File file : (new File(output)).listFiles()) {
            if (!getFileExtension(file.toString()).equals("txt")) {
                file.delete();
            } else {
                outputContainsTxtFiles = true;
            }
        }

        // If the outputs directory already existed, and so long as the outputs directory contains at least one *.txt file.
        if (!outputCreated && outputContainsTxtFiles) {
            // Copy old *.txt outputs to the temporary directory.
            try {
                copyDirectory(new File(output), new File(tmp), true, "txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Delete old *.txt outputs.
            deleteDirContents(new File(output));
        }

        // Execute each *.csv input to create a *.txt output.
        File[] inputContents = new File(readyPath(input)).listFiles();
        List<File> inputFiles = new ArrayList<>();
        if (inputContents.length > 0) {
            for (File file: inputContents) {
                if (!file.isDirectory() && getFileExtension(file.toString()).equals("csv")) {
                    inputFiles.add(file);
                }
            }
            if (!inputFiles.isEmpty()) {
                for (final File file : inputFiles) {
                    for (int i = 0; i < numOfQuestions; i++) {
                        execute(file.toString(), i + 1, separator, output, outputSeparator, singleOutputSeparator);
                    }
                }
            } else {
                System.out.println("The Input directory does not contain any *.csv inputs.");
                return;
            }
        } else {
            System.out.println("The Input directory is empty.");
            return;
        }

        // Create diff *.txt files, if possible.
        if (!outputCreated && outputContainsTxtFiles) {
            // Creates the diff_outputs directory, if it doesn't already exist.
            File diffPath = new File(readyPath(diff));
            if(!diffPath.exists()) {
                diffPath.mkdir();
            }
            // Whether the user has specified to 'clearDiffOutputs' to be set to true or false.
            if (clearDiffOutputs) {
                // Delete old diff *.txt files.
                deleteDirContents(new File(diff));
                // Find diff between previous and most recent *.txt outputs.
                for (int i = 0; i < numOfQuestions; i++) {
                    String oldInputPath = readyPath(tmp) + "question" + (i + 1) + ".txt";
                    String newInputPath = readyPath(output) + "question" + (i + 1) + ".txt";
                    String diff_outputPath = readyPath(diff) + "diff_question" + (i + 1) + ".txt";
                    delta(oldInputPath, newInputPath, diff_outputPath, deltaSeparator);
                }
            } else {
                // Add the content of the previous diff *.txt files to the most recent diff *.txt files.
                for (int i = 0; i < numOfQuestions; i++) {
                    String oldInputPath = readyPath(tmp) + "question" + (i + 1) + ".txt";
                    String newInputPath = readyPath(output) + "question" + (i + 1) + ".txt";
                    String diff_outputPath = readyPath(diff) + "diff_question" + (i + 1) + ".txt";
                    stackDelta(oldInputPath, newInputPath, diff_outputPath, deltaSeparator);
                }
            }
            // Delete the temporary directory containing the previous *.txt outputs.
            deleteDir(new File(tmp));
        }

        // Completion message sent to standard output.
        boolean diff_outputExists = new File(diff).exists();
        if (diff_outputExists) {
            System.out.println("\nCSV to TXT Converter complete!\n\nPlease see the './outputs' directory for the TXT outputs.\n\nPlease see the './diff_outputs' directory for the difference between the old and most recent TXT outputs.\n");
        } else {
            System.out.println("\nCSV to TXT Converter complete!\n\nPlease see the './outputs' directory for the TXT outputs.\n");
        }
    }

    //Creates input and output directories, if they don't already exist.
    private static boolean makeDirectory(String input, String output)
    {
        boolean outputCreated = false;
        File inputPath = new File(readyPath(input));
        if(!inputPath.exists()) {
            inputPath.mkdir();
        }
        File outputPath = new File(readyPath(output));
        if(!outputPath.exists()) {
            outputPath.mkdir();
            outputCreated = true;
        }
        return outputCreated;
    }

    // Copies a directory from a source location to a target location.
    private static void copyDirectory(File sourceLocation, File targetLocation, boolean specificExt, String extension) throws IOException
    {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            for (String file : sourceLocation.list()) {
                if (getFileExtension(file).equals(extension)) {
                    copyDirectory(new File(sourceLocation, file), new File(targetLocation, file), specificExt, extension);
                }
            }
        } else {
            copyFiles(sourceLocation, targetLocation, specificExt, extension);
        }
    }

    // Copy only a *.csv file from a source location to a target location.
    private static void copyFiles(File sourceFile, File destFile, boolean specificExt, String extension) throws IOException
    {
        if (specificExt && getFileExtension(sourceFile.toString()).equals(extension)) {
            copyFile(sourceFile, destFile);
        } else if (!specificExt) {
            copyFile(sourceFile, destFile);
        }
    }

    // Returns the file extension of a file.
    private static String getFileExtension(String path)
    {
        String fileName = new File(path).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    // Copy a file from a source location to a target location.
    private static void copyFile(File sourceFile, File destFile) throws IOException
    {
        if (!sourceFile.exists()) {
            return;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
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
    private static void deleteDirContents(File folder)
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
    private static String readyPath(String s)
    {
        if (s.substring(s.length() - 1).equals(File.separator)) {
            return s;
        } else {
            return s + File.separator;
        }
    }

    // Execute a *.csv input to create a *.txt output.
    private static void execute(String filePath, int question, char separator, String outputs, String outputSeparator, String singleOutputSeparator)
    {
        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(filePath), separator);
            String[] nextLine;
            int y = 0;
            //Read one line at a time
            while ((nextLine = reader.readNext()) != null) {
                if (y == question) {
                    int x = 0;
                    // Get all the answers for a question in the week
                    for (String cell : nextLine) {
                        if (x != 0) {
                            if (cell.equals("")) {
                                x++;
                            } else if (cell.contains("\n")) {
                                writeOutputFile(question, cell.replaceAll("\n", singleOutputSeparator), outputs, outputSeparator);
                            } else {
                                writeOutputFile(question, cell, outputs, outputSeparator);
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

    // Write a string from a *.csv file, and writes it to the bottom of a *.txt file.
    private static void writeOutputFile(int question, String str, String outputs, String outputSeparator)
    {
        try {
            String fullPath = readyPath(outputs) + "question" + question + ".txt";
            if (new File(fullPath).exists()) {
                str = outputSeparator + str;
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(fullPath, true));
            // Change all '/' to a ',', and write the answer for a question in the week to the bottom of the *.txt file.
            out.write(str.replaceAll("/", ",") + "\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Creates diff *.txt files from scratch.
    // TODO Sort out diff *.txt files not identifying duplicate answers in multiple *.txt inputs (test1.csv and test3.csv inputs being the same answers, test3.csv gets added in the next run, but added answers not appearing in the diff *.txt files).
    private static void delta(String oldInputPath, String newInputPath, String diff_outputPath, String deltaSeparator) throws FileNotFoundException
    {
        String sCurrentLine;
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        BufferedReader br1 = new BufferedReader(new FileReader(new File(oldInputPath)));
        BufferedReader br2 = new BufferedReader(new FileReader(new File(newInputPath)));
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
            out.write("\n" + deltaSeparator + "\n\n");
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

    // Creates diff *.txt files, including the contents of the previous diff *.txt files.
    private static void stackDelta(String oldInputPath, String newInputPath, String diff_outputPath, String deltaSeparator) throws FileNotFoundException
    {
        List<String> lines = new ArrayList<>();

        File diff_outputFile = new File(diff_outputPath);
        if (!diff_outputFile.exists()) {
            delta(oldInputPath, newInputPath, diff_outputPath, deltaSeparator);
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
                if (l.equals(deltaSeparator)) {
                    customLine = i - 1;
                } else {
                    i++;
                }
            }
            String sCurrentLine;
            List<String> list1 = new ArrayList<>();
            List<String> list2 = new ArrayList<>();
            BufferedReader br1 = new BufferedReader(new FileReader(new File(oldInputPath)));
            BufferedReader br2 = new BufferedReader(new FileReader(new File(newInputPath)));
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
            for (String listEntry : tmpListB) {
                lines.add(listEntry);
            }
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

    // Remove entries which have been both added and removed in the same diff *.txt file.
    private static List<String> removeDiffDuplicates(List<String> targetList, List<String> removerList)
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
    private static void deleteDir(File folder)
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
