package com.payneteasy.firewall.l2.labels.wire;

public class TemplateText {

    private final String text;

    public TemplateText(String aText) {
        text = aText;
    }

    public String replace(String... args) {
        String result = text;
        for (int i = 0; i < args.length ; i += 2) {
            result = result.replace(args[i], args[i + 1]);
        }
        return result;
    }
}
