package helpers;

import mocks.MockSailPointContextFactory;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import sailpoint.api.SailPointContext;
import sailpoint.object.Application;
import sailpoint.object.Rule;
import sailpoint.object.Script;
import sailpoint.tools.Brand;
import sailpoint.tools.BrandingService;
import sailpoint.tools.BrandingServiceFactory;
import sailpoint.tools.xml.XMLObjectFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

public abstract class AbstractRuleTests {
    protected SailPointContext context;
    private static MockedStatic<BrandingServiceFactory> brandingServiceFactoryMockStatic;
    protected static String objectPath = "src/test/objects/applications/";

    @BeforeAll
    public static void beforeAll() {
        BrandingService brandingService = mock(BrandingService.class);
        when(brandingService.getBrand()).thenReturn(Brand.IIQ);

        brandingServiceFactoryMockStatic = mockStatic(BrandingServiceFactory.class, "BrandingServiceMockStatic");
        when(BrandingServiceFactory.getService()).thenReturn(brandingService);
    }

    @BeforeEach
    public void prepareResources() throws Exception {
        context = MockSailPointContextFactory.createContext();
    }

    @AfterAll
    public static void closeResources() {
        brandingServiceFactoryMockStatic.close();
    }

    /**
     * createRuleFromResource creates a Rule object from an XML file/resource
     * @param filename the name of the file (xml) being loaded
     * @return a populated SailPoint Rule object
     * @throws IOException any ioexception
     */
    protected Rule createRule(String filename) throws IOException {
        String xml = getStringFromFile(filename);

        Rule rule = (Rule) XMLObjectFactory.getInstance().parseXml(context, xml, true);
        rule.setId(String.valueOf(filename.hashCode()));

        return rule;
    }

    /**
     *  Runs a rule loaded from a resource path/file and the provided args map
     * @param ruleResourceName path/name of a rule xml resource/file
     * @param ruleArgs argument map specific to IdentityAttribute rules
     * @param log a pass through log object
     * @return the output of the rule (type varies)
     * @throws Exception any exception
     */
    protected Object runRule(String ruleResourceName, Map<String, Object> ruleArgs, Log log) throws Exception {
        Rule rule = createRule(ruleResourceName);

        try {
            return context.runRule(rule, ruleArgs);
        } catch (Exception e) {
            log.warn(e.getMessage());
            throw new Exception(e);
        } finally {
            log.debug(mockingDetails(context).printInvocations());
        }
    }

    protected Object runScript(String scriptSource, Map<String, Object> args, Log log) throws Exception {
        return runScript(scriptSource, args, List.of(), log);
    }

    protected Object runScript(String scriptSource, Map<String, Object> args, List<String> ruleResourceNames, Log log) {
        Script script = new Script();
        script.setSource(scriptSource);

        List<Rule> rules = ruleResourceNames.stream()
                .map(ruleResourceName -> {
                    try {
                        return createRule(ruleResourceName);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        script.setIncludes(rules);

        try {
            return context.runScript(script, args);
        } catch (Exception e) {
            log.warn(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            log.debug(mockingDetails(context).printInvocations());
        }
    }


    protected void loadApplication(SailPointContext context, String id, String name) throws Exception {
        String xml = getStringFromFile(objectPath + id + ".xml");
        Application application = (Application) XMLObjectFactory.getInstance().parseXml(context, xml, true);
        when(context.getReferencedObject(
                "sailpoint.object.Application",
                id,
                name)).thenReturn(application);
    }


    /**
     * Reads the contents of a file and returns it as a string
     * @param filename the file being loaded
     * @return the loaded string
     * @throws IOException any exception
     */
    public String getStringFromFile(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
}
