package c2w.host.api;

import c2w.hla.FederateState;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FederationManagerControlMessage {
    @JsonProperty
    public FederateState targetState;
}
