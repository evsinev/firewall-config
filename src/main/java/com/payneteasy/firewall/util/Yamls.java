package com.payneteasy.firewall.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * The single place a snakeyaml {@link Yaml} is built.
 *
 * snakeyaml 2.x installs an UnTrustedTagInspector by default, which refuses every global
 * <code>!!com.foo.Bar</code> tag. The description format needs them: pages_history.yml is
 * dumped - and read back - as <code>!!com.payneteasy.firewall.dao.model.TPagesHistory</code>,
 * and those files live in the description repositories, so the format has to stay.
 *
 * Never call <code>new Yaml(...)</code> directly; the default inspector fails at load time
 * with "Global tag is not allowed", far away from the dump that produced the tag.
 */
public class Yamls {

    private static final String ALLOWED_TAG_PREFIX = "com.payneteasy.firewall.";

    public static Yaml newYaml(DumperOptions aDumperOptions) {
        return new Yaml(newLoaderOptions(), aDumperOptions);
    }

    public static Yaml newYaml() {
        return newYaml(new DumperOptions());
    }

    private static LoaderOptions newLoaderOptions() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setTagInspector(tag -> tag.getClassName().startsWith(ALLOWED_TAG_PREFIX));
        return loaderOptions;
    }
}
