# Java: Reflective Journal_CSV to TXT Converter

\*\***IMPORTANT**\*\*: This project is aimed at Year in Industry (university sandwich year) placement students, who may be required to keep a daily Reflective Journal, week-on-week.

---

**CSV**: *Comma-Separated Values file*

**TXT**: *Plain Text file*

---

A simple Java project to parse CSV inputs and generate TXT outputs.
 
Each CSV input stands for one week of a Reflective Journal, with 5 working days per CSV input (5 columns in each CSV file).

Each CSV input contains a certain number of questions answered each day ('x' number of rows in each CSV file).
 
Each TXT output will contain all the answers of a single question, from all CSV inputs.
 
In addition to the TXT outputs, a separate TXT file is generated, which denotes the difference (also known as the 'diff') between the most recent generated TXT outputs and the previously generated TXT outputs (what has been added and removed).

## How to run the CSV to TXT Converter:

Prerequisite:

[Install Java for your Mac or PC](https://java.com/en/download/help/download_options.xml)

(Mac OS)

1. Click "Clone or download" (top right), and unzip the downloaded zip file.

2. Open the "Terminal.app" application.

3. Navigate to the Java project directory. For example:

    (Do not type the `foo@bar:~$`, as this represents the Command Line prompt)

    ```console
    foo@bar:~$ cd ~/Documents/Reflective-Journal_CSV-to-TXT-Converter
    ```

4. Compile the Java 'Main' method:

    ```console
    foo@bar:~$ javac -cp ".:lib/*" Main.java
    ```

    This will produce a `Main.class` file in the same directory. This is a compiled version of the `Main.java` file.

5. Run the Java 'Main' method:

    ```console
    foo@bar:~$ java -cp ".:lib/*" Main
    ```
    
    When the Java 'Main' method has finished successfully, a message will be returned to the standard output:
    
    ```console
    CSV to TXT Converter complete!
    
    Please see the './outputs' directory for the TXT outputs.
    ```
    
    If you have generated TXT outputs previously, you will see an alternative message:
    
    ```console
    CSV to TXT Converter complete!
    
    Please see the './outputs' directory for the TXT outputs.
    
    Please see the './diff_outputs' directory for the difference between the old and most recent TXT outputs.
    ```

(Windows)

1. Click "Clone or download" (top right), and unzip the downloaded zip file.

2. Open the "Command Prompt" ("cmd.exe") application, which may need to be 'Run as Administrator'.

3. Navigate to the Java project directory. For example:

    (Do not type the `C:\Users\foobar>`, as this represents the Command Line prompt)

    ```bat
    C:\Users\foobar> cd ~
    C:\Users\foobar> cd Documents\Reflective-Journal_CSV-to-TXT-Converter
    ```

4. Compile the Java 'Main' method:

    ```bat
    C:\Users\foobar> javac -cp ".;lib/*" Main.java
    ```

    This will produce a `Main.class` file in the same directory. This is a compiled version of the `Main.java` file.

5. Run the Java 'Main' method:

    ```bat
    C:\Users\foobar> java -cp ".;lib/*" Main
    ```
    
    When the Java 'Main' method has finished successfully, a message will be returned to the standard output:
    
    ```console
    CSV to TXT Converter complete!
    
    Please see the './outputs' directory for the TXT outputs.
    ```
    
    If you have generated TXT outputs previously, you will see an alternative message:
    
    ```console
    CSV to TXT Converter complete!
    
    Please see the './outputs' directory for the TXT outputs.
    
    Please see the './diff_outputs' directory for the difference between the old and most recent TXT outputs.
    ```
