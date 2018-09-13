package org.cpswt.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the parameter object for a federate.
 */
public class FederateConfig {

    @JsonIgnore
    final Set<String> fieldsSet = new HashSet<>();

    @JsonIgnore
    static final Logger logger = LogManager.getLogger(FederateConfig.class);

    /**
     * Time to wait before acquiring RTI for the first time (in milliseconds)
     */
    @FederateParameter
    public int federateRTIInitWaitTimeMs = 20;

    /**
     * The type of the Federate (i.e.: the model name).
     */
    @FederateParameter
    public String federateType;

    /**
     * The unique identifier of the federation.
     */
    @FederateParameter
    public String federationId;

    /**
     * Indicates if current federate is a late joiner.
     */
    @FederateParameter
    public boolean isLateJoiner;

    /**
     * The lookAhead value.
     */
    @FederateParameter
    public double lookAhead;

    /**
     * The step size value.
     */
    @FederateParameter
    public double stepSize;

    /**
     * Optional 'name' parameter that will be acquired as 'id'
     * Use {@link FederateParameterOptional} to exclude the field from "isSet" check
     */
    @FederateParameter
    @FederateParameterOptional
    public String name;

    /**
     * Default constructor for FederateConfig.
     */
    public FederateConfig() {}

    /**
     * Creates a new FederateConfig instance.
     * @param federateType The type of the Federate (i.e.: the model name).
     * @param federationId The unique identifier of the federation.
     * @param isLateJoiner Indicates if current federate is a late joiner.
     * @param lookAhead The lookAhead value.
     * @param stepSize The step size value.
     */
    public FederateConfig(String federateType, String federationId, boolean isLateJoiner, double lookAhead, double stepSize) {
        this.federateType = federateType;
        this.federationId = federationId;
        this.isLateJoiner = isLateJoiner;
        this.lookAhead = lookAhead;
        this.stepSize = stepSize;
    }

    @JsonIgnore
    public static Set<Field> getFederateParameterFields(Class<? extends  FederateConfig> configClass) {
        Set<Field> fieldSet = new HashSet<>();
        Field[] fields = configClass.getFields();

        for (Field field : fields) {
            if (field.getAnnotation(FederateParameter.class) != null) {
                fieldSet.add(field);
            }
        }

        return fieldSet;
    }

    @JsonIgnore
    public static Set<Field> getMandatoryFederateParameterFields(Class<? extends  FederateConfig> configClass) {
        Set<Field> fieldSet = new HashSet<>();
        Field[] fields = configClass.getFields();

        for (Field field : fields) {
            if (field.getAnnotation(FederateParameter.class) != null
                    && field.getAnnotation(FederateParameterOptional.class) == null) {
                fieldSet.add(field);
            }
        }

        return fieldSet;
    }
}
