package org.cpswt.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * ConfigParser
 */
public class ConfigParser {

    public static <TConfig extends FederateConfig> TConfig parseFederateConfig(File configFile, final Class<TConfig> clazz) throws IOException {

        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        TConfig configObj = mapper.readValue(configFile, clazz);

        // "save" which fields were set from the config file
        JsonParser jsonParser = jsonFactory.createParser(configFile);
        while(jsonParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonParser.getCurrentName();
            configObj.fieldsSet.add(fieldName);
        }
        jsonParser.close();

        return configObj;
    }

    public static <TConfig> TConfig parseConfig(File configFile, final Class<TConfig> clazz) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        TConfig configObj = mapper.readValue(configFile, clazz);

        return configObj;
    }
}
