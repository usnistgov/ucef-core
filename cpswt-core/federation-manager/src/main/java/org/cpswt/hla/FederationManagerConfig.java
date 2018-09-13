package org.cpswt.hla;

import org.cpswt.config.FederateConfig;
import org.cpswt.config.FederateParameter;

public class FederationManagerConfig extends FederateConfig {
    /**
     * The host to bind the RESTful API.
     */
    @FederateParameter
    public String bindHost;

    /**
     * The port where the RESTful API listens.
     */
    @FederateParameter
    public int port;

    /**
     * The endpoint where federation manager control is done.
     */
    @FederateParameter
    public String controlEndpoint;

    /**
     * The endpoint where federation queries can be executed.
     */
    @FederateParameter
    public String federatesEndpoint;

    /**
     * Indicates the federation end time.
     */
    @FederateParameter
    public double federationEndTime;

    /**
     * Indicates if the federation manager should run in real time mode.
     */
    @FederateParameter
    public boolean realTimeMode;

    /**
     * The fed file for RTI.
     */
    @FederateParameter
    public String fedFile;

    /**
     * Represents the configuration of the experiment.
     */
    @FederateParameter
    public String experimentConfig;

}
