package sailpoint.iiq.testing;

import sailpoint.api.SailPointContext;
import sailpoint.object.Rule;
import sailpoint.object.RuleRunner;
import sailpoint.object.Script;
import sailpoint.server.BSFRuleRunner;
import sailpoint.server.Environment;
import sailpoint.tools.GeneralException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Factory that creates a mock {@link SailPointContext} with .runRule() and .runScript() methods implemented.
 * <p>
 * Other methods must be explicitly mocked. For example:
 * <pre>
 * when(context.getObject(Application.class, name)).thenReturn(app);
 * when(context.getObject(Custom.class, CUSTOM_GRACE_PERIOD_MAP.getName())).thenReturn(CUSTOM_GRACE_PERIOD_MAP);
 * when(context.getObjectByName(Identity.class, IDENTITY_ID)).thenReturn(identity);
 * when(context.getReferencedObject("sailpoint.object.Application", id, name)).thenReturn(app);
 * </pre>
 */
public class MockSailPointContextFactory {

    /**
     * Creates a SailPointContext.
     * @return a MockSailPointContext that has .runRule() and .runScript() methods implemented
     * @throws Exception any exception
     */
    public static SailPointContext createContext() throws Exception {
        new Environment();

        return buildContext();
    }

    private static SailPointContext buildContext() throws Exception {
        SailPointContext context = spy(SailPointContext.class);

        RuleRunner ruleRunner = new BSFRuleRunner();
        configRuleRunner(context, ruleRunner);
        configScriptRunner(context, ruleRunner);

        return context;
    }

    private static void configRuleRunner(SailPointContext context, RuleRunner ruleRunner) throws GeneralException {
        when(context.runRule(any(Rule.class), anyMap())).thenAnswer(
                invocation -> ruleRunner.runRule(invocation.getArgument(0), invocation.getArgument(1))
        );
        when(context.runRule(any(Rule.class), anyMap(), anyList())).thenAnswer(
                invocation -> ruleRunner.runRule(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))
        );
    }

    private static void configScriptRunner(SailPointContext context, RuleRunner ruleRunner) throws GeneralException {
        when(context.runScript(any(Script.class), anyMap())).thenAnswer(
                invocation -> ruleRunner.runScript(invocation.getArgument(0), invocation.getArgument(1))
        );
        when(context.runScript(any(Script.class), anyMap(), anyList())).thenAnswer(
                invocation -> ruleRunner.runScript(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))
        );
    }
}
