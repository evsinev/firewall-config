package com.payneteasy.firewall.dao.model;

public class TCustomRule {
    public ChainType chain;
    public String    rule;
    public String    description;
    public String    justification;

    public ChainType getChain() {
        return chain;
    }

    public String getRule() {
        return rule;
    }

    public String getDescription() {
        return description;
    }

    public String getJustification() {
        return justification;
    }

    @Override
    public String toString() {
        return "TCustomRule{" +
                "chain=" + chain +
                ", rule='" + rule + '\'' +
                ", description='" + description + '\'' +
                ", justification='" + justification + '\'' +
                '}';
    }
}
