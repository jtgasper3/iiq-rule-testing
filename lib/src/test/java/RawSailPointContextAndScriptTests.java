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
        Map<String, Object> map = Map.of("context", context, "log", log, "environment", new HashMap<String, Object>());

        String expected = "This is a test";
        Object actual = runRule(
                "src/test/objects/RawRuleTest/Local-RawRuleTest.xml",
            map,
            log
        );

        assertEquals(expected, actual);
    }

    @Test
    void rawScriptTest() throws Exception {
        Map<String, Object> map = Map.of("input1", 20, "input2", 3);

        int expected = 23;
        Object actual = runScript(
                "addTwoNumbers(input1, input2);",
                map,
                List.of("src/test/objects/RawRuleTest/Local-RawRuleFunctionLibrary.xml"),
                log
        );

        assertEquals(expected, actual);
    }
}
