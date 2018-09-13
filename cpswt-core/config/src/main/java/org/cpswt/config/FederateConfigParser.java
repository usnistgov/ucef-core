package org.cpswt.config;

import org.cpswt.utils.CpswtDefaults;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.ClassUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Parser for Federate parameters
 */
public class FederateConfigParser {
    private static final Logger logger = LogManager.getLogger(FederateConfigParser.class);

    private Options cliOptions;
    private Option configFileOption;

    /**
     * Default constructor
     */
    public FederateConfigParser() {
        this.cliOptions = new Options();

        this.configFileOption = Option.builder()
                .longOpt(CpswtDefaults.ConfigFileOptionName)
                .argName(CpswtDefaults.ConfigFileOptionName)
                .hasArg()
                .required(false)
                .type(String.class)
                .build();
    }

    /**
     * Instantiate a parser with pre-defined CLI options.
     *
     * @param cliOptions
     */
    public FederateConfigParser(Options cliOptions) {
        this.cliOptions = cliOptions;
    }

    /**
     * Add more CLI options to the existing ones.
     *
     * @param options
     */
    public void addCLIOptions(Options options) {
        for (Option opt : options.getOptions()) {
            this.cliOptions.addOption(opt);
        }
    }

    /**
     * Add more CLI option to the existing ones.
     *
     * @param option
     */
    public void addCLIOption(Option option) {
        this.cliOptions.addOption(option);
    }

    static final Set<Class<?>> supportedCLIArgTypes = new HashSet();

    static {
        supportedCLIArgTypes.add(Double.class);
        supportedCLIArgTypes.add(Integer.class);
        supportedCLIArgTypes.add(Boolean.class);
        supportedCLIArgTypes.add(Long.class);
    }

    /**
     * Parse the command line arguments provided to the main function.
     *
     * @param args     Command line arguments provided to 'main'
     * @param clazz    The class that represents the federate parameter.
     * @param <TParam> The generic type of the federate parameter.
     * @return An instance of the class that represents the federate parameter.
     */
    public <TParam extends FederateConfig> TParam parseArgs(final String[] args, final Class<TParam> clazz) {

        CommandLineParser parser = new DefaultParser();

        try {
            // get cli options for the class
            Options opts = this.getClassCLIOptions(clazz);

            // merge with pre-defined options
            for (Option opt : this.cliOptions.getOptions()) opts.addOption(opt);
            // add the special configFile option

            opts.addOption(this.configFileOption);

            // parse args
            CommandLine commandLine = parser.parse(opts, args);

            // get parsed parameter
            TParam currentParameter = this.parseCommandLine(commandLine, clazz);

            return currentParameter;
        } catch (ParseException parseExp) {
            logger.error("Parsing CLI arguments failed. Reason: " + parseExp.getMessage(), parseExp);
            System.exit(-1);
        }

        return null;

    }

    <TParam extends FederateConfig> TParam parseCommandLine(CommandLine commandLine, final Class<TParam> clazz) {
        File configFile = null;

        // get the "configFile" parameter from the command line
        String mConfigFilePath = commandLine.getOptionValue(CpswtDefaults.ConfigFileOptionName);

        TParam federateParameter = null;

        // fallback to default config from resources
        if (mConfigFilePath == null || mConfigFilePath.isEmpty()) {
            logger.trace("configFile CLI parameter not provided");
            logger.trace("Trying to load {} as a resource of {} class.", CpswtDefaults.FederateConfigDefaultResource, clazz.getName());

            ClassLoader classLoader = clazz.getClassLoader();
            URL resource = classLoader.getResource(CpswtDefaults.FederateConfigDefaultResource);

            // fallback to environment variable definition
            if (resource == null) {
                logger.trace("No resource found for class {}", clazz.getName());
                logger.trace("Trying to load configFile set in {} environment variable.", CpswtDefaults.FederateConfigEnvironmentVariable);
                String configFileFromEnv = System.getenv(CpswtDefaults.FederateConfigEnvironmentVariable);
                if (configFileFromEnv != null) {
                    logger.trace("{} environment variable set, loading config file {}", CpswtDefaults.FederateConfigEnvironmentVariable, configFileFromEnv);
                    configFile = new File(configFileFromEnv);
                }
            } else {
                logger.trace("Resource found. Loading {}.", resource.getPath());
                configFile = new File(resource.getFile());
            }
        }
        // load passed config file
        else {
            logger.trace("Trying to load config file {}.", mConfigFilePath);

            configFile = new File(mConfigFilePath.trim());
        }

        if (configFile != null && configFile.exists()) {

            try {
                federateParameter = ConfigParser.parseFederateConfig(configFile, clazz);
            } catch (IOException ioExp) {
                logger.error("Parsing input configuration file failed.");
                logger.error(ioExp);
                System.exit(-1);
            }
        }

        // if default config file was not found
        if (federateParameter == null) {
            logger.trace("No configFile could be loaded. Instantiating empty {} parameter class.", clazz.getName());
            try {
                federateParameter = clazz.newInstance();
            } catch (InstantiationException instEx) {
                logger.error("There was an error while instantiating class {}.", clazz.getName());
                logger.error(instEx);
                System.exit(-1);
            } catch (IllegalAccessException iaccEx) {
                logger.error("There was an error while overriding config values with the provided CLI values.");
                logger.error(iaccEx);
                System.exit(-1);
            }
        }

        // manual override of any parameters
        for (Option opt : commandLine.getOptions()) {

            // ignore "configFile"
            if (opt.getArgName().equals(CpswtDefaults.ConfigFileOptionName)) {
                continue;
            }

            try {
                String optVal = commandLine.getOptionValue(opt.getArgName());
                if (optVal != null) {
                    Class<?> optType = (Class<?>) opt.getType();

                    Field optField = clazz.getField(opt.getArgName());
                    boolean accessible = optField.isAccessible();

                    optField.setAccessible(true);

                    if (optType == String.class) {
                        optField.set(federateParameter, optType.cast(optVal));
                        federateParameter.fieldsSet.add(optField.getName());
                    } else if (supportedCLIArgTypes.contains(optType)) {
                        Object castedValue = optType.cast(optType.getDeclaredMethod("valueOf", String.class).invoke(null, optVal));
                        optField.set(federateParameter, castedValue);
                        federateParameter.fieldsSet.add(optField.getName());
                    } else {
                        logger.error("{} type not supported as command line argument. Skipping...", optType.getName());
                    }

                    optField.setAccessible(accessible);
                }
            } catch (IllegalAccessException iaccEx) {
                logger.error("There was an error while overriding config values with the provided CLI values.");
                logger.error(iaccEx);
            } catch (NoSuchFieldException noSuchFieldEx) {
                logger.error("There was an error while trying to access a field that doesn't exist.");
                logger.error(noSuchFieldEx);
            } catch (NoSuchMethodException | InvocationTargetException castEx) {
                logger.error("There was a problem casting numeric values from CLI arguments.");
                logger.error(castEx);
            }
        }

        // get TParam class' FederateParameter fields
        Set<Field> federateParameterFields = FederateConfig.getMandatoryFederateParameterFields(clazz);

        // warn if a field wasn't set by either a JSON field or a command line argument
        for(Field field : federateParameterFields) {
            if(!federateParameter.fieldsSet.contains(field.getName())) {
                logger.warn("No config parameter was provided for \"{}\" (type: \"{}\"). Default value set by runtime environment...",
                        field.getName(), field.getType());
            }
        }

        return federateParameter;
    }

    /**
     * Helper to determine what command line arguments we support for federate parameters.
     *
     * @return The command line argument options.
     */
    private Options getClassCLIOptions(Class<? extends FederateConfig> configClass) {
        Options options = new Options();

        Set<Field> fields = FederateConfig.getFederateParameterFields(configClass);

        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            if (fieldType.isPrimitive()) {
                fieldType = ClassUtils.primitiveToWrapper(fieldType);
            }

            options.addOption(Option.builder()
                    .longOpt(fieldName)
                    .argName(fieldName)
                    .hasArg()
                    .required(false)
                    .type(fieldType)
                    .build()
            );
        }

        return options;
    }
}
