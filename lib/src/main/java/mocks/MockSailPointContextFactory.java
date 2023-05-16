package mocks;

import sailpoint.api.SailPointContext;
import sailpoint.object.Rule;
import sailpoint.object.RuleRunner;
import sailpoint.object.Script;
import sailpoint.server.BSFRuleRunner;
import sailpoint.server.Environment;
import sailpoint.tools.GeneralException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Factory that creates a context and populates the applications and a RuleRunner
 */
public class MockSailPointContextFactory {

    /**
     * Creates a MockSailPointContext.
     * @return a MockSailPointContext that is preloaded with Applications and a RuleRunner
     * @throws Exception any exception
     */
    public static SailPointContext createContext() throws Exception {
        new Environment();

        return buildContext();
    }

    private static SailPointContext buildContext() throws Exception {
        SailPointContext context = spy(SailPointContext.class);

        configRuleRunner(context);
        configScriptRunner(context);

        return context;
    }

    private static void configRuleRunner(SailPointContext context) throws GeneralException {
        RuleRunner ruleRunner = new BSFRuleRunner();
        when(context.runRule(any(Rule.class), any(Map.class))).thenAnswer(
                invocation -> ruleRunner.runRule(invocation.getArgument(0), invocation.getArgument(1))
        );
        when(context.runRule(any(Rule.class), any(Map.class), any(List.class))).thenAnswer(
                invocation -> ruleRunner.runRule(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))
        );
    }

    private static void configScriptRunner(SailPointContext context) throws GeneralException {
        RuleRunner ruleRunner = new BSFRuleRunner();
        when(context.runScript(any(Script.class), any(Map.class))).thenAnswer(
                invocation -> ruleRunner.runScript(invocation.getArgument(0), invocation.getArgument(1))
        );
        when(context.runScript(any(Script.class), any(Map.class), any(List.class))).thenAnswer(
                invocation -> ruleRunner.runScript(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))
        );
    }
}
