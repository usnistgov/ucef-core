package org.cpswt.hla;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;

/**
 * FederateInfo
 */
public class FederateInfo {
    private final int federateHandle;
    private final String federateType;
    private final String federateHost;
    private final DateTime joinTime;
    
    private String federateId = null;
    private boolean isExpected = false;
    private DateTime resignTime = null;

    public int getFederateHandle() {
        return federateHandle;
    }
    
    public String getFederateType() {
        return federateType;
    }
    
    public String getFederateHost() {
        return federateHost;
    }
    
    public DateTime getJoinTime() {
        return joinTime;
    }
    
    public String getFederateId() {
        return federateId;
    }

    public boolean isExpected() {
        return isExpected;
    }
    
    public DateTime getResignTime() {
        return resignTime;
    }
    
    public void update(FederateJoinInteraction interaction) {
        this.federateId = interaction.get_FederateId();
        this.isExpected = !interaction.get_IsLateJoiner();
    }

    public void setResignTime(DateTime resignTime) {
        this.resignTime = resignTime;
    }

    @JsonIgnore
    public boolean hasResigned() {
        return resignTime != null;
    }

    public FederateInfo(FederateObject object) {
        this.federateHandle = object.get_FederateHandle();
        this.federateType = object.get_FederateType();
        this.federateHost = object.get_FederateHost();
        this.joinTime = DateTime.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FederateInfo) {
            return this.federateHandle == ((FederateInfo)obj).federateHandle;
        }
        return false;
    }
}
