package sailpoint.iiq.testing;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import sailpoint.api.SailPointContext;
import sailpoint.object.Application;
import sailpoint.object.Bundle;
import sailpoint.object.Rule;
import sailpoint.object.Script;
import sailpoint.tools.Brand;
import sailpoint.tools.BrandingService;
import sailpoint.tools.BrandingServiceFactory;
import sailpoint.tools.GeneralException;
import sailpoint.tools.xml.XMLObjectFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

/**
 * Abstract JUnit test class that provides support for testing SailPoint IIQ Rules, etc.
 * <p>
 * This class initializes a mock {@link SailPointContext}. It has methods for creating SailPoint IIQ objects from
 * SailPoint DTD compliant XML files/strings and running {@link Rule}s and {@link Script}s.
 * <p>
 * Example which loads an application file, a barebones identity file (just the data, roles, and app links we need),
 * runs a script which calls a Rule library, and validates the data:
 * <pre>
 * Application application = (Application) xmlFileToObject(OBJECT_BASE_PATH + "application/" + filename);
 * application.setId(id);
 * String name = app.getName();
 * when(context.getReferencedObject("sailpoint.object.Application", id, name)).thenReturn(app);
 * when(context.getObject(Application.class, name)).thenReturn(app);
 *
 * // identities with links to applications, must provide a SailPoint context with the app pre-populated for things like IdentityService to work
 * Identity identity = (Identity) xmlFileToObject("TEST_OBJECT_BASE_PATH + "RuleLibraryGracePeriods/setGracePeriodAttributes/" +  testCaseFileName + ".xml", context);
 * when(context.getObjectByName(Identity.class, IDENTITY_ID)).thenReturn(identity);
 *
 * // The script is a string literal of the entry point of our rule under test (since the rule is a library and not a self driving rule.
 * // If the Rule was self-driving, we could call .runRule() passing in the Rule object or a Rule xml file and the argMap (global objects)
 * // used by the beanshell script
 * runScript(
 *      createScript("setGracePeriodAttributes(identityName, isRehire, isMover, isLeaver);");
 *      createMethodArgs(false, false, true), // Map of (global object) contex and method parameters: identityName, isRehire, isMover, and isLeaver)
 *      List.of(createRule(OBJECT_BASE_PATH + "/rule/RuleLibraryGracePeriods.xml")),
 *      LOG
 * );
 *
 * verify(context, times(1)).getObjectByName(Identity.class, IDENTITY_ID);
 * assertEquals("2024-05-16", identity.getAttribute("passiveGracePeriod"));
 * assertNull(identity.getAttribute("activeGracePeriod"));
 * verify(context, times(1)).saveObject(identity);
 * verify(context, times(1)).commitTransaction();
 * </pre>
 */

public abstract class AbstractSailPointContextTests {
    protected SailPointContext context;
    private static MockedStatic<BrandingServiceFactory> brandingServiceFactoryMockStatic;
    private static final Map<String, Application> APPLICATION_OBJECT_CACHE = new HashMap<>();
    private static final Map<String, Bundle> BUNDLE_OBJECT_CACHE = new HashMap<>();

    @BeforeAll
    public static void beforeAll() {
        final BrandingService brandingService = mock(BrandingService.class);
        when(brandingService.getBrand()).thenReturn(Brand.IIQ);

        brandingServiceFactoryMockStatic = mockStatic(BrandingServiceFactory.class, "BrandingServiceMockStatic");
        when(BrandingServiceFactory.getService()).thenReturn(brandingService);
    }

    @BeforeEach
    public void prepareResources() {
        try {
            context = MockSailPointContextFactory.createContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void closeResources() {
        brandingServiceFactoryMockStatic.close();
    }

    /**
     * creates a Rule object from an XML file/resource
     * @param filepath the name of the file (xml) being loaded
     * @return a populated SailPoint Rule object
     */
    protected static Rule createRule(String filepath)  {
        final Rule rule = (Rule) xmlFileToObject(filepath);
        rule.setId(String.valueOf(filepath.hashCode()));

        return rule;
    }

    /**
     * create a Script object from a string
     * @param scriptSource string containing the script to execute
     * @return a SailPoint Script object
     */
    protected static Script createScript(String scriptSource) {
        Script script = new Script();
        script.setSource(scriptSource);

        return script;
    }

    /**
     * Runs a rule loaded from a resource path/file and the provided args map
     * @param ruleFilepath path/name of a rule xml resource/file
     * @param ruleArgs argument map
     * @param log a pass through log object
     * @return the output of the rule (type varies)
     */
    protected Object runRule(String ruleFilepath, Map<String, Object> ruleArgs, Log log) {
        final Rule rule = createRule(ruleFilepath);
        return runRule(rule, ruleArgs, log);
    }

    /**
     * Runs a Rule loaded from a resource path/file and the provided args map
     * @param rule a Rule object
     * @param ruleArgs argument map
     * @param log a pass through log object
     * @return the output of the rule (type varies)
     */
    protected Object runRule(Rule rule, Map<String, Object> ruleArgs, Log log) {
        try {
            return context.runRule(rule, ruleArgs);
        } catch (Exception e) {
            log.warn(e.getMessage());
            log.debug(mockingDetails(context).printInvocations());
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a script from a String and the provided args map
     * @param scriptSource the script to run
     * @param args argument map
     * @param log a pass through log object
     * @return the output of the rule (type varies)
     */
    protected Object runScript(String scriptSource, Map<String, Object> args, Log log) {
        List<String> ruleFilepaths = new java.util.ArrayList<>();
        return runScript(scriptSource, args, ruleFilepaths, log);
    }

    /**
     * Runs a script from a string, the provided args map, and a list of reference rule libraries filepaths
     * @param scriptSource the script to run
     * @param args argument map
     * @param ruleFilepaths a list of reference rule libraries filepaths
     * @param log a pass through log object
     * @return the output of the rule (type varies)
     */
    protected Object runScript(String scriptSource, Map<String, Object> args, List<String> ruleFilepaths, Log log) {
        final Script script = new Script();
        script.setSource(scriptSource);

        final List<Rule> rules = ruleFilepaths.stream()
                .map(AbstractSailPointContextTests::createRule)
                .collect(Collectors.toList());

        return runScript(script, args, rules, log);
    }

    /**
     * Runs a Script with the provided args map
     * @param script the script to run
     * @param args argument map
     * @param log a pass through log object
     * @return the output of the rule (type varies)
     */
    protected Object runScript(Script script, Map<String, Object> args, Log log) {
        List<Rule> rules = new java.util.ArrayList<>();
        return runScript(script, args, rules, log);
    }

    /**
     * Runs a script and the provided args map
     * @param script the script to run
     * @param args argument map
     * @param rules a list of reference Rule libraries
     * @param log a pass through log object
     * @return the output of the rule (type varies)
     */
    protected Object runScript(Script script, Map<String, Object> args, List<Rule> rules, Log log) {
        script.setIncludes(rules);

        try {
            return context.runScript(script, args);
        } catch (Exception e) {
            log.warn(e.getMessage());
            log.debug(mockingDetails(context).printInvocations());
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the contents of a file and returns it as a string
     * @param filename the file being loaded
     * @return the loaded string
     */
    public static String getStringFromFile(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an IIQ object from a string (xml) using a null SailPointContext
     * @param xml string containing xml
     * @return a SailPoint object
     */
    public static Object xmlStringToObject(String xml) {
        return xmlStringToObject(xml, null);
    }

    /**
     * Creates an IIQ object from a string (xml) and a SailPointContext
     * @param xml string containing xml
     * @param context a SailPoint Context
     * @return a SailPoint object
     */
    public static Object xmlStringToObject(String xml, SailPointContext context) {
        return XMLObjectFactory.getInstance().parseXml(context, xml, true);
    }

    /**
     * Creates an IIQ object from an XML file using a null SailPointContext
     * @param filepath path to xml file to consume
     * @return a SailPoint object
     */
    public static Object xmlFileToObject(String filepath) {
        return xmlFileToObject(filepath, null);
    }

    /**
     * Creates an IIQ object from an XML file using a SailPointContext
     * @param filepath path to xml file to consume
     * @param context a SailPoint Context
     * @return a SailPoint object
     */
    public static Object xmlFileToObject(String filepath, SailPointContext context) {
        return xmlStringToObject(getStringFromFile(filepath), context);
    }

    /**
     * Caches an {@link Application} object from disk during a Test class startup (@BeforeAll).
     * Call loadCachedApplications() in the @BeforeEach or the @Test method to associate each cached Application
     * object into the {@link SailPointContext}.
     * @param id the guid to associate with the application and in Application Links in the identity xml file
     * @param filename the Application object xml file
     */
    protected static void cacheTheApplication(String id, String filename) {
        Application application = (Application) xmlFileToObject(filename);
        application.setId(id);
        APPLICATION_OBJECT_CACHE.put(id, application);
    }

    /**
     *  Associate each cached Application object into the {@link SailPointContext}.
     *  Usually used in the @BeforeEach or the @Test method.
     */
    protected void loadCachedApplications() {
        APPLICATION_OBJECT_CACHE.forEach((id, app) -> {
            String name = app.getName();
            try {
                when(context.getObject(Application.class, name)).thenReturn(app);
                when(context.getObjectByName(Application.class, name)).thenReturn(app);
                when(context.getReferencedObject("sailpoint.object.Application", id, name)).thenReturn(app);
            } catch (GeneralException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Caches an {@link Bundle} object from disk during a Test class startup (@BeforeAll).
     * Call loadCachedBundles() in the @BeforeEach or the @Test method to associate each cached Bundle
     * object into the {@link SailPointContext}.
     * @param id the guid to associate with the bundle and in AssignedRoles Links in the identity xml file
     * @param filename the Bundle object xml file
     */
    protected static void cacheTheBundle(String id, String filename) {
        Bundle bundle = (Bundle) xmlFileToObject(filename);
        bundle.setId(id);
        BUNDLE_OBJECT_CACHE.put(id, bundle);
    }

    /**
     *  Associate each cached {@link Bundle} object into the {@link SailPointContext}.
     *  Usually used in the @BeforeEach or the @Test method.
     */
    protected void loadCachedBundles() {
        BUNDLE_OBJECT_CACHE.forEach((id, bundle) -> {
            String name = bundle.getName();
            try {
                when(context.getObject(Bundle.class, name)).thenReturn(bundle);
                when(context.getObjectByName(Bundle.class, name)).thenReturn(bundle);
                when(context.getReferencedObject("sailpoint.object.Bundle", id, name)).thenReturn(bundle);
            } catch (GeneralException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
