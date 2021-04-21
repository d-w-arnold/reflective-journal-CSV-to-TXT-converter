import java.io.FileNotFoundException;

/**
 * @author David W. Arnold
 * @version 31/08/2019
 */
public class Main
{
    public static void main(String[] args)
    {
        try {
            CSV_To_TXT_Converter converter = new CSV_To_TXT_Converter(
                    ',', '/', "inputs", "outputs",
                    "diff_outputs", "question",
                    22, false, true
            );
            converter.start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
