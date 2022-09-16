package ke.co.count.sirib2c.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ronald.langat
 */
public final class Prop {

    private transient Properties props;
    private transient List<String> loadErrors;
    private final transient String error1 = "ERROR: %s is <= 0 or may not have been set";
    private final transient String error2 = "ERROR: %s may not have been set";
    private static final String PROPS_FILE = System.getProperty("user.dir") + File.separator + "appconfig" + File.separator + "configurations.properties";

    private transient String LOGS_PATH;
    private transient String DATABASE_DRIVER;
    private transient String DATABASE_IP;
    private transient String DATABASE_PORT;
    private transient String DATABASE_NAME;
    private transient String DATABASE_USER;
    private transient String DATABASE_PASSWORD;
    private transient String DATABASE_SERVER_TIME_ZONE;
    private transient String SYSTEM_PORT;
    private transient String SYSTEM_HOST;
    //B2C
    private transient String SHORTCODE;
    private transient String COMMAND_ID;
    private transient String QUEUETIMEOUT_URL;
    private transient String RESULT_URL;
    private transient String TOKEN_URL;
    private transient String INITIATOR_NAME;
    private transient String B2C_URL_To;
    private transient String CONSUMER_KEY;
    private transient String CONSUMER_SECRET;
    private transient String SECURITY_CREDENTIAL;

    private transient String TRANSACTION_STATUS_URL;
    private transient String TRANSACTION_STATUS_RESULT_URL;

    /**
     * Instantiates a new Props.
     */
    public Prop() {
        loadProperties(PROPS_FILE);
    }

    private void loadProperties(final String propsFileName) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(propsFileName);
            props = new Properties();
            props.load(inputStream);

            LOGS_PATH = readString("LOGS_PATH").trim();

            DATABASE_DRIVER = readString("DATABASE_DRIVER").trim();
            DATABASE_IP = readString("DATABASE_IP").trim();
            DATABASE_PORT = readString("DATABASE_PORT").trim();
            DATABASE_NAME = readString("DATABASE_NAME").trim();
            DATABASE_USER = readString("DATABASE_USER").trim();
            DATABASE_PASSWORD = readString("DATABASE_PASSWORD").trim();
            DATABASE_SERVER_TIME_ZONE = readString("DATABASE_SERVER_TIME_ZONE").trim();
            SYSTEM_PORT = readString("SYSTEM_PORT").trim();
            SYSTEM_HOST = readString("SYSTEM_HOST").trim();

            SHORTCODE = readString("SHORTCODE").trim();
            COMMAND_ID = readString("COMMAND_ID").trim();
            QUEUETIMEOUT_URL = readString("QUEUETIMEOUT_URL").trim();
            RESULT_URL = readString("RESULT_URL").trim();
            TOKEN_URL = readString("TOKEN_URL").trim();
            INITIATOR_NAME = readString("INITIATOR_NAME").trim();
            B2C_URL_To = readString("B2C_URL_To").trim();
            CONSUMER_KEY = readString("CONSUMER_KEY").trim();
            CONSUMER_SECRET = readString("CONSUMER_SECRET").trim();
            SECURITY_CREDENTIAL = readString("SECURITY_CREDENTIAL").trim();

            TRANSACTION_STATUS_URL = readString("TRANSACTION_STATUS_URL").trim();
            TRANSACTION_STATUS_RESULT_URL = readString("TRANSACTION_STATUS_RESULT_URL").trim();

        } catch (IOException ex) {
            Logger.getLogger(Prop.class.getName()).log(Level.SEVERE, "ERROR: Failed to load properties file.\nCause: \n", ex);

        } catch (Exception ex) {
            Logger.getLogger(Prop.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(Prop.class.getName()).log(Level.SEVERE, "ERROR: Failed to load properties file.\nCause: \n", ex);
            }
        }
    }

    /**
     * Read string string. - This function reads a String from the properties
     * file
     *
     * @param propertyName the property name
     * @return the string
     */
    public String readString(String propertyName) {
        String property = props.getProperty(propertyName);
        if (property.isEmpty()) {
            getLoadErrors().add(String.format(error2, propertyName));
        }
        return property;
    }

    /**
     * Read integer. - This function gets a String property from the properties
     * file and parses it into and INT.
     *
     * @param propertyName the property name
     * @return the integer
     */
    public int readInt(String propertyName) {
        int property = 0;
        String propertyString = props.getProperty(propertyName);
        if (propertyString.isEmpty()) {
            getLoadErrors().add(String.format(error1, propertyName));
        } else {
            property = Integer.parseInt(propertyString);
            if (property < 0) {
                getLoadErrors().add(String.format(error1,
                        propertyName));
            }
        }
        return property;
    }

    /**
     * Read float float. - This function gets a String property from the
     * properties file and parses it into and FLOAT.
     *
     * @param propertyName the property name
     * @return the float
     */
    public float readFloat(String propertyName) {
        float property = 0;
        String propertyString = props.getProperty(propertyName);
        if (propertyString.isEmpty()) {
            getLoadErrors().add(String.format(error1, propertyName));
        } else {
            property = Float.parseFloat(propertyString);
            if (property < 0) {
                getLoadErrors().add(String.format(error1,
                        propertyName));
            }
        }
        return property;
    }

    /**
     * Read double double. - This function gets a String property from the
     * properties file and parses it into and DOUBLE.
     *
     * @param propertyName the property name
     * @return the double
     */
    public double readDouble(String propertyName) {
        double property = 0.0;
        String propertyString = props.getProperty(propertyName);
        if (propertyString.isEmpty()) {
            getLoadErrors().add(String.format(error1, propertyName));
        } else {
            property = Double.parseDouble(propertyString);
            if (property < 0) {
                getLoadErrors().add(String.format(error1,
                        propertyName));
            }
        }
        return property;
    }

    /**
     * Gets load errors.
     *
     * @return the load errors
     */
    public List<String> getLoadErrors() {
        return loadErrors;
    }

    /**
     * Gets logs path.
     *
     * @return the logs path
     */
    public String getLogsPath() {
        return LOGS_PATH;
    }

    public String getDATABASE_DRIVER() {
        return DATABASE_DRIVER;
    }

    public String getDATABASE_IP() {
        return DATABASE_IP;
    }

    public String getDATABASE_PORT() {
        return DATABASE_PORT;
    }

    public String getDATABASE_NAME() {
        return DATABASE_NAME;
    }

    public String getDATABASE_USER() {
        return DATABASE_USER;
    }

    public String getDATABASE_PASSWORD() {
        return DATABASE_PASSWORD;
    }

    public String getDATABASE_SERVER_TIME_ZONE() {
        return DATABASE_SERVER_TIME_ZONE;
    }

    public String getSYSTEM_PORT() {
        return SYSTEM_PORT;
    }

    public String getSYSTEM_HOST() {
        return SYSTEM_HOST;
    }

    public String getSHORTCODE() {
        return SHORTCODE;
    }

    public String getCOMMAND_ID() {
        return COMMAND_ID;
    }

    public String getQUEUETIMEOUT_URL() {
        return QUEUETIMEOUT_URL;
    }

    public String getRESULT_URL() {
        return RESULT_URL;
    }

    public String getINITIATOR_NAME() {
        return INITIATOR_NAME;
    }

    public String getB2C_URL_To() {
        return B2C_URL_To;
    }

    public String getCONSUMER_KEY() {
        return CONSUMER_KEY;
    }

    public String getCONSUMER_SECRET() {
        return CONSUMER_SECRET;
    }

    public String getSECURITY_CREDENTIAL() {
        return SECURITY_CREDENTIAL;
    }

    public String getTOKEN_URL() {
        return TOKEN_URL;
    }

    public String getTRANSACTION_STATUS_URL() {
        return TRANSACTION_STATUS_URL;
    }

    public String getTRANSACTION_STATUS_RESULT_URL() {
        return TRANSACTION_STATUS_RESULT_URL;
    }

    
}
