package com.payneteasy.firewall.testing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * Fixture lookup and small IO helpers shared by the tests.
 *
 * Text is always normalized to \n line endings so a golden comparison does not
 * depend on the platform the file was captured on.
 */
public class TestFixtures {

    /**
     * examples/demo-network, used read-only. Surefire runs with the module root
     * as the working directory; the basedir property makes it work from an IDE too.
     */
    public static File demoNetworkDir() {
        File dir = new File(System.getProperty("basedir", "."), "examples/demo-network");
        if (!dir.isDirectory()) {
            throw new IllegalStateException("Demo network not found at " + dir.getAbsolutePath()
                    + ".\nRun the tests from the module root or set -Dbasedir=<module root>.");
        }
        return dir;
    }

    /**
     * A config fixture from src/test/resources/config. Resolves to
     * target/test-classes/config/&lt;name&gt;, i.e. already a copy - a generator
     * that writes into its config dir cannot dirty src/.
     */
    public static File configDir(String aName) {
        return resource("/config/" + aName);
    }

    public static File resource(String aPath) {
        try {
            java.net.URL url = TestFixtures.class.getResource(aPath);
            if (url == null) {
                throw new IllegalStateException("No test resource " + aPath);
            }
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Bad test resource path " + aPath, e);
        }
    }

    /** Expected output committed under src/test/resources/golden. */
    public static String golden(String aName) {
        return readFile(resource("/golden/" + aName));
    }

    public static String readFile(File aFile) {
        try {
            byte[] bytes = Files.readAllBytes(aFile.toPath());
            return new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + aFile.getAbsolutePath(), e);
        }
    }

    public static List<String> readLines(File aFile) {
        return java.util.Arrays.asList(readFile(aFile).split("\n", -1));
    }

    /**
     * The text with its lines sorted, for comparing output whose line order depends on
     * File.listFiles() and therefore differs between filesystems (APFS vs ext4).
     *
     * Only two generated files need this - see MainWikiTest and MainBindTest. Everything
     * else is compared byte for byte.
     */
    public static String sortedLines(String aText) {
        List<String> lines = new java.util.ArrayList<>(java.util.Arrays.asList(aText.split("\n", -1)));
        java.util.Collections.sort(lines);
        return String.join("\n", lines);
    }

    /** Keeps only the lines matching a regex - used for zone files, whose headers carry a timestamp. */
    public static String grepLines(File aFile, String aRegex) {
        StringBuilder sb = new StringBuilder();
        for (String line : readLines(aFile)) {
            if (line.matches(aRegex)) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * A fresh writable copy of the demo network containing only its inputs - the
     * hosts tree and the top-level yml/diag files.
     *
     * Deliberately NOT a plain copyDir: examples/demo-network also accumulates
     * gitignored output (gen/, bind-out/, wiki-out/, pages_history.yml) from local
     * runs, and a copied pages_history.yml would make MainWiki decide every page is
     * unchanged and write nothing.
     */
    public static void copyDemoNetworkInputs(File aTo) {
        File from = demoNetworkDir();
        copyDir(new File(from, "hosts"), new File(aTo, "hosts"));
        for (File file : from.listFiles()) {
            if (!file.isFile() || "pages_history.yml".equals(file.getName())) {
                continue;
            }
            if (file.getName().endsWith(".yml") || file.getName().endsWith(".diag")) {
                try {
                    Files.copy(file.toPath(), new File(aTo, file.getName()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException("Could not copy " + file, e);
                }
            }
        }
    }

    /** Recursive copy, so a test can hand a generator a config dir it may write into. */
    public static void copyDir(File aFrom, File aTo) {
        Path from = aFrom.toPath();
        Path to = aTo.toPath();
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path source : (Iterable<Path>) paths::iterator) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not copy " + aFrom + " to " + aTo, e);
        }
    }

    /**
     * Captures System.out while the body runs. Several generators print their whole
     * output instead of writing a file, so this is the only way to assert on them.
     * Not thread safe - see the surefire comment in pom.xml about not going parallel.
     */
    public static String captureStdout(RunnableWithException aBody) {
        PrintStream saved = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, "UTF-8"));
            aBody.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed while capturing stdout", e);
        } finally {
            System.out.flush();
            System.setOut(saved);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }
}
