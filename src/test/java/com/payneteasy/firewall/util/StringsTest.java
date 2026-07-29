package com.payneteasy.firewall.util;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The string and file helpers. They look trivial, but padRight drives the bind zone
 * alignment and split drives every --filter argument.
 */
public class StringsTest {

    @Test
    public void hasTextTreatsBlankAsEmpty() {
        assertThat(Strings.hasText("x"), is(true));
        assertThat(Strings.hasText(null), is(false));
        assertThat(Strings.hasText(""), is(false));
        assertThat(Strings.hasText("   "), is(false));
        assertThat(Strings.hasText("\t\n"), is(false));

        assertThat(Strings.isEmpty("   "), is(true));
        assertThat(Strings.isEmpty("x"), is(false));
    }

    @Test
    public void firstReturnsTheFirstNonBlankArgument() {
        assertThat(Strings.first(null, "  ", "found", "later"), is("found"));
        assertThat(Strings.first(null, "  "), nullValue());
        assertThat(Strings.first((String[]) null), nullValue());
    }

    @Test
    public void splitAcceptsSeveralDelimitersAndDropsEmptyTokens() {
        assertThat(Strings.split("a,b;c d", ',', ';', ' '), is(new String[]{"a", "b", "c", "d"}));
        assertThat(Strings.split("a,,,b", ','), is(new String[]{"a", "b"}));
        assertThat(Strings.split("", ','), is(new String[0]));
    }

    @Test
    public void splitParamsUsesTheStandardFilterDelimiters() {
        assertThat(Strings.splitParams("internal, ipmi;internet\tdmz"),
                is(new String[]{"internal", "ipmi", "internet", "dmz"}));
    }

    @Test
    public void padRightPadsToTheGivenLengthAndReturnsTheInputWhenItAlreadyFits() {
        assertThat(Strings.padRight("db-1", 8), is("db-1    "));
        assertThat(Strings.padRight("db-1", 4), is("db-1"));
        // longer than the target: the loop never runs, so the text comes back untouched
        assertThat(Strings.padRight("db-1", 2), is("db-1"));
    }

    @Test
    public void maxLengthMeasuresTheLongestExtractedText() {
        assertThat(Strings.maxLength(Arrays.asList("a", "bbb", "cc"), aObj -> aObj), is(3));
        assertThat(Strings.maxLength(Arrays.<String>asList(), aObj -> aObj), is(0));
    }

    @Test
    public void appenderJoinsWithItsDelimiter() {
        StringAppender appender = new StringAppender(",");

        assertThat(appender.toString(), is(""));
        assertThat(appender.append("a").append("b").append("c").toString(), is("a,b,c"));
    }

    @Test
    public void anEmptyAppenderFailsWithTheGivenMessage() {
        try {
            new StringAppender(",").toStringFailIfEmpty("nothing to join");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("nothing to join"));
        }
    }

    @Test
    public void aNonEmptyAppenderReturnsItsTrimmedContent() {
        assertThat(new StringAppender(",").append("a").toStringFailIfEmpty("unused"), is("a"));
    }

    /** MainMikrotik relies on this to reject a port listed twice for one vlan. */
    @Test
    public void theUniqueAppenderRejectsADuplicate() {
        UniqueStringAppender appender = new UniqueStringAppender(",");
        appender.append("ether1");
        appender.append("ether2");

        try {
            appender.append("ether1");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("String ether1 already added"));
        }
        assertThat(appender.toString(), is("ether1,ether2"));
    }

    @Test
    public void readFileJoinsLinesWithNewlinesAndAppendsATrailingOne() {
        File file = com.payneteasy.firewall.testing.TestFixtures.resource("/l2/l2positions.properties");

        String text = Files.readFile(file);

        assertThat(text, startsWith("sw-core-1.1 = 120, 40\n"));
        assertThat(text, endsWith("\n"));
    }

    @Test
    public void readingAMissingFileFailsWithItsPath() {
        try {
            Files.readFile(new File("no/such/file.txt"));
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Couldn't read file"));
            assertThat(e.getMessage(), containsString("file.txt"));
        }
    }
}
