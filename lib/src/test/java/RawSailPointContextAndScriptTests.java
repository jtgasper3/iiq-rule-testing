import sailpoint.iiq.testing.AbstractSailPointContextTests;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RawSailPointContextAndScriptTests extends AbstractSailPointContextTests {
    private static final Log log = LogFactory.getLog(RawSailPointContextAndScriptTests.class);

    @Test
    void rawRuleTest() throws Exception {
        Map<String, Object> argMap = new HashMap<>();
        argMap.put("context", context);
        argMap.put("log", log);
        argMap.put("environment", new HashMap<String, Object>());

        String expected = "This is a test";
        Object actual = runRule(
                "src/test/objects/RawRuleTest/Local-RawRuleTest.xml",
                argMap,
                log
        );

        assertEquals(expected, actual);
    }

    @Test
    void rawScriptTest() throws Exception {
        Map<String, Object> argMap = new HashMap<>();
        argMap.put("input1", 20);
        argMap.put("input2", 3);

        int expected = 23;
        List<String> ruleFilepaths = new java.util.ArrayList<>();
        ruleFilepaths.add("src/test/objects/RawRuleTest/Local-RawRuleFunctionLibrary.xml");

        Object actual = runScript(
                "addTwoNumbers(input1, input2);",
                argMap,
                ruleFilepaths,
                log
        );

        assertEquals(expected, actual);
    }
}
