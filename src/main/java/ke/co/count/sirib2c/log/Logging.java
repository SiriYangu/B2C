package ke.co.count.sirib2c.log;

import ke.co.count.sirib2c.main.SiriB2C;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Logging {

    private static String LOGS_PATH;

    public Logging() {
    }

    static {
        LOGS_PATH = "";
    }

    public void applicationLog(String details, String uniqueId, int logLevel) {
        LOGS_PATH = System.getProperty("user.dir") + File.separator + SiriB2C.LOGS_PATH;

        String typeOfLog = "";
        switch (logLevel) {
            case 1:
                typeOfLog = "PICKED-TRANSACTIONS-TABLE";
                break;
            case 2:
                typeOfLog = "WRITTEN-TO-MPESA-OUT-TABLE";
                break;
            case 3:
                typeOfLog = "SENT-TO-SAFARICOM";
                break;
            case 4:
                typeOfLog = "ACK-RESPONSE-FROM-SAFARICOM";
                break;
            case 5:
                typeOfLog = "APPLICATION-ERRORS";
                break;
            case 6:
                typeOfLog = "CALLBACK-RESPONSE-FROM-SAF";
                break;
            case 7:
                typeOfLog = "PICKED-FROM-MPESA-OUT-TABLE";
                break;
            case 8:
                typeOfLog = "ERROR_FETCHING";
                break;
            case 9:
                typeOfLog = "DATABASE";
                break;
            default:
                typeOfLog = "Others";
                break;
        }

        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String LogDate = formatter.format(today);
        SimpleDateFormat LogTimeformatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String LogTime = LogTimeformatter.format(today);
        File dir = new File(LOGS_PATH + File.separator + typeOfLog + File.separator + LogDate);
        BufferedWriter writer = null;
        if (dir.exists()) {
            dir.setWritable(true);
        } else {
            boolean mkdirs = dir.mkdirs();
            dir.setWritable(true);
        }

        String randomNum = "";
        int maximum = 100000;

        try {
            if (uniqueId.equals("")) {
                String minimum = "5";
                randomNum = minimum + (int) (Math.random() * maximum);
                uniqueId = randomNum;

            } else {
                randomNum = "10" + (int) (Math.random() * maximum);
            }

            String fileName = "";

            File[] fileList = dir.listFiles();

            if (fileList.length > 0) {
                for (File fileList1 : fileList) {
                    if (fileList1.length() < 25000000) {
                        fileName = File.separator + fileList1.getName();
                    } else {
                        fileName = File.separator + LogDate + "-" + uniqueId + ".log";
                    }
                }
            } else {
                fileName = File.separator + LogDate + "-" + randomNum + ".log";
            }

            writer = new BufferedWriter(new FileWriter(dir + fileName, true));
            writer.write(LogTime + " ~ " + details);
            writer.newLine();
        } catch (IOException e) {
            System.err.printf(logPreString() + "ERROR IN LOGS:-" + e.getMessage(), e);
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (IOException e) {
                System.err.printf(logPreString() + "ERROR IN LOGS:-" + e.getMessage(), e);
            }
        }
    }

    /**
     * Log pre string.
     *
     * @return the string
     */
    public String logPreString() {
        return "MPESAB2C.V2 | " + Thread.currentThread().getStackTrace()[2].getClassName() + " | "
                + Thread.currentThread().getStackTrace()[2].getLineNumber() + " | "
                + Thread.currentThread().getStackTrace()[2].getMethodName() + "() | ";
    }
}
