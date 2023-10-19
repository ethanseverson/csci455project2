package FundraiserServer;

/**
 * Print a neatly generated ACSII table from a 2D String Array.
 * Includes options for padding, headers, and table title.
 *
 * @author Ethan Severson
 * @date 20220315
 * Created for CSCI 161 at NDSU
 */
public class ASCIITableCreator {

    /**
     * Print a neatly generated ACSII table from a 2D String Array.
     *
     * @param input The 2D String Array to print
     * @param cellPadding The amount of cell padding on the left and right of
     * each element
     * @param separatorLines Print a separator line after the first row
     * @param tableHeader Print a table header (if null then header will be
     * skipped)
     * @param columnHeaders 1D String Array of the column headers, "null" to
     * disable.
     * @param respectHeaders When set to true, the columns will widen to fit the
     * table title and column headers (if applicable)
     * @param formatNumbers When set to true, numeric values will be converted
     * to have a 2 decimal percision and comma seperation.
     */
    public static void print(String[][] input, int cellPadding, boolean separatorLines, String tableHeader, String[] columnHeaders, boolean respectHeaders, boolean formatNumbers) {
        if (input.length > 0) {
            if (input[0].length > 0) {
                String[][] sign = new String[input.length][input[0].length];
                if (formatNumbers) {
                    //convert numbers with comma and 2 decimal percision
                    for (int i = 0; i < input.length; i++) {
                        for (int j = 0; j < input[0].length; j++) {
                            if (input[i][j] == null) {
                                throw new NullPointerException("ACSII Table cannot be generated since the input array contains a null value.");
                            }
                            try {
                                float number = Float.parseFloat(input[i][j]);
                                input[i][j] = String.format("%,.0f", number);
                                sign[i][j] = "";
                            } catch (NumberFormatException e) {
                                sign[i][j] = "-";
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < input.length; i++) {
                        for (int j = 0; j < input[0].length; j++) {
                            if (input[i][j] == null) {
                                throw new NullPointerException("ACSII Table cannot be generated since the input array contains a null value.");
                            }
                            sign[i][j] = "-";
                        }
                    }
                }

                int[] cellWidths = findColumnWidths(input); //Calculate column widths

                if (respectHeaders && !(columnHeaders == null)) {
                    if (columnHeaders.length != input[0].length) {
                        throw new NullPointerException("Column headers input does not match data column count.");
                    }
                    for (int j = 0; j < cellWidths.length; j++) {
                        if (cellWidths[j] < columnHeaders[j].length()) {
                            cellWidths[j] = columnHeaders[j].length();
                        }
                    }
                }

                if (!(tableHeader == null || tableHeader.equals(""))) {
                    int width = 1;
                    for (int i = 0; i < cellWidths.length; i++) {
                        width += (cellPadding * 2) + cellWidths[i] + 1;
                    }
                    if (tableHeader.length() > (width - 4)) {
                        if (respectHeaders) {
                            int diff = tableHeader.length() - (width - 4);
                            while (diff > 0) {
                                for (int j = 0; j < cellWidths.length; j++) {
                                    if (diff > 0) {
                                        cellWidths[j] += 1;
                                        diff--;
                                        width++;
                                    }
                                }
                            }

                        } else {
                            tableHeader = tableHeader.substring(0, (width - 7)) + "...";
                        }
                    }
                    int pad = (width - (tableHeader.length() + 4)) / 2;
                    String padStr = " ";
                    for (int p = 0; p < pad; p++) {
                        padStr += " ";
                    }
                    String padStrR = padStr;
                    if ((width - (tableHeader.length() + 4)) % 2 == 1) {
                        padStrR += " ";
                    }
                    System.out.printf("+");
                    for (int d = 0; d < (width - 2); d++) {
                        System.out.printf("-");
                    }
                    System.out.printf("+\n|%s%s%s|\n", padStr, tableHeader, padStrR);
                }

                printSeparator(cellWidths, cellPadding); //Print seperator line (header)

                //Create cell padding string to use before and after each element
                //(not the cleanest way and avoids using printf options)
                String cellPad = "";
                for (int p = 0; p < cellPadding; p++) {
                    cellPad += " ";
                }
                //Column headers
                if (!(columnHeaders == null)) {
                    if (columnHeaders.length != input[0].length) {
                        throw new NullPointerException("Column headers input does not match data column count.");
                    } else {
                        for (int c = 0; c < input[0].length; c++) {
                            String header = columnHeaders[c];
                            int cellWidth = (cellPadding * 2) + cellWidths[c];
                            if (header.length() > cellWidths[c]) {
                                header = header.substring(0, (cellWidth - 7)) + "...";
                            }
                            int pad = (cellWidth - (header.length() + 2)) / 2;
                            String padStr = " ";
                            for (int p = 0; p < pad; p++) {
                                padStr += " ";
                            }
                            String padStrR = padStr;
                            if ((cellWidth - (header.length() + 2)) % 2 == 1) {
                                padStrR += " ";
                            }

                            System.out.printf("|%s%s%s", padStr, header, padStrR);
                        }
                        System.out.printf("|\n");
                        printSeparator(cellWidths, cellPadding); //Print seperator line (cell header)
                    }
                }

                //MAIN PRINT LOOP
                for (int i = 0; i < input.length; i++) {

                    //If containsRowHeaders is enabled, a seperator line will be outputed after the first row of data
                    if (i >= 1 && separatorLines) {
                        printSeparator(cellWidths, cellPadding);
                    }
                    //For each element loop
                    for (int j = 0; j < input[i].length; j++) {
                        System.out.printf("|%s%" + sign[i][j] + cellWidths[j] + "s%s", cellPad, input[i][j], cellPad); //Printing the element
                    }
                    System.out.printf("|\n");

                }

                printSeparator(cellWidths, cellPadding); //Print seperator line (footer)

            }
        }
    }

    private static void printSeparator(int[] cellWidths, int cellPadding) {
        for (int c = 0; c < cellWidths.length; c++) {
            System.out.printf("+");
            for (int d = 0; d < cellWidths[c] + (cellPadding * 2); d++) {
                System.out.printf("-");
            }
        }
        System.out.printf("+\n");
    }

    private static int[] findColumnWidths(String[][] input) {
        int[] cellWidths = new int[input[0].length];

        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[i].length; j++) {
                if (input[i][j] == null) {
                    throw new NullPointerException("ACSII Table cannot be generated since the input array contains a null value.");
                }

                if (cellWidths[j] < input[i][j].length()) {
                    cellWidths[j] = input[i][j].length();
                }
            }
        }

        return cellWidths;
    }

    //Float constructor
    /**
     * Print a neatly generated ACSII table from a 2D float Array.
     *
     * @param input The 2D String Array to print
     * @param cellPadding The amount of cell padding on the left and right of
     * each element
     * @param separatorLines Print a separator line after the first row
     * @param tableHeader Print a table header (if null then header will be
     * skipped)
     * @param columnHeaders 1D String Array of the column headers, "null" to
     * disable.
     * @param respectHeaders When set to true, the columns will widen to fit the
     * table title and column headers (if applicable)
     * @param formatNumbers When set to true, numeric values will be converted
     * to have a 2 decimal percision and comma seperation.
     */
    public static void print(float[][] input, int cellPadding, boolean separatorLines, String tableHeader, String[] columnHeaders, boolean respectHeaders, boolean formatNumbers) {
        if (input.length > 0) {
            if (input[0].length > 0) {
                String[][] strInput = new String[input.length][input[0].length];
                for (int i = 0; i < input.length; i++) {
                    for (int j = 0; j < input[i].length; j++) {
                        strInput[i][j] = String.valueOf(input[i][j]);
                    }
                }
                print(strInput, cellPadding, separatorLines, tableHeader, columnHeaders, respectHeaders, formatNumbers);
            }
        }
    }

    //Double constructor
    /**
     * Print a neatly generated ACSII table from a 2D double Array.
     *
     * @param input The 2D String Array to print
     * @param cellPadding The amount of cell padding on the left and right of
     * each element
     * @param separatorLines Print a separator line after the first row
     * @param tableHeader Print a table header (if null then header will be
     * skipped)
     * @param columnHeaders 1D String Array of the column headers, "null" to
     * disable.
     * @param respectHeaders When set to true, the columns will widen to fit the
     * table title and column headers (if applicable)
     * @param formatNumbers When set to true, numeric values will be converted
     * to have a 2 decimal percision and comma seperation.
     */
    public static void print(double[][] input, int cellPadding, boolean separatorLines, String tableHeader, String[] columnHeaders, boolean respectHeaders, boolean formatNumbers) {
        if (input.length > 0) {
            if (input[0].length > 0) {
                String[][] strInput = new String[input.length][input[0].length];
                for (int i = 0; i < input.length; i++) {
                    for (int j = 0; j < input[i].length; j++) {
                        strInput[i][j] = String.valueOf(input[i][j]);
                    }
                }
                print(strInput, cellPadding, separatorLines, tableHeader, columnHeaders, respectHeaders, formatNumbers);
            }
        }
    }
}
