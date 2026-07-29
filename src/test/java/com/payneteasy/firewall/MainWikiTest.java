package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Golden test for the Redmine wiki pages.
 *
 * MainWiki writes pages_history.yml back into the config dir and skips pages whose
 * hash has not changed, so every test gets a fresh writable copy of the demo network -
 * against the repository copy the second run would generate nothing at all.
 */
public class MainWikiTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File configDir;
    private File wikiDir;

    @Before
    public void setUp() throws Exception {
        configDir = tmp.newFolder("config");
        TestFixtures.copyDemoNetworkInputs(configDir);
        wikiDir = tmp.newFolder("wiki-out");
    }

    @Test
    public void writesEveryPageAndAllOfThemMatchGolden() throws Exception {
        generate();

        String[] pages = wikiDir.list();
        assertThat(pages.length, is(20));

        for (File page : wikiDir.listFiles()) {
            assertThat(page.getName(), TestFixtures.readFile(page),
                    is(TestFixtures.golden("wiki/" + page.getName())));
        }
    }

    /**
     * MainWiki swallows per-host exceptions into a log warning, so an empty or missing
     * page is the only symptom of a broken host. Assert both pages exist for all 8 hosts.
     */
    @Test
    public void generatesTwoNonEmptyPagesForEveryHost() throws Exception {
        generate();

        for (String host : new String[]{"adm-1", "db-1", "fw-1", "fw-2", "proxy-1", "web-1",
                "sw-core-1", "partner-api.example.com"}) {
            for (String suffix : new String[]{"_details", "_packets"}) {
                File page = new File(wikiDir, host + suffix + ".wiki");
                assertThat(page.getName() + " exists", page.isFile(), is(true));
                assertThat(page.getName() + " is not empty", page.length(), greaterThan(0L));
            }
        }
    }

    @Test
    public void generatesOneGroupPagePerGroupPlusServices() throws Exception {
        generate();

        assertThat(new File(wikiDir, "internal_group.wiki").isFile(), is(true));
        assertThat(new File(wikiDir, "external_group.wiki").isFile(), is(true));
        assertThat(new File(wikiDir, "ipmi_group.wiki").isFile(), is(true));
        assertThat(new File(wikiDir, "services.wiki").isFile(), is(true));
    }

    @Test
    public void persistsPagesHistoryAndSkipsUnchangedPagesOnTheSecondRun() throws Exception {
        generate();

        File history = new File(configDir, "pages_history.yml");
        assertThat("pages_history.yml written into the config dir", history.isFile(), is(true));

        // the history reloads without tripping the pageHash == 0 validation
        ConfigDaoYaml reloaded = new ConfigDaoYaml(configDir);
        assertThat(reloaded.thePagesHistoryMap.size(), is(20));
        assertThat(reloaded.thePagesHistoryMap.get("services").pageHash, not(0L));

        File secondRun = tmp.newFolder("wiki-out-2");
        MainWiki.main(new String[]{secondRun.getPath(), "dummy-key", configDir.getPath()});
        assertThat("unchanged pages are not rewritten", secondRun.list(), emptyArray());
    }

    @Test
    public void forceRewritesEveryPageEvenWhenUnchanged() throws Exception {
        generate();

        File forced = tmp.newFolder("wiki-out-forced");
        MainWiki.main(new String[]{forced.getPath(), "dummy-key", configDir.getPath(), "--force"});
        assertThat(forced.list().length, is(20));
    }

    @Test
    public void failsOnAMissingConfigDir() {
        try {
            MainWiki.main(new String[]{wikiDir.getPath(), "dummy-key", "no/such/dir"});
            throw new AssertionError("expected an IllegalStateException");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("is not exists"));
        }
    }

    @Test
    public void requiresAtLeastThreeArguments() {
        try {
            MainWiki.main(new String[]{"one", "two"});
            throw new AssertionError("expected an IllegalArgumentException");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("usage: redmine-wiki-address"));
        }
    }

    private void generate() throws Exception {
        MainWiki.main(new String[]{wikiDir.getPath(), "dummy-key", configDir.getPath()});
    }
}
